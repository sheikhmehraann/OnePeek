package io.github.sheikhmehraann.onepeek

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Resources

internal object ModuleResources {
    @Volatile
    private var moduleInfo: ApplicationInfo? = null

    private var cached: Resources? = null

    fun init(applicationInfo: ApplicationInfo) {
        synchronized(this) {
            moduleInfo = applicationInfo
            cached = null
        }
    }

    fun text(context: Context, resId: Int, vararg args: Any): String {
        if (context.packageName == BuildConfig.APPLICATION_ID) {
            return context.getString(resId, *args)
        }
        val config = context.resources.configuration
        val resources = synchronized(this) {
            val info = checkNotNull(moduleInfo) { "ModuleResources not initialized" }
            // Passing ApplicationInfo avoids Android 11+ package visibility blocks inside the host app.
            val current = cached
            if (current != null && current.configuration == config) {
                current
            } else {
                context.packageManager.getResourcesForApplication(info).also {
                    @Suppress("DEPRECATION")
                    it.updateConfiguration(config, context.resources.displayMetrics)
                    cached = it
                }
            }
        }
        return resources.getString(resId, *args)
    }
}
