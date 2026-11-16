package dev.limeroot.research

import android.content.Intent
import android.graphics.Color as AColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF0A0F0C)
private val CardBg = Color(0xFF121A15)
private val Green = Color(0xFFB7F36B)
private val GreenDim = Color(0xFF83B84B)
private val Muted = Color(0xFF8E9A91)
private val Stroke = Color(0xFF26332A)
private val Amber = Color(0xFFFFC36A)
private val Red = Color(0xFFFF7777)

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
    var snapshot by remember { mutableStateOf<DiagnosticSnapshot?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var pendingReport by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val report = pendingReport
        if (uri != null && report != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val output = context.contentResolver.openOutputStream(uri, "wt")
                            ?: error("Не удалось открыть выбранный файл")
                        output.use { it.write(report.toByteArray(Charsets.UTF_8)) }
                    }
                }
                Toast.makeText(
                    context,
                    if (result.isSuccess) "TXT-отчёт сохранён" else "Ошибка сохранения: ${result.exceptionOrNull()?.message ?: "неизвестная ошибка"}",
                    Toast.LENGTH_LONG
                ).show()
                pendingReport = null
            }
        } else {
            pendingReport = null
        }
    }

    val startScan: () -> Unit = {
        if (!scanning) {
            scope.launch {
                scanning = true
                scanError = null
                try {
                    val debuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    snapshot = withContext(Dispatchers.IO) {
                        DeviceDiagnostics.collect(debuggable)
                    }
                } catch (error: Exception) {
                    scanError = "Не удалось завершить диагностику: ${error.message ?: error.javaClass.simpleName}"
                } finally {
                    scanning = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { startScan() }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Green,
        onPrimary = Bg,
        secondary = GreenDim,
        background = Bg,
        surface = CardBg,
        onSurface = Color(0xFFEAF1E8),
        surfaceVariant = Color(0xFF19231C),
        onSurfaceVariant = Muted
    )) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            Header()
            Box(Modifier.weight(1f)) {
                when {
                    snapshot == null && scanning -> LoadingPage()
                    snapshot == null -> ErrorPage(scanError ?: "Диагностика ещё не запускалась", startScan)
                    tab == 0 -> OverviewPage(snapshot!!, scanning, startScan, onExport = { showExportDialog = true })
                    tab == 1 -> DevicePage(snapshot!!.device)
                    else -> ChecksPage(snapshot!!, scanning, startScan, onExport = { showExportDialog = true })
                }
            }
            NavigationBar(
                containerColor = Color(0xFF0E1510),
                windowInsets = WindowInsets.navigationBars
            ) {
                listOf(
                    Triple("Обзор", Icons.Rounded.Home, 0),
                    Triple("Устройство", Icons.Rounded.PhoneAndroid, 1),
                    Triple("Диагностика", Icons.Rounded.BugReport, 2)
                ).forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Bg,
                            selectedTextColor = Green,
                            indicatorColor = Green,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    }

    if (showExportDialog && snapshot != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = CardBg,
            title = { Text("Состав TXT-отчёта", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "В файл попадут модель и код устройства, версия Android, Build fingerprint, сведения о ядре, состоянии загрузки, SELinux, sandbox и результаты проверок.\n\n" +
                        "Не включаются IMEI, серийный номер, номер телефона, аккаунты, местоположение, список приложений и личные файлы.\n\n" +
                        "Файл сохранится только после того, как ты сам выберешь место.",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    pendingReport = DiagnosticReport.format(snapshot!!)
                    val stamp = snapshot!!.generatedAt.filter { it.isLetterOrDigit() }.take(14)
                    exportLauncher.launch("LimeRoot_Report_${stamp.ifBlank { "latest" }}.txt")
                }) { Text("Выбрать место", color = Green) }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Отмена", color = Muted)
                }
            }
        )
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
        ) {
            Image(painter = painterResource(R.drawable.limeroot_icon), contentDescription = "LimeRoot", modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("LimeRoot", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            Text("ANDROID DEVICE LAB", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        }
        Pill("RESEARCH 0.4", Green)
    }
}

@Composable
private fun LoadingPage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Green)
            Spacer(Modifier.height(14.dp))
            Text("Полная диагностика", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text("Проверяем систему в фоновом потоке", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ErrorPage(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.ErrorOutline, null, tint = Amber, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(14.dp))
            Text("Не удалось получить отчёт", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) {
                Text("Повторить проверку")
            }
        }
    }
}

