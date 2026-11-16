package dev.limeroot.research

import android.content.Intent
import android.graphics.Color as AColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Bg = Color(0xFF0A0F0C)
private val CardBg = Color(0xFF121A15)
private val Green = Color(0xFFB7F36B)
private val GreenDim = Color(0xFF83B84B)
private val Muted = Color(0xFF8E9A91)
private val Stroke = Color(0xFF26332A)
private val Amber = Color(0xFFFFC36A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AColor.rgb(10, 15, 12)
        window.navigationBarColor = AColor.rgb(10, 15, 12)
        window.decorView.systemUiVisibility = 0
        setContent { LimeRootApp() }
    }
}

@Composable
private fun LimeRootApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var info by remember { mutableStateOf<DeviceInfo?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        info = runCatching {
            withContext(Dispatchers.IO) { DeviceInfo.read() }
        }.getOrNull()
    }

    val currentInfo = info

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Green, onPrimary = Bg, secondary = GreenDim, background = Bg,
        surface = CardBg, onSurface = Color(0xFFEAF1E8),
        surfaceVariant = Color(0xFF19231C), onSurfaceVariant = Muted
    )) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            Header()
            Box(Modifier.weight(1f)) {
                when {
                    currentInfo == null -> LoadingPage()
                    tab == 0 -> Overview(currentInfo, busy, onRefresh = {
                        scope.launch {
                            busy = true
                            try {
                                info = withContext(Dispatchers.IO) { DeviceInfo.read() }
                            } catch (_: Exception) {
                                // Keep the last successful snapshot if refresh fails.
                            } finally {
                                busy = false
                            }
                        }
                    }, onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "LimeRoot report")
                            putExtra(Intent.EXTRA_TEXT, currentInfo.report())
                        }
                        context.startActivity(Intent.createChooser(intent, "Поделиться отчётом"))
                    })
                    tab == 1 -> DevicePage(currentInfo)
                    else -> ChecksPage(currentInfo)
                }
            }
            NavigationBar(containerColor = Color(0xFF0E1510), windowInsets = WindowInsets.navigationBars) {
                listOf(
                    Triple("Обзор", Icons.Rounded.Home, 0),
                    Triple("Устройство", Icons.Rounded.PhoneAndroid, 1),
                    Triple("Проверки", Icons.Rounded.BugReport, 2)
                ).forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = tab == index, onClick = { tab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Bg, selectedTextColor = Green,
                            indicatorColor = Green, unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingPage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Green)
            Spacer(Modifier.height(14.dp))
            Text("Читаем данные устройства", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text("Проверка выполняется в фоне", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Green, Color(0xFF6FAD3D)))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Security, null, tint = Bg, modifier = Modifier.size(25.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("LimeRoot", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Text("ANDROID DEVICE LAB", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        }
        Pill("RESEARCH 0.2", Green)
    }
}

@Composable
private fun Overview(info: DeviceInfo, busy: Boolean, onRefresh: () -> Unit, onShare: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF263B24), Color(0xFF15251A), Color(0xFF111B15))))
                .padding(20.dp)
        ) {
            Box(Modifier.size(145.dp).align(Alignment.CenterEnd).clip(CircleShape).background(Color.White.copy(alpha = .035f)))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ТВОЁ УСТРОЙСТВО", color = Color(0xFFB6CBAB), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.width(8.dp))
                    Pill("LEMON", Green)
                }
                Spacer(Modifier.height(12.dp))
                Text(info.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp)
                Text("Snapdragon 662 · SM6115", color = Color(0xFFB3C3B5), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(22.dp))
                Surface(color = Color(0xFF0D1A10).copy(alpha = .75f), shape = RoundedCornerShape(30.dp)) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Green))
                        Spacer(Modifier.width(8.dp))
                        Text("УСТРОЙСТВО ОПРЕДЕЛЕНО", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
                    }
                }
            }
        }

        Section("Состояние системы", "Текущая конфигурация")
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Stroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, tint = Green, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Защита Android", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Проверка состояния загрузки", color = Muted, fontSize = 11.sp)
                    }
                    Pill(if (info.verifiedGreen) "GREEN" else "UNKNOWN", if (info.verifiedGreen) Green else Amber)
                }
                HorizontalDivider(color = Stroke)
                Status("Verified Boot", info.verified, info.verifiedGreen)
                Status("Загрузчик", info.bootloader, info.bootLocked)
                Status("Стандартные su-пути", info.rootHint, !info.rootFound)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(Modifier.weight(1f), Icons.Rounded.Memory, "ЯДРО", info.kernelShort, "Linux / ARM64")
            Metric(Modifier.weight(1f), Icons.Rounded.Lock, "ЗАГРУЗЧИК", if (info.bootLocked) "Заблокирован" else info.bootloader, "Состояние защиты")
        }

        Section("Диагностика", "Без изменения системы")
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Stroke), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow("Версия Android", info.android)
                InfoRow("Патч безопасности", info.patch)
                InfoRow("Доступ к kallsyms", info.kallsymsText)
                InfoRow("Конфигурация ядра", info.configText)
            }
        }

        Button(
            onClick = onRefresh, enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)
        ) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text(if (busy) "Проверяем устройство…" else "Обновить диагностику", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onShare, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Rounded.Share, null, tint = Muted, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text("Поделиться отчётом", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DevicePage(info: DeviceInfo) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Section("Устройство", "Идентификаторы и версии")
        Group("МОДЕЛЬ УСТРОЙСТВА") {
            Detail("Название", info.name); Detail("Модель", info.model)
            Detail("Кодовое имя", info.codename); Detail("Плата", info.board); Detail("Платформа", info.platform)
        }
        Group("ПРОГРАММНОЕ ОБЕСПЕЧЕНИЕ") {
            Detail("Android", info.android); Detail("MIUI / Build", info.build)
            Detail("Security patch", info.patch); Detail("Fingerprint", info.fingerprint)
        }
        Group("ЯДРО") {
            Detail("Версия", info.kernel); Detail("Архитектура", info.arch); Detail("ABI", info.abi)
        }
        Text("Информация читается из публичных Android API и системных свойств. Некоторые параметры могут быть скрыты производителем.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 18.dp))
    }
}

@Composable
private fun ChecksPage(info: DeviceInfo) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Section("Проверки", "Только чтение · без модификаций")
        Surface(color = Color(0xFF1F2B1D), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFF384D2B))) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Info, null, tint = Green, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Text("LimeRoot только собирает диагностические сведения. Приложение не запрашивает root и не выполняет команды прошивки.", color = Color(0xFFD6E5D0), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        Group("ЗАГРУЗКА И ЗАЩИТА") {
            Probe("Verified Boot", info.verified, info.verifiedGreen)
            Probe("Bootloader", info.bootloader, info.bootLocked)
            Probe("Стандартные su-пути", info.rootHint, !info.rootFound)
        }
        Group("ДОСТУПНОСТЬ ЯДРА") {
            Probe("/proc/kallsyms", info.kallsymsText, info.kallsyms)
            Probe("/proc/config.gz", info.configText, info.config)
            Probe("/proc/modules", info.modulesText, info.modules)
            Probe("/proc/net/xfrm_stat", info.xfrmText, info.xfrm)
        }
        Group("КАК ЧИТАТЬ РЕЗУЛЬТАТЫ") {
            Text("«Ограничено» означает, что Android запретил чтение файла этому приложению. Это не подтверждает и не исключает уязвимость. Проверка su охватывает только стандартные пути и не доказывает отсутствие root.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(14.dp))
        }
        Text("LimeRoot Research · v0.2.0", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 18.dp))
    }
}

