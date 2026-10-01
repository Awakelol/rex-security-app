package io.github.awakelol.rex.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class AlertTextTest {

    @Test
    fun `new app message includes all details`() {
        val report = RiskReport(RiskLevel.HIGH, listOf("Asks to draw over other apps."))
        val msg = AlertText.newApp(
            label = "Super Cleaner",
            packageName = "com.shady.cleaner",
            installer = "com.android.vending",
            report = report,
            time = Instant.parse("2026-10-02T14:03:00Z"),
            zone = ZoneOffset.UTC,
        )
        assertTrue(msg.contains("Super Cleaner"))
        assertTrue(msg.contains("com.shady.cleaner"))
        assertTrue(msg.contains("Google Play Store"))
        assertTrue(msg.contains("HIGH"))
        assertTrue(msg.contains("- Asks to draw over other apps."))
        assertTrue(msg.contains("14:03"))
    }

    @Test
    fun `very long messages fit discord limit`() {
        val report = RiskReport(RiskLevel.HIGH, List(200) { "Reason number $it is quite long." })
        val msg = AlertText.newApp("X", "x.y", null, report, Instant.EPOCH, ZoneOffset.UTC)
        assertTrue(msg.length <= 2000)
    }
}
