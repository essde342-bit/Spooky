package dev.limeroot.research

import android.os.Build
import java.io.File
import java.util.zip.GZIPInputStream
import java.math.BigInteger
import java.util.concurrent.TimeUnit

object DeviceDiagnostics {
    private val propertyAllowlist = setOf(
        "ro.boot.flash.locked",
        "ro.boot.verifiedbootstate",
        "ro.boot.vbmeta.device_state",
        "ro.boot.veritymode",
        "ro.build.version.incremental"
    )

    fun collect(appDebuggable: Boolean): DiagnosticSnapshot {
        val generatedAt = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        // Only fixed, allow-listed getprop keys are queried. No shell is involved.
        val properties = propertyAllowlist.associateWith(::readProperty)
        val bootLocked = properties["ro.boot.flash.locked"].orEmpty()
        val vbState = properties["ro.boot.verifiedbootstate"].orEmpty()
        val vbmeta = properties["ro.boot.vbmeta.device_state"].orEmpty()
        val verity = properties["ro.boot.veritymode"].orEmpty()
        val selinuxEnforce = readSmallText("/sys/fs/selinux/enforce")
        val selinuxContext = readSmallText("/proc/self/attr/current").ifBlank { "Недоступен" }
        val procStatus = readProcStatus()
        val capEff = procStatus["CapEff"].orEmpty()
        val noNewPrivs = procStatus["NoNewPrivs"].orEmpty()
        val seccomp = procStatus["Seccomp"].orEmpty()

        val kernelConfig = readKernelConfig()
        val userNamespacesMax = readSmallText("/proc/sys/user/max_user_namespaces").ifBlank { "Не удалось прочитать" }

        val rootPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system_ext/bin/su",
            "/vendor/bin/su",
            "/debug_ramdisk/su",
            "/data/adb/magisk"
        ).filter { path -> runCatching { File(path).exists() }.getOrDefault(false) }

        val details = DeviceDetails(
            manufacturer = Build.MANUFACTURER.ifBlank { "Неизвестно" },
            brand = Build.BRAND.ifBlank { "Неизвестно" },
            model = Build.MODEL.ifBlank { "Неизвестно" },
            device = Build.DEVICE.ifBlank { "Неизвестно" },
            product = Build.PRODUCT.ifBlank { "Неизвестно" },
            board = Build.BOARD.ifBlank { "Неизвестно" },
            hardware = Build.HARDWARE.ifBlank { "Неизвестно" },
            android = "Android ${Build.VERSION.RELEASE ?: "неизвестно"}",
            sdk = Build.VERSION.SDK_INT,
            build = properties["ro.build.version.incremental"].orEmpty().ifBlank { Build.DISPLAY.orEmpty().ifBlank { "Неизвестно" } },
            buildType = Build.TYPE.orEmpty().ifBlank { "Неизвестно" },
            buildTags = Build.TAGS.orEmpty().ifBlank { "Неизвестно" },
            securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty().ifBlank { "Неизвестно" },
            kernel = System.getProperty("os.version").orEmpty().ifBlank { "Неизвестно" },
            architecture = System.getProperty("os.arch").orEmpty().ifBlank { "Неизвестно" },
            abis = Build.SUPPORTED_ABIS.joinToString().ifBlank { "Неизвестно" },
            fingerprint = Build.FINGERPRINT.orEmpty().ifBlank { "Неизвестно" },
            bootloaderState = when (bootLocked) {
                "1" -> "Заблокирован"
                "0" -> "Разблокирован"
                else -> "Не удалось определить"
            },
            verifiedBootState = vbState.ifBlank { "Неизвестно" }.uppercase(),
            vbmetaState = vbmeta.ifBlank { "Неизвестно" },
            verityMode = verity.ifBlank { "Неизвестно" },
            selinuxState = when (selinuxEnforce) {
                "1" -> "Enforcing (принудительный режим)"
                "0" -> "Permissive (непринудительный режим)"
                else -> "Не удалось прочитать"
            },
            selinuxContext = selinuxContext.take(180),
            capEff = capEff.ifBlank { "Не удалось прочитать" },
            noNewPrivs = noNewPrivs.ifBlank { "Не удалось прочитать" },
            seccomp = seccomp.ifBlank { "Не удалось прочитать" },
            appDebuggable = appDebuggable,
            runtimeMaxMemoryMb = Runtime.getRuntime().maxMemory() / (1024 * 1024),
            rootPathsFound = rootPaths,
            kernelConfig = kernelConfig,
            userNamespacesMax = userNamespacesMax
        )

        val checks = mutableListOf<DiagnosticCheck>()

