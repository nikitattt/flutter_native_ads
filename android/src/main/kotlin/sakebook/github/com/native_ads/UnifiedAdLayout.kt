package sakebook.github.com.native_ads

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.formats.UnifiedNativeAdView
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import android.os.Bundle
import com.google.ads.mediation.admob.AdMobAdapter
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable


class UnifiedAdLayout(context: Context, messenger: BinaryMessenger, id: Int, arguments: HashMap<String, Any>) : PlatformView {

    private val hostPackageName = arguments["package_name"].toString()
    private val layoutRes = context.resources.getIdentifier(arguments["layout_name"].toString(), "layout", hostPackageName)
    private val unifiedNativeAdView: UnifiedNativeAdView = View.inflate(context, layoutRes, null) as UnifiedNativeAdView
    private val headlineView: TextView = unifiedNativeAdView.findViewById(context.resources.getIdentifier("flutter_native_ad_headline", "id", hostPackageName))
    private val bodyView: TextView = unifiedNativeAdView.findViewById(context.resources.getIdentifier("flutter_native_ad_body", "id", hostPackageName))
    private val callToActionView: TextView = unifiedNativeAdView.findViewById(context.resources.getIdentifier("flutter_native_ad_call_to_action", "id", hostPackageName))

    private val iconView: ImageView? = unifiedNativeAdView.findViewById(context.resources.getIdentifier("flutter_native_ad_icon", "id", hostPackageName))
    private val starRatingView: TextView? = unifiedNativeAdView.findViewById(context.resources.getIdentifier("flutter_native_ad_star", "id", hostPackageName))
    private val storeView: TextView? = unifiedNativeAdView.findViewById(context.resources.getIdentifier("flutter_native_ad_store", "id", hostPackageName))
    private val priceView: TextView? = unifiedNativeAdView.findViewById(context.resources.getIdentifier("flutter_native_ad_price", "id", hostPackageName))
    private val advertiserView: TextView? = unifiedNativeAdView.findViewById(context.resources.getIdentifier("flutter_native_ad_advertiser", "id", hostPackageName))

    private val methodChannel: MethodChannel = MethodChannel(messenger, "com.github.sakebook.android/unified_ad_layout_$id")
    private var ad: UnifiedNativeAd? = null

    init {
        unifiedNativeAdView.findViewById<TextView>(context.resources.getIdentifier("flutter_native_ad_attribution", "id", hostPackageName)).apply {
            this.text = arguments["text_attribution"].toString()
        }

        val extras = Bundle()
        extras.putString("npa", "1")

        AdLoader.Builder(context, arguments["placement_id"].toString())
                .forUnifiedNativeAd {
                    ad = it
                    ensureUnifiedAd(it)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdImpression() {
                        super.onAdImpression()
                        methodChannel.invokeMethod("onAdImpression", null)
                    }

                    override fun onAdLeftApplication() {
                        super.onAdLeftApplication()
                        methodChannel.invokeMethod("onAdLeftApplication", null)
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        methodChannel.invokeMethod("onAdClicked", null)
                    }

                    override fun onAdFailedToLoad(errorCode: Int) {
                        super.onAdFailedToLoad(errorCode)
                        methodChannel.invokeMethod("onAdFailedToLoad", hashMapOf("errorCode" to errorCode))
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        methodChannel.invokeMethod("onAdLoaded", null)
                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder()
                        .build())
                .build()
                .loadAd(
                        if (arguments["personalized"] == false) AdRequest.Builder()
                                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                                .build()
                        else AdRequest.Builder()
                                .build())
    }

    override fun getView(): View {
        return unifiedNativeAdView
    }

    override fun dispose() {
        ad?.destroy()
        unifiedNativeAdView.removeAllViews()
        methodChannel.setMethodCallHandler(null)
    }

    private fun ensureUnifiedAd(ad: UnifiedNativeAd?) {
        headlineView.text = ad?.headline
        bodyView.text = ad?.body
        callToActionView.text = ad?.callToAction

        val roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
                Resources.getSystem(),
                ad?.icon?.drawable?.toBitmap()
        )

        roundedBitmapDrawable.cornerRadius = 95.0f
        roundedBitmapDrawable.setAntiAlias(true)

        iconView?.setImageDrawable(roundedBitmapDrawable)
        starRatingView?.text = "${ad?.starRating}"
        storeView?.text = ad?.store
        priceView?.text = ad?.price
        advertiserView?.text = ad?.advertiser

        unifiedNativeAdView.bodyView = bodyView
        unifiedNativeAdView.headlineView = headlineView
        unifiedNativeAdView.callToActionView = callToActionView

        unifiedNativeAdView.iconView = iconView
        unifiedNativeAdView.starRatingView = starRatingView
        unifiedNativeAdView.storeView = storeView
        unifiedNativeAdView.priceView = priceView
        unifiedNativeAdView.advertiserView = advertiserView

        unifiedNativeAdView.setNativeAd(ad)
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) {
            return bitmap
        }

        val width = if (bounds.isEmpty) intrinsicWidth else bounds.width()
        val height = if (bounds.isEmpty) intrinsicHeight else bounds.height()

        return Bitmap.createBitmap(width.nonZero(), height.nonZero(), Bitmap.Config.ARGB_8888).also {
            val canvas = Canvas(it)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
        }
    }

    private fun Int.nonZero() = if (this <= 0) 1 else this
}