package dev.limeroot.research

private data class KernelVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<KernelVersion> {
    override fun compareTo(other: KernelVersion): Int =
        compareValuesBy(this, other, KernelVersion::major, KernelVersion::minor, KernelVersion::patch)
    fun atLeast(major: Int, minor: Int, patch: Int = 0) =
        compareTo(KernelVersion(major, minor, patch)) >= 0
}

enum class AssessmentTone { POTENTIAL, UNKNOWN, LOW }

data class VulnerabilityAssessment(
    val cveId: String, val title: String, val versionAssessment: String,
    val conditions: String, val conclusion: String, val sourceUrl: String, val tone: AssessmentTone
)

object RootReadinessAssessment {
    fun evaluate(device: DeviceDetails): List<VulnerabilityAssessment> {
        val version = parseKernelVersion(device.kernel)
        val label = version?.let { "${it.major}.${it.minor}.${it.patch}" } ?: "версия не распознана"

        // GhostLock is in the futex priority-inheritance rtmutex path.
        val ghostMatch = version?.atLeast(2, 6, 39)
        val futexPi = configEnabled(device, "CONFIG_FUTEX_PI")
        val ghostConditions = when (futexPi) {
            true -> "CONFIG_FUTEX_PI включён. Это подтверждает наличие соответствующего кода, но не доказывает достижимость уязвимого сценария из Android-приложения."
            false -> "CONFIG_FUTEX_PI явно выключен в доступной конфигурации; основной затронутый путь не должен собираться."
            null -> "CONFIG_FUTEX_PI не удалось проверить: /proc/config.gz недоступен или конфигурация неполная."
        }
        val ghostVersion = when (ghostMatch) {
            true -> "Ядро $label старше версии появления проблемы (2.6.39). Для ветки 4.19 отдельный upstream fixed build в опубликованном списке не указан."
            false -> "Ядро $label ниже версии появления проблемы 2.6.39."
            null -> "Не удалось разобрать строку версии ядра: ${device.kernel}."
        }
        val ghostTone = when {
            ghostMatch == false || futexPi == false -> AssessmentTone.LOW
            ghostMatch == true -> AssessmentTone.POTENTIAL
            else -> AssessmentTone.UNKNOWN
        }
        val ghostConclusion = when {
            ghostMatch == false -> "По номеру версии ядра совпадения с опубликованным диапазоном нет."
            futexPi == false -> "Версия пересекается с диапазоном, но CONFIG_FUTEX_PI выключен в доступной конфигурации. Это снижает применимость."
            ghostMatch == true -> "Есть потенциальное совпадение. Xiaomi могла перенести исправление в прошивку без изменения строки 4.19.157; vendor-патч не подтверждён. Получение временного Root не доказано."
            else -> "Недостаточно данных для оценки."
        }

        // DirtyFrag ESP/XFRM variant: CVE-2026-43284.
        val espMatch = version?.atLeast(4, 11, 0)
        val xfrm = configEnabled(device, "CONFIG_XFRM_USER")
        val esp4 = configEnabled(device, "CONFIG_INET_ESP")
        val esp6 = configEnabled(device, "CONFIG_INET6_ESP")
        val userNs = configEnabled(device, "CONFIG_USER_NS")
        val nsMax = device.userNamespacesMax.toIntOrNull()
        val espBuilt = xfrm == true && (esp4 == true || esp6 == true)
        val espExplicitlyOff = xfrm == false && esp4 == false && esp6 == false
        val nsText = when {
            userNs == false || nsMax == 0 -> "Доступные признаки указывают, что user namespaces отключены (CONFIG_USER_NS=n или max_user_namespaces=0)."
            userNs == true && nsMax != null && nsMax > 0 -> "CONFIG_USER_NS включён и max_user_namespaces=$nsMax. Это ещё не доказывает, что untrusted app реально может создать нужный user/network namespace."
            else -> "Разрешено ли непривилегированному приложению создавать user/network namespaces, определить не удалось."
        }
        val espConditions = when {
            espBuilt -> "Конфигурация показывает XFRM_USER и ESP (IPv4/IPv6). Для известного ESP-пути также важна доступность непривилегированных user/network namespaces. $nsText"
            espExplicitlyOff -> "Доступная конфигурация показывает XFRM_USER и оба ESP-компонента выключенными."
            else -> "Не удалось подтвердить всю комбинацию CONFIG_XFRM_USER + CONFIG_INET_ESP/CONFIG_INET6_ESP. $nsText"
        }
        val espVersion = when (espMatch) {
            true -> "Ядро $label новее версии появления проблемы (4.11); по номеру версии оно пересекается с диапазоном CVE-2026-43284. Upstream fix для ветки 4.19 отдельно не указан."
            false -> "Ядро $label ниже точки появления проблемы 4.11; по upstream диапазону совпадения нет."
            null -> "Не удалось разобрать строку версии ядра: ${device.kernel}."
        }
        val espTone = when {
            espMatch == false || espExplicitlyOff -> AssessmentTone.LOW
            espMatch == true -> AssessmentTone.POTENTIAL
            else -> AssessmentTone.UNKNOWN
        }
        val espConclusion = when {
            espMatch == false -> "По версии ядра совпадения с CVE-2026-43284 нет."
            espExplicitlyOff -> "Версия пересекается с диапазоном, но необходимые ESP/XFRM-компоненты выключены в доступной конфигурации."
            espMatch == true -> "Потенциальное совпадение по версии. Нужно проверить vendor backport Xiaomi и возможность непривилегированного namespace; Root не подтверждён."
            else -> "Недостаточно данных для оценки."
        }

        // DirtyFrag RxRPC variant is a separate CVE, introduced in Linux 5.3.
        val rxrpcMatch = version?.atLeast(5, 3, 0)
        val rxrpc = configEnabled(device, "CONFIG_AF_RXRPC")
        val rxrpcVersion = when (rxrpcMatch) {
            true -> "Ядро $label не старше точки появления проблемы (5.3)."
            false -> "Ядро $label ниже точки появления проблемы 5.3; upstream диапазон CVE-2026-43500 начинается позже ветки 4.19."
            null -> "Не удалось разобрать строку версии ядра: ${device.kernel}."
        }
        val rxrpcConditions = when (rxrpc) {
            true -> "CONFIG_AF_RXRPC включён; это необходимая предпосылка, но фактическая достижимость уязвимого пути не подтверждена."
            false -> "CONFIG_AF_RXRPC явно выключен в доступной конфигурации."
            null -> "CONFIG_AF_RXRPC не удалось проверить: конфигурация ядра недоступна."
        }
        val rxrpcTone = when {
            rxrpcMatch == false || rxrpc == false -> AssessmentTone.LOW
            rxrpcMatch == true -> AssessmentTone.POTENTIAL
            else -> AssessmentTone.UNKNOWN
        }
        val rxrpcConclusion = when {
            rxrpcMatch == false -> "Ядро 4.19.157 предшествует появлению этого дефекта в 5.3; по upstream истории эта отдельная RxRPC-уязвимость не подходит."
            rxrpc == false -> "Версия может пересекаться с диапазоном, но CONFIG_AF_RXRPC выключен."
            rxrpcMatch == true -> "Есть потенциальное совпадение по версии и конфигурации. Требуется проверить vendor patch Xiaomi и runtime-доступность RxRPC."
            else -> "Недостаточно данных для оценки."
        }

        return listOf(
            VulnerabilityAssessment("CVE-2026-43499", "GhostLock · futex PI / rtmutex", ghostVersion, ghostConditions, ghostConclusion,
                "https://kernel.googlesource.com/pub/scm/linux/security/vulns/+/refs/heads/master/cve/published/2026/CVE-2026-43499.mbox", ghostTone),
            VulnerabilityAssessment("CVE-2026-43284", "DirtyFrag · ESP / XFRM", espVersion, espConditions, espConclusion,
                "https://kernel.googlesource.com/pub/scm/linux/security/vulns/+/refs/heads/master/cve/published/2026/CVE-2026-43284.mbox", espTone),
            VulnerabilityAssessment("CVE-2026-43500", "DirtyFrag · RxRPC", rxrpcVersion, rxrpcConditions, rxrpcConclusion,
                "https://kernel.googlesource.com/pub/scm/linux/security/vulns/+/refs/heads/master/cve/published/2026/CVE-2026-43500.mbox", rxrpcTone)
        )
    }

    private fun parseKernelVersion(raw: String): KernelVersion? {
        val match = Regex("""(?<!\\d)(\\d+)\\.(\\d+)\\.(\\d+)""").find(raw) ?: return null
        return runCatching { KernelVersion(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt()) }.getOrNull()
    }

    private fun configEnabled(device: DeviceDetails, key: String): Boolean? =
        when (device.kernelConfig[key]?.lowercase()) {
            "y", "m" -> true
            "n" -> false
            else -> null
        }
}
