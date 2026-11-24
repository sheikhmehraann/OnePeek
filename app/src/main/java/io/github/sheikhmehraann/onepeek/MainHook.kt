package io.github.sheikhmehraann.onepeek

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean

class MainHook : XposedModule() {
    private val installed = AtomicBoolean(false)
    private val sessions = Collections.synchronizedMap(WeakHashMap<Activity, BillingSession>())

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        Log.i(TAG, "Loaded on $frameworkName $frameworkVersion (API $apiVersion)")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != GOOGLE_ONE_PACKAGE) return
        ModuleResources.init(moduleApplicationInfo)
        if (!installed.compareAndSet(false, true)) return

        try {
            hook(Activity::class.java.getDeclaredMethod("onResume"))
                .setId("onepeek.activity.resume")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val result = chain.proceed(chain.args.toTypedArray())
                    val activity = chain.thisObject as? Activity
                    if (activity != null && shouldAttach(activity)) {
                        activity.runOnUiThread { installButton(activity) }
                    }
                    result
                }

            hook(Activity::class.java.getDeclaredMethod("onDestroy"))
                .setId("onepeek.activity.destroy")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val activity = chain.thisObject as? Activity
                    if (activity != null) {
                        sessions.remove(activity)?.close()
                    }
                    chain.proceed(chain.args.toTypedArray())
                }

            // Passively collect outbound server-delivered SKUs and doc IDs sent to Play Billing.
            hook(Bundle::class.java.getDeclaredMethod("putStringArrayList", String::class.java, ArrayList::class.java))
                .setId("onepeek.bundle.skus")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val args = chain.args
                    if (args.size >= 2 && args[0] in OUTBOUND_SKU_KEYS) {
                        val added = SkuCatalog.recordObserved(args[1] as? Collection<*>)
                        if (added > 0) {
                            Log.i(TAG, "Captured $added live outbound SKU(s) from Google One")
                        }
                    }
                    chain.proceed(args.toTypedArray())
                }

            // Passively collect inbound product IDs returned in Play Billing 9.2.0 response bundles.
            hook(Bundle::class.java.getDeclaredMethod("getStringArrayList", String::class.java))
                .setId("onepeek.bundle.details")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val args = chain.args
                    val result = chain.proceed(args.toTypedArray())
                    if (args.isNotEmpty() && args[0] in INBOUND_DETAILS_KEYS) {
                        val added = SkuCatalog.recordObservedDetailsJson(result as? Collection<*>)
                        if (added > 0) {
                            Log.i(TAG, "Captured $added live inbound SKU(s) from Play Billing response")
                        }
                    }
                    result
                }

            Log.i(TAG, "Google One hooks installed")
        } catch (t: Throwable) {
            installed.set(false)
            Log.e(TAG, "Failed to install Google One hooks", t)
        }
    }

    private fun shouldAttach(activity: Activity): Boolean {
        val name = activity.javaClass.name
        if (EXCLUDED_PREFIXES.any { name.startsWith(it) }) return false
        return name !in EXCLUDED_ACTIVITIES
    }

    private fun installButton(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        val decor = activity.window?.decorView as? FrameLayout ?: return
        if (decor.findViewWithTag<View>(BUTTON_TAG) != null) return

        val density = activity.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density + 0.5f).toInt()

        val button = TextView(activity).apply {
            tag = BUTTON_TAG
            text = ModuleResources.text(activity, R.string.button_inspect)
            contentDescription = ModuleResources.text(activity, R.string.button_inspect_desc)
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(9), dp(14), dp(9))
            elevation = dp(6).toFloat()
            background = GradientDrawable().apply {
                setColor(Color.rgb(29, 78, 216))
                cornerRadius = dp(24).toFloat()
            }
            setOnClickListener {
                if (activity.isFinishing || activity.isDestroyed) return@setOnClickListener
                val session = synchronized(sessions) {
                    sessions.getOrPut(activity) { BillingSession(activity) }
                }
                session.query()
            }
        }

        val layout = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.END or Gravity.BOTTOM,
        ).apply {
            marginEnd = dp(18)
            bottomMargin = dp(88)
        }
        decor.addView(button, layout)
    }

    private companion object {
        const val TAG = "OnePeek"
        const val GOOGLE_ONE_PACKAGE = "com.google.android.apps.subscriptions.red"
        const val BUTTON_TAG = "onepeek.googleone.button"

        val OUTBOUND_SKU_KEYS = setOf(
            "ITEM_ID_LIST",
            "SKU_SERIALIZED_DOCID_LIST",
        )

        val INBOUND_DETAILS_KEYS = setOf(
            "DETAILS_LIST",
            "UNFETCHED_PRODUCT_LIST",
        )

        val EXCLUDED_PREFIXES = listOf(
            "com.android.billingclient.",
        )

        val EXCLUDED_ACTIVITIES = setOf(
            "com.android.play.core.common.PlayCoreDialogWrapperActivity",
            "com.google.android.apps.subscriptions.red.sdk.TransparentGoogleOneSdkActivity",
            "com.google.android.gms.common.api.GoogleApiActivity",
            "com.google.android.libraries.notifications.entrypoints.systemtray.SystemTrayActivity",
            "com.google.apps.tiktok.nav.gateway.AnimatedGatewayActivity",
            "com.google.apps.tiktok.nav.gateway.GatewayActivity",
        )
    }
}
