package com.beatpath.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beatpath.app.lessons.Curriculum
import com.beatpath.app.model.Channel
import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.CompSettings
import com.beatpath.app.model.DelaySettings
import com.beatpath.app.model.EqSettings
import com.beatpath.app.model.InsertTrack
import com.beatpath.app.model.MasterSettings
import com.beatpath.app.model.Mix
import com.beatpath.app.model.Note
import com.beatpath.app.model.Pattern
import com.beatpath.app.model.Project
import com.beatpath.app.model.ReverbSettings
import com.beatpath.app.model.SynthPatch
import com.beatpath.app.model.Wave
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** The ear-training unit that opens the course (before the Studio levels). */
object EarIds {
    const val HIGH_LOW = "ear.high-low"
    const val SOFT_LOUD = "ear.soft-loud"
    const val CONTOUR = "ear.up-down"
    const val STEP_LEAP = "ear.step-leap"
    const val MAJOR_MINOR = "ear.major-minor"
    val all = listOf(HIGH_LOW, SOFT_LOUD, CONTOUR, STEP_LEAP, MAJOR_MINOR)
}

/**
 * Local-only progress and projects (plan §9: no account, no network).
 * SharedPreferences + JSON keeps the build dependency-free; Room replaces it in the MVP.
 */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("beatpath", Context.MODE_PRIVATE)

    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false)); private set
    var unlockAll by mutableStateOf(prefs.getBoolean("unlockAll", false)); private set
    var sfxOn by mutableStateOf(prefs.getBoolean("sfx", true)); private set
    var xp by mutableStateOf(prefs.getInt("xp", 0)); private set
    var completed by mutableStateOf(prefs.getStringSet("completed", emptySet())!!.toSet()); private set
    var unassisted by mutableStateOf(prefs.getStringSet("unassisted", emptySet())!!.toSet()); private set
    var practiceDays by mutableStateOf(prefs.getStringSet("days", emptySet())!!.toSet()); private set
    var projects by mutableStateOf(loadProjects()); private set
    /** Per-skill practice stats: skill id → (attempts, correct). */
    var skills by mutableStateOf(loadPairs("skills").mapValues { it.value.first.toInt() to it.value.second }); private set
    /** Spaced review: lesson id → (due epoch day, stage). */
    var reviews by mutableStateOf(loadPairs("reviews")); private set

    /** Course order: Music basics, the ear unit, then every Studio lesson. */
    val order: List<String> = com.beatpath.app.lessons.Basics.ids + EarIds.all + Curriculum.lessons.filter { it.level <= 10 }.map { it.id } +
        com.beatpath.app.lessons.EarTwo.ids + Curriculum.lessons.filter { it.level > 10 }.map { it.id }

    /** Display title for any lesson id. */
    fun titleOf(id: String): String = com.beatpath.app.lessons.Basics.byId(id)?.title ?: Curriculum.byId(id)?.title
        ?: com.beatpath.app.ui.lesson.EarLessons.spec(id).title

    // ---------------- XP shop ----------------
    /** XP spent in Lou's Shop. The balance is xp − spent; your level always uses lifetime xp. */
    var spent by mutableStateOf(prefs.getInt("spent", 0)); private set
    var owned by mutableStateOf(prefs.getStringSet("owned", emptySet())!!.toSet()); private set
    /** Equipped item per slot (slot name → item id). */
    var equipped by mutableStateOf(Shop.Slot.entries.mapNotNull { sl -> prefs.getString("eq.${sl.name}", null)?.let { sl to it } }.toMap()); private set
    val balance: Int get() = (xp - spent).coerceAtLeast(0)

    init {
        // sky themes left the shop: give their XP back
        val gone = mapOf("sky.sunset" to 300, "sky.dusk" to 450).filterKeys { it in owned }
        if (gone.isNotEmpty()) {
            spent = (spent - gone.values.sum()).coerceAtLeast(0); owned = owned - gone.keys
            prefs.edit().putInt("spent", spent).putStringSet("owned", owned).remove("eq.SKY").apply()
        }
    }

    // ---------------- daily quests, streak chests, mini games ----------------
    private fun today() = LocalDate.now().toString()
    fun count(m: Quests.Metric): Int = prefs.getInt("qd.${today()}.${m.name}", 0)
    private fun bump(m: Quests.Metric, n: Int = 1) { if (n > 0) prefs.edit().putInt("qd.${today()}.${m.name}", count(m) + n).apply(); questTick++ }
    /** Changes whenever quest progress moves, so screens showing quests recompose. */
    var questTick by mutableStateOf(0); private set
    fun questsToday(): List<Quests.Quest> = Quests.today(LocalDate.now().toEpochDay())
    fun claimed(q: Quests.Quest): Boolean = q.id in prefs.getStringSet("qc.${today()}", emptySet())!!
    fun questDone(q: Quests.Quest): Boolean = count(q.metric) >= q.target
    /** Collect a finished quest's XP (once a day). */
    fun claim(q: Quests.Quest): Int {
        if (!questDone(q) || claimed(q)) return 0
        prefs.edit().putStringSet("qc.${today()}", prefs.getStringSet("qc.${today()}", emptySet())!! + q.id).apply()
        addXp(q.reward, countsToday = false); questTick++
        return q.reward
    }
    /** The streak chest you can open right now (milestone day → XP), if any. */
    fun chestReady(): Pair<Int, Int>? {
        val st = streak(); if (st <= 0) return null
        val start = streakStart(st)
        val opened = prefs.getStringSet("chests", emptySet())!!
        return Quests.chests.entries.filter { it.key <= st && "${it.key}@$start" !in opened }.maxByOrNull { it.key }?.toPair()
    }
    fun openChest(): Int {
        val (day, reward) = chestReady() ?: return 0
        val start = streakStart(streak())
        // opening the biggest one also covers smaller milestones in the same streak
        val keys = Quests.chests.keys.filter { it <= day }.map { "$it@$start" }
        prefs.edit().putStringSet("chests", prefs.getStringSet("chests", emptySet())!! + keys).apply()
        addXp(reward, countsToday = false); questTick++
        return reward
    }
    /** First day of the current streak. The streak may end yesterday (today not practiced yet), so count back from its last day. */
    private fun streakStart(st: Int): String {
        val last = if (LocalDate.now().toString() in practiceDays) LocalDate.now() else LocalDate.now().minusDays(1)
        return last.minusDays((st - 1).toLong()).toString()
    }
    private fun addXp(n: Int, countsToday: Boolean = true) {
        xp += n; prefs.edit().putInt("xp", xp).apply()
        if (countsToday) bump(Quests.Metric.XP, n)
    }

    var bests by mutableStateOf(loadPairs("bests").mapValues { it.value.first.toInt() }); private set
    fun best(gameId: String): Int = bests[gameId] ?: 0
    /** A finished mini game: 5 XP per star, +5 for a new best; counts toward quests and today's streak. */
    fun recordGame(gameId: String, score: Int, stars: Int): Int {
        val newBest = score > best(gameId)
        if (newBest) { bests = bests + (gameId to score); savePairs("bests", bests.mapValues { it.value.toLong() to 0 }) }
        val gained = stars * 5 + if (newBest && stars > 0) 5 else 0
        addXp(gained)
        bump(Quests.Metric.GAMES); if (stars == 3) bump(Quests.Metric.GAME_STARS3)
        if (gained > 0) { practiceDays = practiceDays + LocalDate.now().toString(); prefs.edit().putStringSet("days", practiceDays).apply() }
        return gained
    }

    fun buy(item: Shop.Item): Boolean {
        if (item.id in owned || balance < item.price) return false
        spent += item.price; owned = owned + item.id
        prefs.edit().putInt("spent", spent).putStringSet("owned", owned).apply()
        equip(item)
        return true
    }
    /** Pay for a mystery box and get a random item you don't own yet (null when you can't). */
    fun openMystery(rnd: kotlin.random.Random = kotlin.random.Random): Shop.Item? {
        if (balance < Shop.MYSTERY_PRICE) return null
        val it = Shop.mystery(owned, rnd) ?: return null
        spent += Shop.MYSTERY_PRICE; owned = owned + it.id
        prefs.edit().putInt("spent", spent).putStringSet("owned", owned).apply()
        equip(it)
        return it
    }
    fun equip(item: Shop.Item?) { item ?: return; if (item.id in owned) setEquipped(item.slot, item.id) }
    fun unequip(slot: Shop.Slot) = setEquipped(slot, null)
    private fun setEquipped(slot: Shop.Slot, id: String?) {
        equipped = if (id == null) equipped - slot else equipped + (slot to id)
        prefs.edit().putString("eq.${slot.name}", id).apply()
        onLookChange()
    }
    var onLookChange: () -> Unit = {}

    /** The lesson finished last, so the Learn path can pop its tick in. Read once. */
    private var justCompleted: String? = null
    fun takeJustCompleted(): String? = justCompleted.also { justCompleted = null }

    /** Calm motion: fewer moving things (bubbles stop, Lou moves less). Starts on if the phone's own animations are off. */
    var reduceMotion by mutableStateOf(prefs.getBoolean("reduceMotion", runCatching {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false))); private set
    fun changeReduceMotion(v: Boolean) { reduceMotion = v; prefs.edit().putBoolean("reduceMotion", v).apply(); com.beatpath.app.ui.aero.Motion.reduce = v }

    /** Studio Piano roll: color notes by pitch (the Music basics rainbow). */
    var rainbowRoll by mutableStateOf(prefs.getBoolean("rainbowRoll", true)); private set
    fun changeRainbow(v: Boolean) { rainbowRoll = v; prefs.edit().putBoolean("rainbowRoll", v).apply() }

    fun finishOnboarding(jumpAhead: Boolean) {
        onboarded = true
        if (jumpAhead) unlockAll = true
        prefs.edit().putBoolean("onboarded", true).putBoolean("unlockAll", unlockAll).apply()
    }
    fun changeUnlockAll(v: Boolean) { unlockAll = v; prefs.edit().putBoolean("unlockAll", v).apply() }
    fun changeSfx(v: Boolean) { sfxOn = v; prefs.edit().putBoolean("sfx", v).apply() }

    fun isUnlocked(id: String): Boolean {
        if (unlockAll) return true
        val i = order.indexOf(id)
        // anything you've already passed past stays open (progress from before Music basics was added keeps working)
        return i <= 0 || order[i - 1] in completed || id in completed || order.drop(i + 1).any { it in completed }
    }

    /** The first unfinished lesson after the furthest one you've done, else the first unfinished one. */
    fun nextLesson(): String? {
        val furthest = order.indexOfLast { it in completed }
        return order.drop(furthest + 1).firstOrNull { it !in completed && isUnlocked(it) } ?: order.firstOrNull { it !in completed && isUnlocked(it) }
    }

    /** Repeats earn reduced XP (plan §8); the attempt id guards against double awards. */
    fun recordLesson(lessonId: String, attemptId: String, allUnassisted: Boolean): Int {
        val seen = prefs.getStringSet("attempts", emptySet())!!
        if (attemptId in seen) return 0
        val gained = if (lessonId in completed) 5 else 10 + if (allUnassisted) 5 else 0
        xp += gained; bump(Quests.Metric.LESSONS); bump(Quests.Metric.XP, gained)
        completed = completed + lessonId
        justCompleted = lessonId
        if (allUnassisted) unassisted = unassisted + lessonId
        practiceDays = practiceDays + LocalDate.now().toString()
        // lessons you needed help with come back for review (1, 3, 7 days); solo passes push them out, then retire them
        val next = com.beatpath.app.lessons.Practice.nextReview(reviews[lessonId], allUnassisted, LocalDate.now().toEpochDay())
        reviews = if (next == null) reviews - lessonId else reviews + (lessonId to next)
        savePairs("reviews", reviews)
        prefs.edit().putInt("xp", xp).putStringSet("completed", completed).putStringSet("unassisted", unassisted)
            .putStringSet("days", practiceDays).putStringSet("attempts", seen + attemptId).apply()
        return gained
    }

    fun dueReviews(): List<String> { val today = LocalDate.now().toEpochDay(); return order.filter { id -> reviews[id]?.let { it.first <= today } == true } }

    /** Practice XP and the day counts toward your streak; it never touches course completion. */
    fun recordPractice(attemptId: String, gained: Int): Int {
        val seen = prefs.getStringSet("attempts", emptySet())!!
        if (attemptId in seen) return 0
        xp += gained; practiceDays = practiceDays + LocalDate.now().toString()
        bump(Quests.Metric.PRACTICE); bump(Quests.Metric.XP, gained)
        prefs.edit().putInt("xp", xp).putStringSet("days", practiceDays).putStringSet("attempts", seen + attemptId).apply()
        return gained
    }

    fun recordSkill(skillId: String, ok: Boolean) {
        if (ok) bump(Quests.Metric.EAR_RIGHT)
        val (a, c) = skills[skillId] ?: (0 to 0)
        skills = skills + (skillId to ((a + 1) to (c + if (ok) 1 else 0)))
        savePairs("skills", skills.mapValues { it.value.first.toLong() to it.value.second })
    }

    /** Smoothed accuracy (starts at 50% until you've answered a few). */
    fun accuracy(skillId: String): Float = skills[skillId]?.let { (a, c) -> (c + 1f) / (a + 2f) } ?: 0.5f
    fun attempts(skillId: String): Int = skills[skillId]?.first ?: 0

    fun courseComplete(): Boolean = order.all { it in completed }

    /** Debug builds only (emulator checks): mark the course finished, half of it solo, and 3 reviews due today. */
    /** Debug builds only: add XP to test the shop. */
    fun debugAddXp(n: Int) { xp += n; prefs.edit().putInt("xp", xp).apply() }

    fun debugCompleteAll() {
        completed = order.toSet(); unassisted = order.filterIndexed { i, _ -> i % 2 == 0 }.toSet()
        val today = LocalDate.now().toEpochDay()
        reviews = order.filter { it !in unassisted }.take(3).associateWith { today to 0 }
        prefs.edit().putStringSet("completed", completed).putStringSet("unassisted", unassisted).putBoolean("onboarded", true).apply()
        onboarded = true; savePairs("reviews", reviews)
    }

    private fun loadPairs(key: String): Map<String, Pair<Long, Int>> = runCatching {
        val o = JSONObject(prefs.getString(key, "{}")!!)
        o.keys().asSequence().associateWith { k -> o.getJSONArray(k).let { it.getLong(0) to it.getInt(1) } }
    }.getOrDefault(emptyMap())
    private fun savePairs(key: String, m: Map<String, Pair<Long, Int>>) {
        val o = JSONObject(); m.forEach { (k, v) -> o.put(k, JSONArray(listOf(v.first, v.second))) }
        prefs.edit().putString(key, o.toString()).apply()
    }

    fun streak(): Int {
        var d = LocalDate.now()
        if (d.toString() !in practiceDays) d = d.minusDays(1)
        var n = 0
        while (d.toString() in practiceDays) { n++; d = d.minusDays(1) }
        return n
    }

    // ---------------- projects ----------------
    fun saveProject(p: Project) { projects = listOf(p.copy(editedAt = System.currentTimeMillis())) + projects.filterNot { it.id == p.id }; persist() }
    fun deleteProject(id: Long): Project? { val p = projects.firstOrNull { it.id == id } ?: return null; projects = projects.filterNot { it.id == id }; persist(); return p }
    fun restoreProject(p: Project) { projects = (projects + p).sortedByDescending { it.editedAt }; persist() }
    private fun persist() { val a = JSONArray(); projects.forEach { a.put(toJson(it)) }; prefs.edit().putString("projects", a.toString()).apply() }
    private fun loadProjects(): List<Project> = runCatching {
        val a = JSONArray(prefs.getString("projects", "[]")); (0 until a.length()).mapNotNull { runCatching { fromJson(a.getJSONObject(it)) }.getOrNull() }
    }.getOrDefault(emptyList())

    private fun toJson(p: Project) = JSONObject().apply {
        put("schema", 2); put("id", p.id); put("name", p.name); put("bpm", p.bpm); put("editedAt", p.editedAt)
        patternJson(p.pattern, this)
        put("mix", mixJson(p.mix))
        put("current", p.current); put("songMode", p.songMode); put("songBars", p.songBars)
        put("others", JSONObject().apply { p.others.forEach { (i, pat) -> put(i.toString(), JSONObject().also { patternJson(pat, it) }) } })
        put("playlist", JSONArray().apply { p.playlist.forEach { c -> put(JSONArray(listOf(c.pattern, c.startBar, c.track))) } })
    }

    private fun patternJson(pat: Pattern, o: JSONObject) {
        o.put("bars", pat.bars); o.put("swing", pat.swing.toDouble())
        o.put("channels", JSONArray().apply {
            pat.channels.forEach { ch -> put(JSONObject().apply {
                put("kind", ch.kind.name); put("muted", ch.muted); put("vol", ch.volume.toDouble()); put("pan", ch.pan.toDouble())
                put("notes", JSONArray().apply { ch.notes.forEach { n -> put(JSONArray(listOf(n.id, n.pitch, n.startTick, n.durationTicks, n.velocity))) } })
            }) }
        })
    }

    private fun fromJson(o: JSONObject): Project {
        val others = o.optJSONObject("others")?.let { obj -> obj.keys().asSequence().associate { k -> k.toInt() to patternFrom(obj.getJSONObject(k)) } } ?: emptyMap()
        val playlist = o.optJSONArray("playlist")?.let { a -> (0 until a.length()).map { i -> val c = a.getJSONArray(i); com.beatpath.app.model.Clip(c.getInt(0), c.getInt(1), c.optInt(2)) } } ?: emptyList()
        return Project(o.getLong("id"), o.getString("name"), o.getInt("bpm"), patternFrom(o), o.optJSONObject("mix")?.let { mixFrom(it) } ?: Mix(), o.getLong("editedAt"),
            o.optInt("current"), others, playlist, o.optBoolean("songMode"), o.optInt("songBars", 8))
    }

    private fun patternFrom(o: JSONObject): Pattern {
        val chans = o.getJSONArray("channels")
        val channels = (0 until chans.length()).mapNotNull { i ->
            val c = chans.getJSONObject(i)
            val kind = runCatching { ChannelKind.valueOf(c.getString("kind")) }.getOrNull() ?: return@mapNotNull null
            val notes = c.getJSONArray("notes")
            Channel(kind, c.optBoolean("muted"), c.optDouble("vol", 0.8).toFloat(), c.optDouble("pan", 0.0).toFloat(),
                (0 until notes.length()).map { j -> val a = notes.getJSONArray(j); Note(a.getLong(0), a.getInt(1), a.getInt(2), a.getInt(3), a.getInt(4)) })
        }
        val full = ChannelKind.entries.map { k -> channels.firstOrNull { it.kind == k } ?: Channel(k) }
        return Pattern(o.optInt("bars", 1), full, o.optDouble("swing", 0.0).toFloat())
    }

    private fun mixJson(m: Mix) = JSONObject().apply {
        put("inserts", JSONObject().apply { m.inserts.forEach { (k, i) -> put(k.name, JSONArray(listOf(i.volumeDb, i.pan, i.reverbSend, i.delaySend,
            i.eq.enabled, i.eq.lowCutHz, i.eq.lowGainDb, i.eq.lowFreq, i.eq.midGainDb, i.eq.midFreq, i.eq.midQ, i.eq.highGainDb, i.eq.highFreq,
            i.comp.enabled, i.comp.thresholdDb, i.comp.ratio, i.comp.attackMs, i.comp.releaseMs, i.comp.makeupDb,
            i.eq.lowMidGainDb, i.eq.lowMidFreq, i.eq.lowMidQ, i.eq.presGainDb, i.eq.presFreq, i.eq.presQ, i.eq.highCutHz, i.comp.sidechain?.name ?: "").map { it.toString() })) } })
        put("master", JSONArray(listOf(m.master.volumeDb, m.master.limiterOn, m.master.limiterGainDb, m.master.ceilingDb).map { it.toString() }))
        put("reverb", JSONArray(listOf(m.reverb.size, m.reverb.damping, m.reverb.wet, m.reverb.lowCutHz).map { it.toString() }))
        put("delay", JSONArray(listOf(m.delay.steps, m.delay.feedback, m.delay.damping, m.delay.wet).map { it.toString() }))
        put("synths", JSONObject().apply { m.synths.forEach { (k, s) -> put(k.name, JSONArray(listOf(s.wave.name, s.attackMs, s.decayMs, s.sustain, s.releaseMs, s.cutoffHz, s.resonance, s.detuneCents, s.octave,
            s.filterEnvOct, s.filterEnvDecayMs, s.lfoTarget.name, s.lfoSteps, s.lfoAmount).map { it.toString() })) } })
        put("kit", m.drumKit.name)
    }

    private fun mixFrom(o: JSONObject): Mix {
        fun JSONArray.f(i: Int) = getString(i).toFloat()
        fun JSONArray.b(i: Int) = getString(i).toBoolean()
        val ins = o.getJSONObject("inserts")
        val inserts = ChannelKind.entries.associateWith { k -> ins.optJSONArray(k.name)?.let { a ->
            fun fo(i: Int, d: Float) = if (a.length() > i) a.f(i) else d
            val sc = if (a.length() > 26) a.getString(26).takeIf { it.isNotEmpty() }?.let { runCatching { ChannelKind.valueOf(it) }.getOrNull() } else null
            InsertTrack(a.f(0), a.f(1), EqSettings(a.b(4), a.f(5), a.f(6), a.f(7), a.f(8), a.f(9), a.f(10), a.f(11), a.f(12),
                fo(19, 0f), fo(20, 350f), fo(21, 1f), fo(22, 0f), fo(23, 3500f), fo(24, 1f), fo(25, 20000f)),
                CompSettings(a.b(13), a.f(14), a.f(15), a.f(16), a.f(17), a.f(18), sc), a.f(2), a.f(3)) } ?: InsertTrack() }
        val ma = o.getJSONArray("master"); val ra = o.getJSONArray("reverb"); val da = o.getJSONArray("delay")
        val sy = o.getJSONObject("synths")
        val synths = Mix().synths.mapValues { (k, def) -> sy.optJSONArray(k.name)?.let { a -> SynthPatch(Wave.valueOf(a.getString(0)), a.f(1), a.f(2), a.f(3), a.f(4), a.f(5), a.f(6), a.f(7), a.getString(8).toInt(),
            if (a.length() > 9) a.f(9) else 0f, if (a.length() > 10) a.f(10) else 200f,
            if (a.length() > 11) com.beatpath.app.model.LfoTarget.valueOf(a.getString(11)) else com.beatpath.app.model.LfoTarget.OFF,
            if (a.length() > 12) a.getString(12).toInt() else 4, if (a.length() > 13) a.f(13) else 0.4f) } ?: def }
        return Mix(inserts, MasterSettings(ma.f(0), ma.b(1), ma.f(2), ma.f(3)), ReverbSettings(ra.f(0), ra.f(1), ra.f(2), ra.f(3)),
            DelaySettings(da.getString(0).toInt(), da.f(1), da.f(2), da.f(3)),
            runCatching { com.beatpath.app.model.DrumKit.valueOf(o.optString("kit", "CLASSIC")) }.getOrDefault(com.beatpath.app.model.DrumKit.CLASSIC), synths)
    }
}