@Composable
private fun Metric(modifier: Modifier, icon: ImageVector, label: String, value: String, hint: String) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, Stroke)) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(13.dp))
            Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            Text(hint, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun Status(label: String, value: String, good: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (good) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber, null, tint = if (good) Green else Amber, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color(0xFFEAF1E8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Stroke)) {
        Column {
            Text(title, color = GreenDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, modifier = Modifier.padding(start = 15.dp, top = 15.dp, end = 15.dp, bottom = 5.dp))
            content()
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 8.dp)) {
        Text(label, color = Muted, fontSize = 10.sp)
        Text(value.ifBlank { "—" }, color = Color(0xFFEAF1E8), fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun Probe(label: String, value: String, good: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (good) Green else Amber))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color(0xFFE5ECE4), fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (good) Green else Amber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Section(title: String, subtitle: String) {
    Column {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp)
        Text(subtitle, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun Pill(label: String, color: Color) {
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, color.copy(alpha = .34f))) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

private data class DeviceInfo(
    val model: String, val codename: String, val board: String, val platform: String,
    val name: String, val android: String, val build: String, val patch: String,
    val kernel: String, val kernelShort: String, val arch: String, val abi: String,
    val fingerprint: String, val verified: String, val verifiedGreen: Boolean,
    val bootLocked: Boolean, val bootloader: String, val rootFound: Boolean, val rootHint: String,
    val kallsyms: Boolean, val kallsymsText: String, val config: Boolean, val configText: String,
    val modules: Boolean, val modulesText: String, val xfrm: Boolean, val xfrmText: String
) {
    fun report() = """
        LimeRoot Research 0.2.0
        Device: $name ($model)
        Codename: $codename
        Board: $board
        Platform: $platform
        Android: $android
        Build: $build
        Security patch: $patch
        Kernel: $kernel
        ABI: $abi
        Verified Boot: $verified
        Bootloader: $bootloader
        Standard su paths: $rootHint
        /proc/kallsyms: $kallsymsText
        /proc/config.gz: $configText
        /proc/modules: $modulesText
        /proc/net/xfrm_stat: $xfrmText

        Diagnostic-only build. No root exploit is included.
    """.trimIndent()

    companion object {
        // Run the fixed Android getprop utility once instead of spawning a
        // separate process for every property. This is read-only and never uses a shell.
        private fun readProperties(): Map<String, String> = try {
            val process = ProcessBuilder("/system/bin/getprop")
                .redirectErrorStream(true)
                .start()
            try {
                val lines = process.inputStream.bufferedReader().use { it.readLines() }
                if (!process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    emptyMap()
                } else {
                    lines.mapNotNull { line ->
                        val separator = line.indexOf("]: [")
                        if (line.startsWith("[") && separator > 1 && line.endsWith("]")) {
                            line.substring(1, separator) to line.substring(separator + 4, line.length - 1)
                        } else {
                            null
                        }
                    }.toMap()
                }
            } finally {
                if (process.isAlive) process.destroyForcibly()
            }
        } catch (_: Exception) {
            emptyMap()
        }

        private fun prop(properties: Map<String, String>, key: String): String =
            properties[key].orEmpty().ifBlank { "—" }

        private fun readable(path: String): Boolean = try {
            val f = File(path)
            f.exists() && f.canRead()
        } catch (_: Exception) { false }

        fun read(): DeviceInfo {
            val properties = readProperties()
            val code = Build.DEVICE.ifBlank { prop(properties, "ro.product.device") }
            val bootProp = prop(properties, "ro.boot.flash.locked")
            val boot = bootProp == "1"
            val verified = prop(properties, "ro.boot.verifiedbootstate")
            val kernel = System.getProperty("os.version") ?: "—"
            val su = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system_ext/bin/su", "/vendor/bin/su").any { File(it).exists() }
            fun pathText(path: String, yes: String, no: String) = if (readable(path)) yes else no
            return DeviceInfo(
                model = Build.MODEL.ifBlank { "Unknown" }, codename = code, board = prop(properties, "ro.product.board"),
                platform = prop(properties, "ro.board.platform").let { if (it == "—") "Не указана производителем" else it },
                name = if (code.equals("lemon", true)) "Redmi 9T" else Build.MODEL,
                android = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                build = prop(properties, "ro.build.version.incremental"), patch = Build.VERSION.SECURITY_PATCH ?: "—",
                kernel = kernel, kernelShort = kernel.substringBefore("-"),
                arch = System.getProperty("os.arch") ?: "—", abi = Build.SUPPORTED_ABIS.joinToString(),
                fingerprint = Build.FINGERPRINT,
                verified = if (verified == "green") "GREEN" else verified.uppercase().ifBlank { "UNKNOWN" },
                verifiedGreen = verified == "green", bootLocked = boot,
                bootloader = when (bootProp) { "1" -> "Заблокирован"; "0" -> "Разблокирован"; else -> "Неизвестно" },
                rootFound = su, rootHint = if (su) "Найдены стандартные пути" else "Не найдены стандартные пути",
                kallsyms = readable("/proc/kallsyms"), kallsymsText = pathText("/proc/kallsyms", "Доступен", "Ограничен"),
                config = readable("/proc/config.gz"), configText = pathText("/proc/config.gz", "Доступна", "Ограничена"),
                modules = readable("/proc/modules"), modulesText = pathText("/proc/modules", "Доступны", "Ограничены"),
                xfrm = readable("/proc/net/xfrm_stat"), xfrmText = pathText("/proc/net/xfrm_stat", "Доступна", "Ограничена")
            )
        }
    }
}