        checks += when (vbState.lowercase()) {
            "green" -> check("Загрузка", "Verified Boot", "GREEN", "Система сообщает о зелёном состоянии Verified Boot. Это хороший признак целостности загрузочной цепочки, но не аудит всей прошивки.", CheckSeverity.GOOD)
            "yellow" -> check("Загрузка", "Verified Boot", "YELLOW", "Состояние жёлтое. Возможна пользовательская доверенная конфигурация загрузки.", CheckSeverity.WARNING)
            "orange" -> check("Загрузка", "Verified Boot", "ORANGE", "Загрузчик, вероятно, разблокирован или используется нештатное состояние загрузки.", CheckSeverity.WARNING)
            "red" -> check("Загрузка", "Verified Boot", "RED", "Система сообщает о проблеме проверки целостности загрузки.", CheckSeverity.CRITICAL)
            else -> check("Загрузка", "Verified Boot", "Неизвестно", "Производитель не предоставил доступное значение состояния.", CheckSeverity.INFO)
        }

        checks += when (bootLocked) {
            "1" -> check("Загрузка", "Состояние загрузчика", "Заблокирован", "Свойство ro.boot.flash.locked равно 1.", CheckSeverity.GOOD)
            "0" -> check("Загрузка", "Состояние загрузчика", "Разблокирован", "Свойство ro.boot.flash.locked равно 0. Это снижает гарантии цепочки доверенной загрузки.", CheckSeverity.WARNING)
            else -> check("Загрузка", "Состояние загрузчика", "Неизвестно", "Android не предоставил ожидаемое свойство загрузчика.", CheckSeverity.INFO)
        }

        checks += when (selinuxEnforce) {
            "1" -> check("Изоляция", "SELinux", "Enforcing", "SELinux работает в принудительном режиме.", CheckSeverity.GOOD)
            "0" -> check("Изоляция", "SELinux", "Permissive", "SELinux не блокирует нарушения политик. Это серьёзное ослабление защиты.", CheckSeverity.CRITICAL)
            else -> check("Изоляция", "SELinux", "Недоступно", "Не удалось прочитать /sys/fs/selinux/enforce. Это не означает, что SELinux отключён.", CheckSeverity.INFO)
        }

        checks += when {
            "test-keys" in details.buildTags -> check("Сборка ОС", "Подпись сборки", "test-keys", "Обнаружены тестовые метки сборки. Это часто встречается в пользовательских прошивках.", CheckSeverity.WARNING)
            "release-keys" in details.buildTags -> check("Сборка ОС", "Подпись сборки", "release-keys", "Система сообщает о release-keys. Сама метка не подтверждает подлинность прошивки.", CheckSeverity.GOOD)
            else -> check("Сборка ОС", "Подпись сборки", details.buildTags, "Метки сборки не позволяют сделать однозначный вывод.", CheckSeverity.INFO)
        }

        checks += if (details.buildType.equals("user", true)) {
            check("Сборка ОС", "Тип системной сборки", "user", "Обычный пользовательский тип Android-сборки.", CheckSeverity.GOOD)
        } else {
            check("Сборка ОС", "Тип системной сборки", details.buildType, "Тип отличается от обычной user-сборки. На инженерных сборках некоторые ограничения могут быть слабее.", CheckSeverity.WARNING)
        }

        checks += if (appDebuggable) {
            check("Приложение", "Режим отладки", "Включён", "Установленная сборка LimeRoot помечена как debuggable.", CheckSeverity.WARNING)
        } else {
            check("Приложение", "Режим отладки", "Выключен", "Установленная сборка LimeRoot не помечена как debuggable.", CheckSeverity.GOOD)
        }

        checks += when {
            capEff.isBlank() -> check("Изоляция", "Linux capabilities (CapEff)", "Недоступно", "Не удалось прочитать CapEff из /proc/self/status.", CheckSeverity.INFO)
            runCatching { BigInteger(capEff, 16) == BigInteger.ZERO }.getOrDefault(false) ->
                check("Изоляция", "Linux capabilities (CapEff)", "0", "У текущего процесса не обнаружено эффективных Linux capabilities.", CheckSeverity.GOOD)
            else -> check("Изоляция", "Linux capabilities (CapEff)", capEff, "У процесса есть эффективные capabilities. Это не доказывает наличие root, но требует контекста.", CheckSeverity.WARNING)
        }

        checks += when (noNewPrivs) {
            "1" -> check("Изоляция", "NoNewPrivs", "1", "Процесс не может получать новые привилегии через execve.", CheckSeverity.GOOD)
            "0" -> check("Изоляция", "NoNewPrivs", "0", "Флаг NoNewPrivs не установлен. Само по себе это нормально для многих Android-процессов.", CheckSeverity.INFO)
            else -> check("Изоляция", "NoNewPrivs", "Недоступно", "Поле не удалось прочитать из /proc/self/status.", CheckSeverity.INFO)
        }

        checks += when (seccomp) {
            "2" -> check("Изоляция", "Seccomp", "Filter", "Для процесса включён фильтрующий режим seccomp.", CheckSeverity.GOOD)
            "1" -> check("Изоляция", "Seccomp", "Strict", "Для процесса включён строгий режим seccomp.", CheckSeverity.GOOD)
            "0" -> check("Изоляция", "Seccomp", "Не включён", "В /proc/self/status указано Seccomp=0.", CheckSeverity.WARNING)
            else -> check("Изоляция", "Seccomp", "Недоступно", "Поле не удалось прочитать из /proc/self/status.", CheckSeverity.INFO)
        }

