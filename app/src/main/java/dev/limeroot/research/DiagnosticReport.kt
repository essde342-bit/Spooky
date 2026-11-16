package dev.limeroot.research

object DiagnosticReport {
    fun format(snapshot: DiagnosticSnapshot): String = buildString {
        val device = snapshot.device
        val good = snapshot.checks.count { it.severity == CheckSeverity.GOOD }
        val info = snapshot.checks.count { it.severity == CheckSeverity.INFO }
        val warning = snapshot.checks.count { it.severity == CheckSeverity.WARNING }
        val critical = snapshot.checks.count { it.severity == CheckSeverity.CRITICAL }

        appendLine("LIMEROOT — ПОЛНЫЙ ОТЧЁТ ДИАГНОСТИКИ")
        appendLine("Версия приложения: 0.4.0")
        appendLine("Дата и время: ${snapshot.generatedAt}")
        appendLine("Формат: обычный текст UTF-8")
        appendLine()
        appendLine("══════════════════ ИТОГ ══════════════════")
        appendLine("Всего проверок: ${snapshot.checks.size}")
        appendLine("Норма: $good")
        appendLine("Информация: $info")
        appendLine("Предупреждения: $warning")
        appendLine("Критические признаки: $critical")
        appendLine()
        appendLine("ВАЖНО: это диагностические наблюдения, а не заключение о полной безопасности.")
        appendLine("Отсутствие найденных признаков не доказывает отсутствие root или уязвимостей.")
        appendLine()

        appendLine("══════════════════ УСТРОЙСТВО ══════════════════")
        appendLine("Производитель: ${device.manufacturer}")
        appendLine("Бренд: ${device.brand}")
        appendLine("Модель: ${device.model}")
        appendLine("Код устройства: ${device.device}")
        appendLine("Продукт: ${device.product}")
        appendLine("Плата: ${device.board}")
        appendLine("Аппаратная платформа: ${device.hardware}")
        appendLine("Android: ${device.android} (API ${device.sdk})")
        appendLine("Номер сборки: ${device.build}")
        appendLine("Тип сборки: ${device.buildType}")
        appendLine("Метки сборки: ${device.buildTags}")
        appendLine("Патч безопасности Android: ${device.securityPatch}")
        appendLine("Ядро Linux: ${device.kernel}")
        appendLine("Архитектура процесса: ${device.architecture}")
        appendLine("Поддерживаемые ABI: ${device.abis}")
        appendLine("Build fingerprint: ${device.fingerprint}")
        appendLine("Память процесса (максимум JVM): ${device.runtimeMaxMemoryMb} МБ")
        appendLine()

        appendLine("══════════════════ ЗАГРУЗКА И ЗАЩИТА ══════════════════")
        appendLine("Состояние загрузчика: ${device.bootloaderState}")
        appendLine("Verified Boot: ${device.verifiedBootState}")
        appendLine("VBMeta: ${device.vbmetaState}")
        appendLine("dm-verity: ${device.verityMode}")
        appendLine("SELinux: ${device.selinuxState}")
        appendLine("Контекст процесса: ${device.selinuxContext}")
        appendLine("CapEff текущего процесса: ${device.capEff}")
        appendLine("NoNewPrivs: ${device.noNewPrivs}")
        appendLine("Seccomp: ${device.seccomp}")
        appendLine("Собственная сборка приложения debuggable: ${device.appDebuggable}")
        appendLine("Найденные стандартные root-пути: ${device.rootPathsFound.joinToString().ifBlank { "не обнаружены" }}")
        appendLine()

        appendLine("══════════════════ РЕЗУЛЬТАТЫ ПРОВЕРОК ══════════════════")
        snapshot.checks.forEachIndexed { index, check ->
            appendLine("${index + 1}. [${severityLabel(check.severity)}] ${check.title}")
            appendLine("   Раздел: ${check.category}")
            appendLine("   Результат: ${check.value}")
            appendLine("   Пояснение: ${check.detail}")
            appendLine()
        }

        appendLine("══════════════════ ПРИМЕЧАНИЯ ══════════════════")
        snapshot.notes.forEachIndexed { index, note -> appendLine("${index + 1}. $note") }
        appendLine()
        appendLine("══════════════════ КОНФИДЕНЦИАЛЬНОСТЬ ══════════════════")
        appendLine("В отчёте есть модель, названия системных разделов и Build fingerprint.")
        appendLine("В отчёт не включаются IMEI, серийный номер, номер телефона, аккаунты,")
        appendLine("местоположение, список установленных приложений, личные файлы, пароли или ключи.")
        appendLine("Файл создаётся только после ручного выбора места сохранения пользователем.")
        appendLine()
        appendLine("Конец отчёта LimeRoot 0.4.0")
    }

    private fun severityLabel(severity: CheckSeverity): String = when (severity) {
        CheckSeverity.GOOD -> "НОРМА"
        CheckSeverity.INFO -> "ИНФОРМАЦИЯ"
        CheckSeverity.WARNING -> "ПРЕДУПРЕЖДЕНИЕ"
        CheckSeverity.CRITICAL -> "КРИТИЧЕСКИЙ ПРИЗНАК"
    }
}
