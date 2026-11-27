package com.beatpath.app.data

import android.content.Context
import android.content.Intent
import java.io.File

/**
 * Opt-in crash reports with no servers: if the app ever closes by accident, the error is written to a file on the phone.
 * Next time, the Me tab offers to share it through the normal Android share sheet (the user picks where it goes) or delete it.
 */
object CrashLog {
    private fun file(c: Context) = File(c.filesDir, "last-crash.txt")

    private var installed = false
    fun install(c: Context) {
        if (installed) return; installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                val info = c.packageManager.getPackageInfo(c.packageName, 0)
                file(c).writeText("Beatpath ${info.versionName} on Android ${android.os.Build.VERSION.RELEASE} (${android.os.Build.MODEL})\n\n" + e.stackTraceToString().take(12_000))
            }
            previous?.uncaughtException(t, e)
        }
    }

    fun pending(c: Context): Boolean = file(c).exists()
    fun clear(c: Context) { file(c).delete() }

    /** The share sheet with the report as plain text; nothing is sent unless the user chooses an app. */
    fun shareIntent(c: Context): Intent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "Beatpath crash report"); putExtra(Intent.EXTRA_TEXT, runCatching { file(c).readText() }.getOrDefault(""))
    }, "Share the crash report")
}
