package com.aura.app

import com.aura.app.core.assistant.AssistantCommand
import com.aura.app.core.assistant.LocalCommandRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantCommandRouterTest {

    @Test
    fun testParse_playRainSounds() {
        val cmd = LocalCommandRouter.parse("play rain sounds")
        assertTrue(cmd is AssistantCommand.PlayRainSounds)
    }

    @Test
    fun testParse_startBreathingWithDuration() {
        val cmd = LocalCommandRouter.parse("start 10 minute breathing")
        assertTrue(cmd is AssistantCommand.StartBreathing)
        assertEquals(10, (cmd as AssistantCommand.StartBreathing).minutes)
    }

    @Test
    fun testParse_startFocusDefaultDuration() {
        val cmd = LocalCommandRouter.parse("start focus session")
        assertTrue(cmd is AssistantCommand.StartFocus)
        assertEquals(25, (cmd as AssistantCommand.StartFocus).minutes)
    }

    @Test
    fun testParse_startFocusCustomDuration() {
        val cmd = LocalCommandRouter.parse("start 45 min focus")
        assertTrue(cmd is AssistantCommand.StartFocus)
        assertEquals(45, (cmd as AssistantCommand.StartFocus).minutes)
    }

    @Test
    fun testParse_recordThought() {
        val cmd = LocalCommandRouter.parse("record a thought")
        assertTrue(cmd is AssistantCommand.RecordThought)
    }

    @Test
    fun testParse_pauseMusic() {
        val cmd = LocalCommandRouter.parse("pause the music")
        assertTrue(cmd is AssistantCommand.PauseMusic)
    }

    @Test
    fun testParse_unknown() {
        val cmd = LocalCommandRouter.parse("what is the weather today?")
        assertTrue(cmd is AssistantCommand.Unknown)
    }
}