@Composable
private fun OverviewPage(
    snapshot: DiagnosticSnapshot,
    scanning: Boolean,
    onScan: () -> Unit,
    onExport: () -> Unit
) {
    val device = snapshot.device
    val good = snapshot.checks.count { it.severity == CheckSeverity.GOOD }
    val warnings = snapshot.checks.count { it.severity == CheckSeverity.WARNING || it.severity == CheckSeverity.CRITICAL }
    val info = snapshot.checks.count { it.severity == CheckSeverity.INFO }

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
                    Pill(device.device.uppercase().take(12), Green)
                }
                Spacer(Modifier.height(12.dp))
                Text(device.model, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp)
                Text("${device.hardware} · ${device.architecture}", color = Color(0xFFB3C3B5), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(18.dp))
                Text("${device.android} · патч ${device.securityPatch}", color = Color(0xFFDCE8D9), fontSize = 11.sp)
            }
        }

        Section("Итоги диагностики", "Завершено: ${snapshot.generatedAt}")
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SummaryMetric(Modifier.weight(1f), good.toString(), "Норма", Green)
            SummaryMetric(Modifier.weight(1f), info.toString(), "Инфо", Muted)
            SummaryMetric(Modifier.weight(1f), warnings.toString(), "Внимание", if (warnings > 0) Amber else Green)
        }

        val topChecks = snapshot.checks.filter {
            it.severity == CheckSeverity.CRITICAL || it.severity == CheckSeverity.WARNING
        }
        if (topChecks.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Stroke),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Green, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Критических предупреждений не обнаружено среди выполненных проверок.", color = Color.White, fontSize = 12.sp)
                }
            }
        } else {
            Section("Требует внимания", "Найденные предупреждения")
            topChecks.forEach { CheckCard(it) }
        }

        Section("Состояние устройства", "Ключевые показатели")
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Stroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                InfoRow("Verified Boot", device.verifiedBootState)
                InfoRow("Загрузчик", device.bootloaderState)
                InfoRow("SELinux", device.selinuxState)
                InfoRow("Тип сборки", device.buildType)
                InfoRow("Seccomp", device.seccomp)
            }
        }

        Section("Полный отчёт", "Сохрани TXT и отправь его в чат для разбора")
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF172319)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF334B30)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Description, null, tint = Green, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("LimeRoot_Report.txt", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Text("Отчёт содержит результаты всех проверок и пояснения простым языком. Перед сохранением покажем, какие данные попадут в файл.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)
                ) {
                    Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Скачать TXT-отчёт", fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = onScan,
            enabled = !scanning,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF253327), contentColor = Green)
        ) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (scanning) "Выполняется диагностика…" else "Запустить диагностику повторно")
        }
        Text("LimeRoot не получает root и не изменяет системные разделы. Отсутствие предупреждений не означает 100% безопасность.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(bottom = 18.dp))
    }
}

