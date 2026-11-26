package com.beatpath.app

import com.beatpath.app.model.ChannelKind
import com.beatpath.app.model.DrumKit
import com.beatpath.app.model.Mix
import com.beatpath.app.model.SoundPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundsTest {
    @Test fun everyMelodicChannelHasPresetsAndTheDefaultIsNamed() {
        val mix = Mix()
        for (k in ChannelKind.entries.filter { !it.isDrum }) {
            val presets = SoundPresets.forKind(k)
            assertTrue("$k has presets", presets.size >= 4)
            assertEquals("$k names are unique", presets.size, presets.map { it.name }.toSet().size)
            assertTrue("$k default sound is a named preset", SoundPresets.nameOf(k, mix.synth(k)) != "Custom")
        }
    }

    @Test fun choosingAPresetIsRecognisedAndKitsAreDistinct() {
        val bell = SoundPresets.forKind(ChannelKind.LEAD).first { it.name == "Bell" }
        val mix = Mix().withSynth(ChannelKind.LEAD) { bell.patch }
        assertEquals("Bell", SoundPresets.nameOf(ChannelKind.LEAD, mix.synth(ChannelKind.LEAD)))
        assertEquals(DrumKit.CLASSIC, Mix().drumKit)
        assertEquals(DrumKit.entries.size, DrumKit.entries.map { it.kickDecay to it.hatColor }.toSet().size)
    }
}
