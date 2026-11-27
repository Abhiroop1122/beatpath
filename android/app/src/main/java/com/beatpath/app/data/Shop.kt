package com.beatpath.app.data

/**
 * Lou's Shop: what XP buys. Everything is something Lou wears, and it all stays on the phone. Spending XP never
 * lowers your level, because the level comes from all the XP you've ever earned; the shop spends a separate balance.
 */
object Shop {
    enum class Slot(val label: String) { HAT("Hats"), FACE("Glasses"), NECK("Extras"), SKIN("Colors") }
    enum class Rarity(val label: String) { COMMON("Common"), RARE("Rare"), EPIC("Epic"), LEGENDARY("Legendary") }

    data class Item(val id: String, val slot: Slot, val name: String, val price: Int, val blurb: String) {
        val rarity: Rarity get() = when { price >= 700 -> Rarity.LEGENDARY; price >= 400 -> Rarity.EPIC; price >= 200 -> Rarity.RARE; else -> Rarity.COMMON }
    }

    val items = listOf(
        Item("hat.party", Slot.HAT, "Party hat", 80, "For every lesson that ends in a cheer."),
        Item("hat.cap", Slot.HAT, "Cool cap", 120, "Backwards? Never. Sideways? Always."),
        Item("hat.beanie", Slot.HAT, "Beanie", 150, "Cozy beats for cold days."),
        Item("hat.chef", Slot.HAT, "Chef hat", 180, "Cooks a mean chord cake."),
        Item("hat.headphones", Slot.HAT, "Studio headphones", 220, "Lou hears every hi-hat."),
        Item("hat.flowers", Slot.HAT, "Flower crown", 280, "A garden that hums along."),
        Item("hat.wizard", Slot.HAT, "Wizard hat", 450, "Turns notes into magic."),
        Item("hat.crown", Slot.HAT, "Crown", 750, "King of the Piano roll."),
        Item("face.mustache", Slot.FACE, "Fancy mustache", 140, "Very distinguished."),
        Item("face.shades", Slot.FACE, "Shades", 180, "Too cool for a click track."),
        Item("face.stars", Slot.FACE, "Star glasses", 240, "Seeing stars, in a good way."),
        Item("face.hearts", Slot.FACE, "Heart glasses", 260, "In love with that chord."),
        Item("face.monocle", Slot.FACE, "Monocle", 420, "Inspects every note."),
        Item("neck.bowtie", Slot.NECK, "Bow tie", 100, "Dressed up for the show."),
        Item("neck.scarf", Slot.NECK, "Scarf", 160, "Swishes when he dances."),
        Item("neck.chain", Slot.NECK, "Gold chain", 480, "A real hitmaker's chain."),
        Item("skin.coral", Slot.SKIN, "Coral Lou", 200, "Warm and bright."),
        Item("skin.mint", Slot.SKIN, "Mint Lou", 200, "Fresh as a new beat."),
        Item("skin.grape", Slot.SKIN, "Grape Lou", 220, "A deep purple groove."),
        Item("skin.bubblegum", Slot.SKIN, "Bubblegum Lou", 260, "Pop music, literally."),
        Item("skin.sunny", Slot.SKIN, "Sunny Lou", 300, "Golden hour."),
        Item("skin.midnight", Slot.SKIN, "Midnight Lou", 400, "For late-night sessions."),
        Item("skin.rainbow", Slot.SKIN, "Rainbow Lou", 900, "Cycles through every note color."),
    )
    fun item(id: String?) = items.firstOrNull { it.id == id }

    const val MYSTERY_PRICE = 120

    /** Level from lifetime XP: each level needs 50 more XP than the one before (100, 150, 200…). */
    fun level(totalXp: Int): Int { var lv = 1; var need = 100; var left = totalXp; while (left >= need) { left -= need; lv++; need += 50 }; return lv }
    /** XP into the current level and XP that level needs. */
    fun levelProgress(totalXp: Int): Pair<Int, Int> { var need = 100; var left = totalXp; while (left >= need) { left -= need; need += 50 }; return left to need }
    fun title(level: Int) = when {
        level >= 20 -> "Hitmaker"
        level >= 15 -> "Producer"
        level >= 10 -> "Songwriter"
        level >= 7 -> "Melody maker"
        level >= 4 -> "Beat maker"
        else -> "Beginner"
    }

    /** A random item you don't own yet (null when you own everything). Cheaper items are a little more likely. */
    fun mystery(owned: Set<String>, rnd: kotlin.random.Random): Item? {
        val left = items.filter { it.id !in owned }
        if (left.isEmpty()) return null
        val weights = left.map { 1.0 / (1 + it.price / 200.0) }
        var r = rnd.nextDouble() * weights.sum()
        left.forEachIndexed { i, it -> r -= weights[i]; if (r <= 0) return it }
        return left.last()
    }
}

/**
 * Daily quests: three small goals a day, picked from a pool by the date (so they're the same all day),
 * plus streak chests on days 3, 7, 14 and 30 in a row.
 */
object Quests {
    enum class Metric { LESSONS, GAMES, GAME_STARS3, EAR_RIGHT, PRACTICE, XP }
    data class Quest(val id: String, val text: String, val metric: Metric, val target: Int, val reward: Int)

    val pool = listOf(
        Quest("q.lesson1", "Finish a lesson", Metric.LESSONS, 1, 20),
        Quest("q.lesson2", "Finish 2 lessons", Metric.LESSONS, 2, 35),
        Quest("q.game2", "Play 2 mini games", Metric.GAMES, 2, 25),
        Quest("q.game3", "Play 3 mini games", Metric.GAMES, 3, 35),
        Quest("q.stars3", "Get 3 stars in a mini game", Metric.GAME_STARS3, 1, 40),
        Quest("q.ear5", "Get 5 ear questions right", Metric.EAR_RIGHT, 5, 25),
        Quest("q.practice", "Do a Daily mix or a drill", Metric.PRACTICE, 1, 25),
        Quest("q.xp60", "Earn 60 XP today", Metric.XP, 60, 30),
    )

    /** Today's three quests: always one lesson quest and one game quest, plus one more. */
    fun today(epochDay: Long): List<Quest> {
        val rnd = kotlin.random.Random(epochDay * 7919)
        val lesson = pool.filter { it.metric == Metric.LESSONS }.random(rnd)
        val game = pool.filter { it.metric == Metric.GAMES || it.metric == Metric.GAME_STARS3 }.random(rnd)
        val other = pool.filter { it.metric !in setOf(Metric.LESSONS, Metric.GAMES, Metric.GAME_STARS3) }.random(rnd)
        return listOf(lesson, game, other)
    }

    /** Streak days that open a chest, and its XP. */
    val chests = linkedMapOf(3 to 50, 7 to 120, 14 to 250, 30 to 500)
}
