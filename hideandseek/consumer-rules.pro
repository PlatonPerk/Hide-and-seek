# The website bridge is called from JavaScript by name.
-keepclassmembers class app.hideandseek.web.WebSpotBridge {
    @android.webkit.JavascriptInterface <methods>;
}
