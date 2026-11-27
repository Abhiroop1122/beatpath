package com.beatpath.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.beatpath.app.audio.AudioEngine
import com.beatpath.app.audio.Sfx
import com.beatpath.app.data.EarIds
import com.beatpath.app.data.Store
import com.beatpath.app.lessons.Basics
import com.beatpath.app.lessons.Curriculum
import com.beatpath.app.ui.lesson.BasicsLessonScreen
import com.beatpath.app.lessons.Practice
import com.beatpath.app.model.Project
import com.beatpath.app.ui.home.HomeScaffold
import com.beatpath.app.ui.home.LearnPath
import com.beatpath.app.ui.home.LoopsScreen
import com.beatpath.app.ui.home.MeScreen
import com.beatpath.app.ui.home.PracticeScreen
import com.beatpath.app.ui.games.SkyHop
import com.beatpath.app.ui.games.PianoTiles
import com.beatpath.app.ui.games.BeatRun
import com.beatpath.app.ui.games.BalloonPop
import com.beatpath.app.ui.games.ChordCake
import com.beatpath.app.ui.games.EchoLou
import com.beatpath.app.ui.home.Tab
import com.beatpath.app.ui.home.WelcomeScreen
import com.beatpath.app.ui.lesson.EarLessonScreen
import com.beatpath.app.ui.lesson.EarLessons
import com.beatpath.app.ui.lesson.LessonCompleteScreen
import com.beatpath.app.ui.lou.LouGallery
import com.beatpath.app.ui.studio.StudioScreen
import com.beatpath.app.ui.theme.BeatpathTheme

sealed interface Route {
    data object Welcome : Route
    data class Home(val tab: Tab) : Route
    /** [from] is the tab you came from, so leaving always lands you back where you were. */
    data class Ear(val id: String, val from: Tab = Tab.Learn, val run: Long = System.nanoTime()) : Route
    data class Shop(val from: Tab = Tab.Learn) : Route
    data class Game(val id: String, val from: Tab = Tab.Studio, val run: Long = System.nanoTime()) : Route
    data class Basics(val id: String, val from: Tab = Tab.Learn, val run: Long = System.nanoTime()) : Route
    data class Studio(val project: Project, val lessonId: String? = null, val from: Tab = Tab.Learn, val run: Long = System.nanoTime()) : Route
    data class Complete(val lessonId: String, val xp: Int, val unassisted: Boolean, val seconds: Int, val from: Tab = Tab.Learn) : Route
}