@Composable
private fun DevicePage(device: DeviceDetails) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Section("Устройство", "Сведения из Android API и системных свойств")
        Group("МОДЕЛЬ И ПЛАТФОРМА") {
            Detail("Производитель", device.manufacturer)
            Detail("Бренд", device.brand)
            Detail("Модель", device.model)
            Detail("Код устройства", device.device)
            Detail("Продукт", device.product)
            Detail("Плата", device.board)
            Detail("Аппаратная платформа", device.hardware)
        }
        Group("ANDROID И СБОРКА") {
            Detail("Версия", "${device.android} · API ${device.sdk}")
            Detail("Номер сборки", device.build)
            Detail("Тип сборки", device.buildType)
            Detail("Метки сборки", device.buildTags)
            Detail("Патч безопасности", device.securityPatch)
            Detail("Build fingerprint", device.fingerprint)
        }
        Group("ЯДРО И АРХИТЕКТУРА") {
            Detail("Версия ядра", device.kernel)
            Detail("Архитектура процесса", device.architecture)
            Detail("Поддерживаемые ABI", device.abis)
            Detail("Максимальная память JVM", "${device.runtimeMaxMemoryMb} МБ")
        }
        Group("СОСТОЯНИЕ ЗАГРУЗКИ") {
            Detail("Загрузчик", device.bootloaderState)
            Detail("Verified Boot", device.verifiedBootState)
            Detail("VBMeta", device.vbmetaState)
            Detail("dm-verity", device.verityMode)
        }
        Group("ИЗОЛЯЦИЯ ПРОЦЕССА") {
            Detail("SELinux", device.selinuxState)
            Detail("Контекст", device.selinuxContext)
            Detail("CapEff", device.capEff)
            Detail("NoNewPrivs", device.noNewPrivs)
            Detail("Seccomp", device.seccomp)
        }
        Text("Часть свойств может быть скрыта производителем или ограничена политиками Android.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 18.dp))
    }
}

@Composable
private fun ChecksPage(
    snapshot: DiagnosticSnapshot,
    scanning: Boolean,
    onScan: () -> Unit,
    onExport: () -> Unit
) {
    val groups = snapshot.checks.groupBy { it.category }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Section("Диагностика", "${snapshot.checks.size} проверок · ${snapshot.generatedAt}")
        Surface(
            color = Color(0xFF1F2B1D),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFF384D2B))
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Info, null, tint = Green, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Text("Проверки только читают доступные сведения. Ограниченный доступ к системным файлам не равен обнаруженной уязвимости.", color = Color(0xFFD6E5D0), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        groups.forEach { (category, checks) ->
            Group(category.uppercase()) {
                checks.forEach { CheckRow(it) }
            }
        }
        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)
        ) {
            Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Сохранить все результаты в TXT", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onScan,
            enabled = !scanning,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            border = BorderStroke(1.dp, Stroke),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Green)
        ) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (scanning) "Проверяем…" else "Повторить диагностику")
        }
        Text("Нет root-эксплойтов, прошивки, записи в разделы или сетевых запросов.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 18.dp))
    }
}

@Composable
private fun SummaryMetric(modifier: Modifier, value: String, label: String, color: Color) {
    Card(
        modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Stroke)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CheckCard(check: DiagnosticCheck) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Stroke)
    ) {
        CheckRow(check)
    }
}

@Composable
private fun CheckRow(check: DiagnosticCheck) {
    val color = severityColor(check.severity)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = when (check.severity) {
                CheckSeverity.GOOD -> Icons.Rounded.CheckCircle
                CheckSeverity.INFO -> Icons.Rounded.Info
                CheckSeverity.WARNING -> Icons.Rounded.WarningAmber
                CheckSeverity.CRITICAL -> Icons.Rounded.Error
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(19.dp).padding(top = 1.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(check.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(check.value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            Text(check.detail, color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private fun severityColor(severity: CheckSeverity): Color = when (severity) {
    CheckSeverity.GOOD -> Green
    CheckSeverity.INFO -> Muted
    CheckSeverity.WARNING -> Amber
    CheckSeverity.CRITICAL -> Red
}

@Composable
private fun Metric(modifier: Modifier, icon: ImageVector, label: String, value: String, hint: String) {
    Card(
        modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Stroke)
    ) {
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
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Stroke)
    ) {
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
private fun Section(title: String, subtitle: String) {
    Column {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.4).sp)
        Text(subtitle, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun Pill(label: String, color: Color) {
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(30.dp), border = BorderStroke(1.dp, color.copy(alpha = .34f))) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
