package io.github.awakelol.rex.watch

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import io.github.awakelol.rex.core.PackageFacts

class PackageInspector(context: Context) {
    private val pm = context.packageManager

    fun installedPackages(): Set<String> = installedApps().mapTo(HashSet()) { it.packageName }

    fun isInstalled(pkg: String): Boolean = appInfo(pkg) != null

    fun isSystemApp(pkg: String): Boolean =
        appInfo(pkg)?.let { it.flags and ApplicationInfo.FLAG_SYSTEM != 0 } ?: false

    fun label(pkg: String): String? = appInfo(pkg)?.let { pm.getApplicationLabel(it).toString() }

    fun icon(pkg: String): Drawable? = appInfo(pkg)?.let { pm.getApplicationIcon(it) }

    /** Labels for the given packages that are still installed. */
    fun labels(packages: Set<String>): Map<String, String> =
        installedApps()
            .filter { it.packageName in packages }
            .associate { it.packageName to pm.getApplicationLabel(it).toString() }

    fun facts(pkg: String): PackageFacts? {
        val app = appInfo(pkg) ?: return null
        val permissions = packageInfo(pkg, PackageManager.GET_PERMISSIONS)?.requestedPermissions.orEmpty()
        val services = packageInfo(pkg, PackageManager.GET_SERVICES)?.services.orEmpty()
        val receivers = packageInfo(pkg, PackageManager.GET_RECEIVERS)?.receivers.orEmpty()

        return PackageFacts(
            packageName = pkg,
            label = pm.getApplicationLabel(app).toString(),
            installer = installerOf(pkg),
            drawsOverApps = Manifest.permission.SYSTEM_ALERT_WINDOW in permissions,
            hasAccessibilityService = services.any { it.permission == Manifest.permission.BIND_ACCESSIBILITY_SERVICE },
            hasDeviceAdmin = receivers.any { it.permission == Manifest.permission.BIND_DEVICE_ADMIN },
            isHomeApp = hasHomeActivity(pkg),
            installsApps = Manifest.permission.REQUEST_INSTALL_PACKAGES in permissions,
            readsNotifications = services.any {
                it.permission == Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE
            },
        )
    }

    fun installerOf(pkg: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(pkg).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(pkg)
        }
    } catch (_: Exception) {
        null
    }

    private fun hasHomeActivity(pkg: String): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(pkg)
        val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(home, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(home, 0)
        }
        return matches.isNotEmpty()
    }

    private fun installedApps(): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }

    private fun appInfo(pkg: String): ApplicationInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(pkg, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    // Asked for one component type at a time: asking for everything at once can blow
    // past the binder size limit on very large apps.
    private fun packageInfo(pkg: String, flags: Int): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, flags)
        }
    } catch (_: Exception) {
        null
    }
}
