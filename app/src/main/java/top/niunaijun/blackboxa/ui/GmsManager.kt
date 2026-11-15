package top.niunaijun.blackboxa.ui

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import top.niunaijun.blackbox.BlackBoxCore

/**
 * Google Play Services inside a clone space.
 *
 * Many apps — AI apps especially — need Play Services (Firebase, ML Kit,
 * sign-in) to run. A clone space starts without it, and those apps crash
 * on open. Installing it copies the device's own Google packages into
 * the virtual space, so the clone sees a working Play Services.
 *
 * Everything here deliberately avoids BlackBoxCore.isInstalled(): that
 * call falls back to the SYSTEM package manager when the engine service
 * is unhealthy, which reports every device-installed app as already
 * present in the space. Presence is checked with
 * getInstalledApplications() instead, which has no such fallback, and
 * installation goes package by package with the same reliable check.
 */
object GmsManager {
    private const val TAG = "GmsManager"

    const val GMS_PKG = "com.google.android.gms"

    /** Same set the engine itself installs (GmsCore.GOOGLE_SERVICE + GOOGLE_APP). */
    val GMS_PACKAGES = listOf(
        // Core services
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.gsf.login",
        "com.google.android.backuptransport",
        "com.google.android.backup",
        "com.google.android.configupdater",
        "com.google.android.syncadapters.contacts",
        "com.google.android.feedback",
        "com.google.android.onetimeinitializer",
        "com.google.android.partnersetup",
        "com.google.android.setupwizard",
        "com.google.android.syncadapters.calendar",
        // Google apps that go with them
        "com.android.vending",
        "com.google.android.play.games",
        "com.google.android.wearable.app",
        "com.google.android.wearable.app.cn"
    )

    /** One virtual space that holds clones, and whether it has Play Services. */
    data class GmsSpace(
        val userId: Int,
        val appCount: Int,
        val gmsInstalled: Boolean
    )

    /** Spaces that exist because they hold clones, with their GMS state. */
    fun loadGmsSpaces(context: Context, clones: List<CloneEntry>): List<GmsSpace> {
        val core = BlackBoxCore.get()
        return clones.groupBy { it.userId }
            .map { (userId, entries) ->
                val installed = try {
                    core.getInstalledApplications(0, userId)
                        ?.any { it.packageName == GMS_PKG } == true
                } catch (e: Exception) {
                    false
                }
                GmsSpace(userId, entries.size, installed)
            }
            .sortedBy { it.userId }
    }

    /** True when this clone's space already has Play Services. */
    fun hasGms(userId: Int): Boolean {
        return try {
            BlackBoxCore.get().getInstalledApplications(0, userId)
                ?.any { it.packageName == GMS_PKG } == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Install Play Services into one space. Copies each Google package
     * that exists on the device and is not already in the space.
     * Slow (the Google apps are big) — call it off the main thread.
     */
    fun installGms(context: Context, userId: Int): Result<Unit> {
        val core = BlackBoxCore.get()
        val pm = context.packageManager
        val alreadyThere = try {
            core.getInstalledApplications(0, userId)
                ?.map { it.packageName }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
        for (pkg in GMS_PACKAGES) {
            if (pkg in alreadyThere) continue
            // Only install what actually exists on this device.
            try {
                pm.getApplicationInfo(pkg, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            } catch (e: Exception) {
                continue
            }
            try {
                val result = core.installPackageAsUser(pkg, userId)
                if (!result.success) {
                    Log.e(TAG, "installGms $pkg failed: ${result.msg}")
                    return Result.failure(
                        IllegalStateException(result.msg ?: "Could not install $pkg")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "installGms $pkg failed: ${e.message}")
                return Result.failure(e)
            }
        }
        return Result.success(Unit)
    }
}
