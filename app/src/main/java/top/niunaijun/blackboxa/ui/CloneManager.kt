package top.niunaijun.blackboxa.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.view.main.ShortcutActivity

/** One running copy of an app inside the virtual engine. */
data class CloneEntry(
    val packageName: String,
    val appName: String,
    val userId: Int,
    /** Display number: 1 for the first clone of this app, 2 for the second, ... */
    val number: Int
)

/** An app installed on the device that can be cloned. */
data class DeviceApp(
    val packageName: String,
    val appName: String,
    val cloneCount: Int
)

/**
 * Engine-facing clone operations. A clone is a (packageName, userId) pair:
 * each virtual user id is a separate space with its own data, so two clones
 * of the same app are two independent accounts. No root, no work profile.
 */
object CloneManager {
    private const val TAG = "CloneManager"
    const val MAX_USER_ID = 49

    /** All launchable apps on the device, except CloneX itself. */
    fun loadDeviceApps(context: Context): List<DeviceApp> {
        val pm = context.packageManager
        val core = BlackBoxCore.get()
        val users = try {
            core.users
        } catch (e: Exception) {
            emptyList()
        }
        val hostPkg = context.packageName
        return try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { info ->
                    info.packageName != hostPkg &&
                        pm.getLaunchIntentForPackage(info.packageName) != null
                }
                .map { info ->
                    val label = try {
                        pm.getApplicationLabel(info).toString()
                    } catch (e: Exception) {
                        info.packageName
                    }
                    val count = users.count { user ->
                        try {
                            core.isInstalled(info.packageName, user.id)
                        } catch (e: Exception) {
                            false
                        }
                    }
                    DeviceApp(info.packageName, label, count)
                }
                .sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "loadDeviceApps failed: ${e.message}")
            emptyList()
        }
    }

    /** Every clone currently installed, numbered per app. */
    fun loadClones(context: Context): List<CloneEntry> {
        val core = BlackBoxCore.get()
        val vpm = BlackBoxCore.getPackageManager()
        val users = try {
            core.users
        } catch (e: Exception) {
            return emptyList()
        }
        val byPackage = mutableMapOf<String, MutableList<Pair<Int, ApplicationInfo>>>()
        for (user in users) {
            val list = try {
                core.getInstalledApplications(0, user.id)
            } catch (e: Exception) {
                null
            } ?: continue
            for (app in list) {
                byPackage.getOrPut(app.packageName) { mutableListOf() }.add(user.id to app)
            }
        }
        val out = mutableListOf<CloneEntry>()
        for ((pkg, entries) in byPackage) {
            entries.sortBy { it.first }
            entries.forEachIndexed { index, (userId, appInfo) ->
                val label = try {
                    vpm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg
                }
                out.add(CloneEntry(pkg, label, userId, index + 1))
            }
        }
        return out.sortedWith(compareBy({ it.appName.lowercase() }, { it.number }))
    }

    /** Clone [packageName] into the first free virtual space. */
    fun cloneApp(context: Context, packageName: String): Result<CloneEntry> {
        val core = BlackBoxCore.get()
        val existing = try {
            loadClones(context).count { it.packageName == packageName }
        } catch (e: Exception) {
            0
        }
        for (userId in 0..MAX_USER_ID) {
            val installed = try {
                core.isInstalled(packageName, userId)
            } catch (e: Exception) {
                false
            }
            if (!installed) {
                return try {
                    val result = core.installPackageAsUser(packageName, userId)
                    if (result.success) {
                        val label = try {
                            context.packageManager
                                .getApplicationLabel(
                                    context.packageManager.getApplicationInfo(packageName, 0)
                                ).toString()
                        } catch (e: Exception) {
                            packageName
                        }
                        Result.success(CloneEntry(packageName, label, userId, existing + 1))
                    } else {
                        Result.failure(IllegalStateException(result.msg ?: "Install failed"))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "cloneApp failed: ${e.message}")
                    Result.failure(e)
                }
            }
        }
        return Result.failure(IllegalStateException("No free clone slot"))
    }

    fun launch(packageName: String, userId: Int): Boolean {
        return try {
            BlackBoxCore.get().launchApk(packageName, userId)
        } catch (e: Exception) {
            Log.e(TAG, "launch failed: ${e.message}")
            false
        }
    }

    /** Delete a clone. Removes its virtual space if nothing else lives there. */
    fun deleteClone(packageName: String, userId: Int) {
        val core = BlackBoxCore.get()
        try {
            core.uninstallPackageAsUser(packageName, userId)
        } catch (e: Exception) {
            Log.e(TAG, "uninstall failed: ${e.message}")
        }
        try {
            if (core.getInstalledApplications(0, userId).isNullOrEmpty()) {
                core.deleteUser(userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteUser failed: ${e.message}")
        }
    }

    /** App icon with the clone number composited as a green badge, like the home screen. */
    fun badgedIcon(context: Context, packageName: String, number: Int): Bitmap? {
        val drawable: Drawable = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            return null
        }
        val size = 192
        val base = drawable.toBitmap(size, size)
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val radius = 34f
        val cx = out.width - radius - 4f
        val cy = out.height - radius - 4f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(cx, cy, radius + 5f, ring)
        val badge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#22C55E") }
        canvas.drawCircle(cx, cy, radius, badge)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val label = number.toString()
        val bounds = Rect()
        text.getTextBounds(label, 0, label.length, bounds)
        canvas.drawText(label, cx, cy - bounds.exactCenterY(), text)
        return out
    }

    /** Pin "AppName N" on the home screen, launching this clone. */
    fun createHomeShortcut(context: Context, clone: CloneEntry): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val icon = badgedIcon(context, clone.packageName, clone.number) ?: return false
        val label = "${clone.appName} ${clone.number}"
        val intent = Intent(context, ShortcutActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .putExtra("pkg", clone.packageName)
            .putExtra("userId", clone.userId)
        val info = ShortcutInfoCompat.Builder(context, "${clone.packageName}_${clone.userId}")
            .setIntent(intent)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithBitmap(icon))
            .build()
        return try {
            ShortcutManagerCompat.requestPinShortcut(context, info, null)
        } catch (e: Exception) {
            Log.e(TAG, "createHomeShortcut failed: ${e.message}")
            false
        }
    }
}
