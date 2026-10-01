package io.github.awakelol.rex.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScorerTest {

    private val approved = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.facebook.katana" to "Facebook",
        "com.android.chrome" to "Chrome",
    )

    private fun playApp(label: String, pkg: String = "com.example.app") =
        PackageFacts(packageName = pkg, label = label, installer = "com.android.vending")

    @Test
    fun `plain store app is low`() {
        val report = RiskScorer.score(playApp("Weather Today"), approved)
        assertEquals(RiskLevel.LOW, report.level)
        assertTrue(report.reasons.isEmpty())
    }

    @Test
    fun `galaxy store counts as trusted`() {
        val facts = playApp("Weather").copy(installer = "com.sec.android.app.samsungapps")
        assertEquals(RiskLevel.LOW, RiskScorer.score(facts, approved).level)
    }

    @Test
    fun `sideloaded app is high`() {
        val facts = playApp("Weather").copy(installer = "com.google.android.packageinstaller")
        val report = RiskScorer.score(facts, approved)
        assertEquals(RiskLevel.HIGH, report.level)
        assertEquals(1, report.reasons.size)
    }

    @Test
    fun `unknown installer is high`() {
        val facts = playApp("Weather").copy(installer = null)
        assertEquals(RiskLevel.HIGH, RiskScorer.score(facts, approved).level)
    }

    @Test
    fun `hash prefixed clone of an installed app is high`() {
        val report = RiskScorer.score(playApp("#WhatsApp", pkg = "com.whatsapp.fake"), approved)
        assertEquals(RiskLevel.HIGH, report.level)
        assertTrue(report.reasons.any { "WhatsApp" in it && "same name" in it })
        assertTrue(report.reasons.any { "symbol" in it })
    }

    @Test
    fun `clone match ignores case spaces and symbols`() {
        val report = RiskScorer.score(playApp("face book!", pkg = "com.fb.lite.fake"), approved)
        assertEquals(RiskLevel.HIGH, report.level)
    }

    @Test
    fun `same package with same name is not a clone`() {
        val report = RiskScorer.score(playApp("WhatsApp", pkg = "com.whatsapp"), approved)
        assertEquals(RiskLevel.LOW, report.level)
    }

    @Test
    fun `each high permission signal alone is high`() {
        val base = playApp("Torch")
        listOf(
            base.copy(drawsOverApps = true),
            base.copy(hasAccessibilityService = true),
            base.copy(hasDeviceAdmin = true),
            base.copy(isHomeApp = true),
        ).forEach { facts ->
            assertEquals(facts.toString(), RiskLevel.HIGH, RiskScorer.score(facts, approved).level)
        }
    }

    @Test
    fun `one medium signal is medium`() {
        val report = RiskScorer.score(playApp("Torch").copy(installsApps = true), approved)
        assertEquals(RiskLevel.MEDIUM, report.level)
        assertEquals(1, report.reasons.size)
    }

    @Test
    fun `two medium signals are high`() {
        val facts = playApp("Torch").copy(installsApps = true, readsNotifications = true)
        assertEquals(RiskLevel.HIGH, RiskScorer.score(facts, approved).level)
    }

    @Test
    fun `bait names are medium`() {
        listOf("Super Cleaner", "Phone Booster Pro", "Battery Saver", "Photo Recovery").forEach { label ->
            assertEquals(label, RiskLevel.MEDIUM, RiskScorer.score(playApp(label), approved).level)
        }
    }

    @Test
    fun `bait name plus odd name is high`() {
        val report = RiskScorer.score(playApp("#1 Antivirus"), approved)
        assertEquals(RiskLevel.HIGH, report.level)
        assertEquals(2, report.reasons.size)
    }

    @Test
    fun `normalize strips symbols and case`() {
        assertEquals("whatsapp", RiskScorer.normalize("#Whats App™"))
        assertEquals("", RiskScorer.normalize("***"))
    }
}
