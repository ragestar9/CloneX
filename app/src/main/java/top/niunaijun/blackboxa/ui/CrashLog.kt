package top.niunaijun.blackboxa.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Crash report for cloned apps.
 *
 * When a clone crashes, Android writes the reason (FATAL EXCEPTION +
 * stack trace) to the system log. Clones run in CloneX's own processes,
 * so CloneX can read those lines back with logcat — no root, no PC,
 * no adb needed. This collects the last crash block plus device info
 * so the exact cause can be fixed instead of guessed.
 */
object CrashLog {
    private const val TAG = "CrashLog"

    /** Full report text, ready to paste. */
    fun collect(context: Context): String {
        val version = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
        val header = buildString {
            appendLine("CloneX crash report")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("CloneX: $version")
            appendLine()
        }
        val lines = readLogcat()
        if (lines.isEmpty()) {
            return header + "No log lines captured — this phone blocks log access, " +
                "so the crash reason could not be read on-device."
        }
        val fatalAt = lines.indexOfLast { it.contains("FATAL EXCEPTION") }
        val block = if (fatalAt >= 0) {
            val from = maxOf(0, fatalAt - 30)
            val to = minOf(lines.size, fatalAt + 80)
            lines.subList(from, to)
        } else {
            val interesting = lines.filter {
                it.contains("AndroidRuntime") || it.contains("has died") ||
                    it.contains("crash", ignoreCase = true) ||
                    it.contains("FATAL", ignoreCase = true)
            }
            if (interesting.isNotEmpty()) interesting.takeLast(80) else lines.takeLast(80)
        }
        return header + block.joinToString("\n").take(12000)
    }

    private fun readLogcat(): List<String> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "2000"))
            val out = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            out
        } catch (e: Exception) {
            Log.e(TAG, "logcat failed: ${e.message}")
            emptyList()
        }
    }

    fun copyToClipboard(context: Context, text: String): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("CloneX crash report", text))
            true
        } catch (e: Exception) {
            Log.e(TAG, "clipboard failed: ${e.message}")
            false
        }
    }
}