class MainActivity : ComponentActivity() {
    private val engine = AudioEngine()
    private val sfx by lazy { Sfx(engine) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.beatpath.app.data.CrashLog.install(applicationContext)
        enableEdgeToEdge()
        val store = Store(applicationContext)
        com.beatpath.app.lessons.Practice.accuracy = { store.accuracy(it.id) }
        val debuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable && intent?.getBooleanExtra("debug_complete_all", false) == true) store.debugCompleteAll()
        if (debuggable) intent?.getIntExtra("debug_xp", 0)?.takeIf { it > 0 }?.let { store.debugAddXp(it) }
        com.beatpath.app.ui.games.GameDebug.autoplay = debuggable && intent?.getBooleanExtra("autoplay", false) == true
        engine.sfxEnabled = store.sfxOn
        fun applyLook() {
            val eq = store.equipped
            com.beatpath.app.ui.lou.LouLook.outfit = com.beatpath.app.ui.lou.LouOutfit(eq[com.beatpath.app.data.Shop.Slot.HAT], eq[com.beatpath.app.data.Shop.Slot.FACE],
                eq[com.beatpath.app.data.Shop.Slot.SKIN], eq[com.beatpath.app.data.Shop.Slot.NECK])
        }
        applyLook(); store.onLookChange = { applyLook() }
        com.beatpath.app.ui.aero.Motion.reduce = store.reduceMotion
        com.beatpath.app.model.NoteColors.rainbowRoll = store.rainbowRoll
        com.beatpath.app.model.NoteColors.onRainbowChange = { store.changeRainbow(it) }
        val gallery = intent?.getBooleanExtra("lou_gallery", false) == true
        val startRoute = intent?.getStringExtra("route")
        setContent {
            if (gallery) { LouGallery(); return@setContent }
            BeatpathTheme {
                var route by remember {
                    mutableStateOf<Route>(when {
                        startRoute != null && (Curriculum.byId(startRoute) != null || startRoute == Practice.DICTATION) -> Route.Studio(Project(1, "Debug"), startRoute)
                        startRoute != null && (startRoute in EarIds.all || Practice.isEar(startRoute)) -> Route.Ear(startRoute)
                        startRoute != null && Basics.byId(startRoute) != null -> Route.Basics(startRoute)
                        startRoute == "practice" -> Route.Home(Tab.Practice)
                        startRoute == "shop" -> Route.Shop()
                        startRoute != null && com.beatpath.app.ui.games.Games.byId(startRoute) != null -> Route.Game(startRoute)
                        startRoute == "games" -> Route.Home(Tab.Studio)
                        startRoute == "home" -> Route.Home(Tab.Learn)
                        startRoute == "welcome" -> Route.Welcome
                        startRoute == "complete" -> Route.Complete("debug", 15, true, 74)
                        startRoute == "studio" -> Route.Studio(Project(System.currentTimeMillis(), "Untitled loop"), from = Tab.Studio)
                        store.onboarded -> Route.Home(Tab.Learn)
                        else -> Route.Welcome
                    })
                }
                fun start(id: String, from: Tab = Tab.Learn) {
                    route = if (Basics.byId(id) != null) Route.Basics(id, from)
                    else if (id in EarIds.all || Practice.isEar(id)) Route.Ear(id, from)
                    else Route.Studio(Project(System.currentTimeMillis(), Curriculum.byId(id)?.title ?: if (id == Practice.DICTATION) "Dictation" else "Lesson"), id, from)
                }
                // system Back: other tabs → Learn tab; Lesson complete → where you came from (Learn otherwise exits the app)
                androidx.activity.compose.BackHandler(enabled = (route as? Route.Home)?.tab?.let { it != Tab.Learn } == true) { route = Route.Home(Tab.Learn) }
                androidx.activity.compose.BackHandler(enabled = route is Route.Complete) { route = Route.Home((route as Route.Complete).from) }
                AnimatedContent(route, transitionSpec = {
                    // Apple-like: content eases in with a soft spring scale while the old screen fades away
                    (fadeIn(androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f))) +
                        androidx.compose.animation.scaleIn(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 380f), initialScale = 0.96f)) togetherWith
                        (fadeOut(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.scaleOut(androidx.compose.animation.core.tween(220), targetScale = 1.015f))
                }, contentKey = { it::class }, label = "route") { r ->
                    when (r) {
                        Route.Welcome -> WelcomeScreen(sfx,
                            onStart = { store.finishOnboarding(false); route = Route.Basics(Basics.ids.first()) },
                            onJump = { store.finishOnboarding(true); route = Route.Home(Tab.Learn) })
                        is Route.Home -> HomeScaffold(r.tab, sfx, onTab = { route = Route.Home(it) }) {
                            when (r.tab) {
                                Tab.Learn -> LearnPath(store, sfx, onPractice = { route = Route.Home(Tab.Practice) },
                                    onShop = { route = Route.Shop(Tab.Learn) },
                                    onSong = { route = Route.Studio(Project(System.currentTimeMillis(), "My song", pattern = com.beatpath.app.model.Pattern(4)), from = Tab.Learn) }) { start(it, Tab.Learn) }
                                Tab.Practice -> PracticeScreen(store, sfx) { start(it, Tab.Practice) }
                                Tab.Studio -> com.beatpath.app.ui.games.StudioHub(store, sfx, onFree = { route = Route.Studio(Project(System.currentTimeMillis(), "Untitled loop"), from = Tab.Studio) },
                                    onSong = { route = Route.Studio(Project(System.currentTimeMillis(), "My song", songMode = true), from = Tab.Studio) },
                                    onGame = { route = Route.Game(it, Tab.Studio) })
                                Tab.Loops -> LoopsScreen(store, engine, sfx, onOpen = { route = Route.Studio(it, from = Tab.Loops) }, onNew = { route = Route.Studio(Project(System.currentTimeMillis(), "Untitled loop"), from = Tab.Loops) })
                                Tab.Me -> MeScreen(store, engine) { route = Route.Shop(Tab.Me) }
                            }
                        }
                        is Route.Ear -> EarLessonScreen(remember(r.run) { EarLessons.spec(r.id) }, engine, sfx,
                            onExit = { route = Route.Home(r.from) },
                            onFinished = { un, attempt, secs ->
                                sfx.complete()
                                val xp = if (Practice.isPractice(r.id)) store.recordPractice(attempt, if (un) 10 else 8) else store.recordLesson(r.id, attempt, un)
                                route = Route.Complete(r.id, xp, un, secs, r.from)
                            },
                            onAnswer = { kind, ok -> store.recordSkill(Practice.skillOf(kind).id, ok) })
                        is Route.Basics -> BasicsLessonScreen(remember(r.run) { Basics.byId(r.id)!! }, engine, sfx,
                            onExit = { route = Route.Home(r.from) },
                            onFinished = { un, attempt, secs ->
                                sfx.complete()
                                route = Route.Complete(r.id, store.recordLesson(r.id, attempt, un), un, secs, r.from)
                            })
                        is Route.Studio -> StudioScreen(engine, sfx, r.project, Curriculum.byId(r.lessonId) ?: remember(r.run) { Practice.lesson(r.lessonId, r.run) },
                            onSave = { store.saveProject(it) },
                            onExit = { route = Route.Home(r.from) },
                            onLessonDone = { id, attempt, un ->
                                val xp = if (Practice.isPractice(id)) { store.recordSkill(Practice.Skill.DICTATION.id, un); store.recordPractice(attempt, if (un) 14 else 10) }
                                         else store.recordLesson(id, attempt, un)
                                route = Route.Complete(id, xp, un, 0, r.from)
                            })
                        is Route.Complete -> LessonCompleteScreen(sfx, r.xp, r.unassisted, r.seconds,
                            onContinue = { route = Route.Home(r.from) }, onAgain = { start(r.lessonId, r.from) },
                            balance = store.balance, onShop = { route = Route.Shop(r.from) })
                        is Route.Game -> {
                            val info = com.beatpath.app.ui.games.Games.byId(r.id)!!
                            com.beatpath.app.ui.games.GameShell(info, store.best(r.id), sfx, onExit = { route = Route.Home(r.from) },
                                onResult = { score, stars -> store.recordGame(r.id, score, stars) }) { control ->
                                val G = com.beatpath.app.ui.games.Games
                                when (r.id) {
                                    G.pianoTiles.id -> PianoTiles(control, engine, sfx)
                                    G.skyHop.id -> SkyHop(control, engine, sfx)
                                    G.beatRun.id -> BeatRun(control, engine, sfx)
                                    G.balloonPop.id -> BalloonPop(control, engine, sfx)
                                    G.chordCake.id -> ChordCake(control, engine, sfx)
                                    else -> EchoLou(control, engine, sfx)
                                }
                            }
                        }
                        is Route.Shop -> com.beatpath.app.ui.home.ShopScreen(store, sfx) { route = Route.Home(r.from) }
                    }
                }
            }
        }
    }

    private val audioManager by lazy { getSystemService(android.media.AudioManager::class.java) }
    /** Calls and other apps take the audio: stop the music so it never plays over them. */
    private val focusRequest by lazy {
        android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_GAME).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setOnAudioFocusChangeListener { change -> if (change < 0) runOnUiThread { engine.interrupt() } }
            .build()
    }
    /** Headphones pulled out: stop at once instead of blasting the speaker. */
    private val noisy = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: android.content.Context?, i: android.content.Intent?) { if (i?.action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY) engine.interrupt() }
    }
    override fun onResume() {
        super.onResume(); engine.start()
        runCatching { audioManager.requestAudioFocus(focusRequest) }
        androidx.core.content.ContextCompat.registerReceiver(this, noisy, android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    override fun onPause() {
        runCatching { unregisterReceiver(noisy) }; runCatching { audioManager.abandonAudioFocusRequest(focusRequest) }
        engine.interrupt(); engine.release(); super.onPause()
    }
}
