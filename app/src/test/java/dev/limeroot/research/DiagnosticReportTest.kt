package dev.limeroot.research

import org.junit.Assert.assertEquals
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
        assertTrue(report.contains("ОЦЕНКА ПРИМЕНИМОСТИ CVE"))
        assertTrue(report.contains("CVE-2026-43284"))
        assertFalse(report.contains("password="))
    }
}


class RootReadinessAssessmentTest {
    private fun sample(kernel:String="4.19.157-perf",config:Map<String,String> = emptyMap()) = DeviceDetails(
        manufacturer="Xiaomi",brand="Redmi",model="Redmi 9T",device="lemon",product="lemon_ru",board="lemon",hardware="qcom",
        android="Android 12",sdk=31,build="V14.0.2.0.SJQRUXM",buildType="user",buildTags="release-keys",securityPatch="2023-04-01",
        kernel=kernel,architecture="aarch64",abis="arm64-v8a",fingerprint="Redmi/lemon/lemon:12/test/user/release-keys",
        bootloaderState="Заблокирован",verifiedBootState="GREEN",vbmetaState="locked",verityMode="enforcing",selinuxState="Enforcing",
        selinuxContext="u:r:untrusted_app:s0",capEff="0",noNewPrivs="0",seccomp="2",appDebuggable=false,runtimeMaxMemoryMb=256,
        rootPathsFound=emptyList(),kernelConfig=config
    )
    @Test fun kernel419MatchesBothIntroductionRanges(){
        val list=RootReadinessAssessment.evaluate(sample())
        assertEquals(AssessmentTone.POTENTIAL,list[0].tone)
        assertEquals(AssessmentTone.POTENTIAL,list[1].tone)
    }
    @Test fun hiddenConfigIsUnknownNotSafe(){
        val list=RootReadinessAssessment.evaluate(sample())
        assertTrue(list.all{it.tone!=AssessmentTone.LOW})
        assertTrue(list[1].conditions.contains("Не удалось"))
    }
    @Test fun disabledDirtyFragComponentsReduceApplicability(){
        val cfg=mapOf("CONFIG_XFRM_USER" to "n","CONFIG_INET_ESP" to "n","CONFIG_INET6_ESP" to "n","CONFIG_AF_RXRPC" to "n")
        assertEquals(AssessmentTone.LOW,RootReadinessAssessment.evaluate(sample(config=cfg))[1].tone)
    }
    @Test fun olderKernelIsOutsideDirtyFragIntroductionRange(){
        assertEquals(AssessmentTone.LOW,RootReadinessAssessment.evaluate(sample(kernel="3.10.108"))[1].tone)
    }
}
