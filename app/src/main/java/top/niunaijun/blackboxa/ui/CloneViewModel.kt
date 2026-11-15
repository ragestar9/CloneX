package top.niunaijun.blackboxa.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CloneViewModel(application: Application) : AndroidViewModel(application) {

    private val _clones = MutableStateFlow<List<CloneEntry>>(emptyList())
    val clones: StateFlow<List<CloneEntry>> = _clones

    private val _apps = MutableStateFlow<List<DeviceApp>>(emptyList())
    val apps: StateFlow<List<DeviceApp>> = _apps

    private val _icons = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val icons: StateFlow<Map<String, Bitmap>> = _icons

    /** Package names currently being cloned. */
    private val _cloning = MutableStateFlow<Set<String>>(emptySet())
    val cloning: StateFlow<Set<String>> = _cloning

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    /** Clone spaces and whether each one has Google Play Services. */
    private val _gmsSpaces = MutableStateFlow<List<GmsManager.GmsSpace>>(emptyList())
    val gmsSpaces: StateFlow<List<GmsManager.GmsSpace>> = _gmsSpaces

    /** User ids currently having Play Services installed. */
    private val _gmsInstalling = MutableStateFlow<Set<Int>>(emptySet())
    val gmsInstalling: StateFlow<Set<Int>> = _gmsInstalling

    /** Last collected crash report text, if any. */
    private val _crashReport = MutableStateFlow<String?>(null)
    val crashReport: StateFlow<String?> = _crashReport

    private val _collectingCrashReport = MutableStateFlow(false)
    val collectingCrashReport: StateFlow<Boolean> = _collectingCrashReport

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            val ctx = getApplication<Application>().applicationContext
            _apps.value = CloneManager.loadDeviceApps(ctx)
            _clones.value = CloneManager.loadClones(ctx)
            _gmsSpaces.value = GmsManager.loadGmsSpaces(ctx, _clones.value)
            _loading.value = false
        }
    }

    fun clone(packageName: String, appName: String) {
        if (_cloning.value.contains(packageName)) return
        viewModelScope.launch(Dispatchers.IO) {
            _cloning.value = _cloning.value + packageName
            val ctx = getApplication<Application>().applicationContext
            val result = CloneManager.cloneApp(ctx, packageName)
            _cloning.value = _cloning.value - packageName
            if (result.isSuccess) {
                val clone = result.getOrNull()!!
                _clones.value = CloneManager.loadClones(ctx)
                _apps.value = CloneManager.loadDeviceApps(ctx)
                _gmsSpaces.value = GmsManager.loadGmsSpaces(ctx, _clones.value)
                _message.value = if (GmsManager.hasGms(clone.userId)) {
                    "${clone.appName} ${clone.number} is ready"
                } else {
                    "${clone.appName} ${clone.number} is ready — if it crashes when " +
                        "opened, install Google Play Services in Settings"
                }
            } else {
                _message.value = "Could not clone $appName: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun launch(clone: CloneEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!CloneManager.launch(clone.packageName, clone.userId)) {
                _message.value = "Could not launch ${clone.appName} ${clone.number}"
            }
        }
    }

    fun delete(clone: CloneEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>().applicationContext
            CloneManager.deleteClone(clone.packageName, clone.userId)
            _clones.value = CloneManager.loadClones(ctx)
            _apps.value = CloneManager.loadDeviceApps(ctx)
            _gmsSpaces.value = GmsManager.loadGmsSpaces(ctx, _clones.value)
            _message.value = "${clone.appName} ${clone.number} deleted"
        }
    }

    /** Install Google Play Services into one clone space (takes a while). */
    fun installGms(userId: Int) {
        if (_gmsInstalling.value.contains(userId)) return
        viewModelScope.launch(Dispatchers.IO) {
            _gmsInstalling.value = _gmsInstalling.value + userId
            val ctx = getApplication<Application>().applicationContext
            val result = GmsManager.installGms(ctx, userId)
            _gmsInstalling.value = _gmsInstalling.value - userId
            _gmsSpaces.value = GmsManager.loadGmsSpaces(ctx, _clones.value)
            _message.value = if (result.isSuccess) {
                "Google Play Services installed — open your clones again"
            } else {
                "Could not install Google Play Services: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    /** Read the last clone crash from the system log and copy it. */
    fun copyCrashReport(context: Context) {
        if (_collectingCrashReport.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _collectingCrashReport.value = true
            val report = CrashLog.collect(context.applicationContext)
            _crashReport.value = report
            _collectingCrashReport.value = false
            _message.value = if (CrashLog.copyToClipboard(context, report)) {
                "Crash report copied — paste it to Muse"
            } else {
                "Could not copy the crash report"
            }
        }
    }

    fun addShortcut(context: Context, clone: CloneEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = CloneManager.createHomeShortcut(context, clone)
            _message.value = if (ok) {
                "Home screen icon added for ${clone.appName} ${clone.number}"
            } else {
                "Your launcher does not allow pinned icons"
            }
        }
    }

    /** Load an app icon once and cache it by package name. */
    fun ensureIcon(packageName: String) {
        if (_icons.value.containsKey(packageName)) return
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>().applicationContext
            val drawable: Drawable? = try {
                ctx.packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                null
            }
            val bitmap = try {
                drawable?.toBitmap(144, 144)
            } catch (e: Exception) {
                null
            }
            if (bitmap != null) {
                _icons.value = _icons.value + (packageName to bitmap)
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
