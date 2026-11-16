package dev.limeroot.research

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticReportTest {
    private fun sample(): DiagnosticSnapshot = DiagnosticSnapshot(
        generatedAt = "2026-11-16 20:00:00",
        device = DeviceDetails(
            manufacturer = "Xiaomi",
            brand = "Redmi",
            model = "Redmi 9T",
            device = "lemon",
            product = "lemon",
            board = "qcom",
            hardware = "qcom",
            android = "Android 12",
            sdk = 31,
            build = "V14-test",
            buildType = "user",
            buildTags = "release-keys",
            securityPatch = "2023-04-01",
            kernel = "4.19.157",
            architecture = "aarch64",
            abis = "arm64-v8a",
            fingerprint = "Xiaomi/lemon/lemon:12/test/release-keys",
            bootloaderState = "Заблокирован",
            verifiedBootState = "GREEN",
            vbmetaState = "locked",
            verityMode = "enforcing",
            selinuxState = "Enforcing",
            selinuxContext = "u:r:untrusted_app:s0",
            capEff = "0000000000000000",
            noNewPrivs = "1",
            seccomp = "2",
            appDebuggable = false,
            runtimeMaxMemoryMb = 256,
            rootPathsFound = emptyList()
        ),
        checks = listOf(
            DiagnosticCheck("Загрузка", "Verified Boot", "GREEN", "Цепочка загрузки.", CheckSeverity.GOOD),
            DiagnosticCheck("Root", "su paths", "Не обнаружены", "Ограниченная проверка.", CheckSeverity.INFO),
            DiagnosticCheck("SELinux", "SELinux", "Permissive", "Ослабленная защита.", CheckSeverity.CRITICAL)
        ),
        notes = listOf("Тестовое примечание")
    )

    @Test
    fun reportContainsDeviceAndAllCheckResults() {
        val report = DiagnosticReport.format(sample())
        assertTrue(report.contains("LIMEROOT — ПОЛНЫЙ ОТЧЁТ ДИАГНОСТИКИ"))
        assertTrue(report.contains("Redmi 9T"))
        assertTrue(report.contains("Build fingerprint"))
        assertTrue(report.contains("Verified Boot"))
        assertTrue(report.contains("Тестовое примечание"))
        assertTrue(report.contains("Критические признаки: 1"))
    }

    @Test
    fun reportExplainsPrivacyAndDiagnosticLimits() {
        val report = DiagnosticReport.format(sample())
        assertTrue(report.contains("IMEI"))
        assertTrue(report.contains("местоположение"))
        assertTrue(report.contains("не доказывает отсутствие"))
        assertFalse(report.contains("password="))
    }
}
