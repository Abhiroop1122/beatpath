package com.beatpath.app

import com.beatpath.app.data.Shop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShopTest {
    @Test fun levelsGrowAndNeverSkip() {
        assertEquals(1, Shop.level(0)); assertEquals(1, Shop.level(99)); assertEquals(2, Shop.level(100))
        assertEquals(3, Shop.level(250)); assertEquals(4, Shop.level(450))
        var last = 1
        for (xp in 0..20_000 step 7) { val lv = Shop.level(xp); assertTrue(lv == last || lv == last + 1); last = lv }
        val (into, need) = Shop.levelProgress(260); assertEquals(10, into); assertEquals(200, need)
        assertEquals("Beginner", Shop.title(1)); assertEquals("Beat maker", Shop.title(4)); assertEquals("Hitmaker", Shop.title(25))
    }

    @Test fun mysteryOnlyGivesNewItems() {
        val owned = Shop.items.take(10).map { it.id }.toMutableSet()
        repeat(50) { val it = Shop.mystery(owned, Random(it))!!; assertTrue(it.id !in owned) }
        assertNull(Shop.mystery(Shop.items.map { it.id }.toSet(), Random(1)))
        assertEquals("ids are unique", Shop.items.size, Shop.items.map { it.id }.toSet().size)
        assertTrue(Shop.items.all { it.price > 0 })
    }
}
