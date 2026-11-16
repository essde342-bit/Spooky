package dev.limeroot.research

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

class MainActivity : Activity() {
    private fun readProp(name: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", name))
            process.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) { "Недоступно" }
    }

    private fun access(path: String): String {
        val file = File(path)
        return when {
            !file.exists() -> "Не существует / не экспортируется"
            !file.canRead() -> "Доступ запрещён Android"
            else -> "Чтение разрешено"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF101410.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun addLine(label: String, value: String, heading: Boolean = false) {
            val view = TextView(this).apply {
                text = if (heading) label else "$label\n$value"
                textSize = if (heading) 19f else 14f
                setTextColor(if (heading) 0xFF9BE15D.toInt() else 0xFFE7EDE5.toInt())
                if (heading) setTypeface(null, Typeface.BOLD)
                setPadding(0, if (heading) 22 else 10, 0, 10)
            }
            content.addView(view)
        }

        addLine("LimeRoot Research", "", true)
        addLine("Режим", "Только диагностика. Root не предоставляется.")
        addLine("Устройство", "${Build.MANUFACTURER} ${Build.MODEL}")
        addLine("Кодовое имя", readProp("ro.product.device"))
        addLine("Плата", readProp("ro.product.board"))
        addLine("Fingerprint", Build.FINGERPRINT)
        addLine("Android", "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        addLine("Патч безопасности", Build.VERSION.SECURITY_PATCH ?: "Неизвестно")
        addLine("Ядро", System.getProperty("os.version") ?: "Неизвестно")
        addLine("ABI", Build.SUPPORTED_ABIS.joinToString())
        addLine("Verified Boot", readProp("ro.boot.verifiedbootstate"))
        addLine("Boot locked", readProp("ro.boot.flash.locked"))

        addLine("Доступность диагностических файлов", "", true)
        addLine("/proc/config.gz", access("/proc/config.gz"))
        addLine("/proc/kallsyms", access("/proc/kallsyms"))
        addLine("/proc/modules", access("/proc/modules"))
        addLine("/proc/net/xfrm_stat", access("/proc/net/xfrm_stat"))

        addLine("Важно", "Отказ в доступе — ограничение Android, а не доказательство наличия или отсутствия уязвимости. Приложение ничего не меняет в системе.")

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }
}