        checks += if (rootPaths.isEmpty()) {
            check("Root-признаки", "Известные пути su / Magisk", "Не обнаружены", "Проверены только распространённые пути. Их отсутствие НЕ доказывает отсутствие root.", CheckSeverity.INFO)
        } else {
            check("Root-признаки", "Известные пути su / Magisk", "Найдены: ${rootPaths.joinToString()}", "Обнаружен путь, связанный с su или Magisk. Наличие файла само по себе не подтверждает активный root-доступ.", CheckSeverity.WARNING)
        }

        listOf(
            "/proc/kallsyms" to "Символы ядра",
            "/proc/config.gz" to "Конфигурация ядра",
            "/proc/modules" to "Список модулей ядра",
            "/proc/net/xfrm_stat" to "Счётчики XFRM"
        ).forEach { (path, label) ->
            val accessible = canOpenForRead(path)
            checks += check(
                "Доступность ядра",
                label,
                if (accessible) "Доступен для чтения" else "Ограничен",
                if (accessible) "$path открывается текущему приложению для чтения." else "$path не удалось открыть. Ограничение доступа Android является обычным и не указывает само по себе на уязвимость.",
                CheckSeverity.INFO
            )
        }

        checks += if (vbmeta.equals("locked", true)) {
            check("Загрузка", "Состояние VBMeta", "locked", "Значение ro.boot.vbmeta.device_state сообщает locked.", CheckSeverity.GOOD)
        } else if (vbmeta.equals("unlocked", true)) {
            check("Загрузка", "Состояние VBMeta", "unlocked", "VBMeta сообщает unlocked.", CheckSeverity.WARNING)
        } else {
            check("Загрузка", "Состояние VBMeta", vbmeta.ifBlank { "Неизвестно" }, "Дополнительное свойство недоступно или не распознано.", CheckSeverity.INFO)
        }

        val notes = listOf(
            "Проверки выполняются без root и не изменяют системные разделы.",
            "Ограниченный доступ к файлам /proc и /sys является нормальной частью Android sandbox.",
            "Проверка su/Magisk охватывает только известные пути и не анализирует скрытые или системно встроенные методы root.",
            "Build fingerprint и модель устройства помогают определить точную прошивку; перед отправкой отчёта убедись, что тебе комфортно делиться этими данными.",
            "Отчёт не является доказательством отсутствия уязвимостей и не заменяет независимый аудит."
        )

        return DiagnosticSnapshot(generatedAt, details, checks, notes)
    }

    private fun check(
        category: String,
        title: String,
        value: String,
        detail: String,
        severity: CheckSeverity
    ) = DiagnosticCheck(category, title, value, detail, severity)

    private fun readProperty(key: String): String {
        if (key !in propertyAllowlist) return ""
        return try {
            val process = ProcessBuilder("/system/bin/getprop", key)
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(1500, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                return ""
            }
            process.inputStream.bufferedReader().use { it.readText().take(256).trim() }
        } catch (_: Exception) {
            ""
        }
    }

    private fun readSmallText(path: String): String = try {
        File(path).inputStream().bufferedReader().use { it.readLine().orEmpty().take(512).trim() }
    } catch (_: Exception) {
        ""
    }

    private fun readProcStatus(): Map<String, String> {
        val wanted = setOf("CapEff", "NoNewPrivs", "Seccomp")
        return try {
            File("/proc/self/status").bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val separator = line.indexOf(':')
                    if (separator <= 0) null else {
                        val key = line.substring(0, separator)
                        if (key in wanted) key to line.substring(separator + 1).trim() else null
                    }
                }.toMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private val relevantKernelConfig = setOf("CONFIG_FUTEX", "CONFIG_USER_NS", "CONFIG_XFRM_USER", "CONFIG_INET_ESP", "CONFIG_INET6_ESP", "CONFIG_AF_RXRPC")

    private fun readKernelConfig(): Map<String, String> {
        return try {
            GZIPInputStream(File("/proc/config.gz").inputStream()).bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val pair = when {
                        line.startsWith("CONFIG_") && '=' in line -> {
                            val separator = line.indexOf('=')
                            line.substring(0, separator) to line.substring(separator + 1).trim()
                        }
                        line.startsWith("# CONFIG_") && line.endsWith(" is not set") ->
                            line.removePrefix("# ").removeSuffix(" is not set") to "n"
                        else -> null
                    }
                    pair?.takeIf { it.first in relevantKernelConfig }
                }.toMap()
            }
        } catch (_: Exception) { emptyMap() }
    }

    private fun canOpenForRead(path: String): Boolean = try {
        File(path).inputStream().use { it.read(ByteArray(64)) >= 0 }
    } catch (_: Exception) {
        false
    }
}
