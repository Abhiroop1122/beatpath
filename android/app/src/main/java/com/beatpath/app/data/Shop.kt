package com.beatpath.app.data

/**
 * Lou's Shop: what XP buys. Everything is cosmetic and stays on the phone. Spending XP never lowers your level,
 * because the level comes from all the XP you've ever earned; the shop spends a separate balance.
 */
object Shop {
    enum class Slot(val label: String) { HAT("Hats"), FACE("Glasses"), SKIN("Lou colors"), SKY("Sky themes") }

    data class Item(val id: String, val slot: Slot, val name: String, val price: Int, val blurb: String)

    val items = listOf(
        Item("hat.party", Slot.HAT, "Party hat", 80, "For every lesson that ends in a cheer."),
        Item("hat.beanie", Slot.HAT, "Beanie", 150, "Lo-fi producer mode."),
        Item("hat.headphones", Slot.HAT, "Studio headphones", 220, "Lou hears every hi-hat."),
        Item("hat.crown", Slot.HAT, "Crown", 500, "King of the Piano roll."),
        Item("face.shades", Slot.FACE, "Shades", 180, "Too cool for a click track."),
        Item("face.hearts", Slot.FACE, "Heart glasses", 260, "In love with that chord."),
        Item("skin.coral", Slot.SKIN, "Coral Lou", 200, "Warm and bright."),
        Item("skin.grape", Slot.SKIN, "Grape Lou", 200, "A deep purple groove."),
        Item("skin.sunny", Slot.SKIN, "Sunny Lou", 250, "Golden hour."),
        Item("skin.midnight", Slot.SKIN, "Midnight Lou", 350, "For late-night sessions."),
        Item("skin.rainbow", Slot.SKIN, "Rainbow Lou", 800, "Cycles through every note color."),
        Item("sky.sunset", Slot.SKY, "Sunset sky", 300, "The menu turns golden pink."),
        Item("sky.dusk", Slot.SKY, "Starry dusk", 450, "Lavender sky with twinkling stars."),
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

    /** A random item you don't own yet (null when you own everything). */
    fun mystery(owned: Set<String>, rnd: kotlin.random.Random): Item? = items.filter { it.id !in owned }.randomOrNull(rnd)
}
