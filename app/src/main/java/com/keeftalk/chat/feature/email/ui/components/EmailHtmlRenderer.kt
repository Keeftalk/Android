package com.keeftalk.chat.feature.email.ui.components

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun EmailHtmlRenderer(
    htmlContent: String,
    modifier: Modifier = Modifier
) {
    val sanitizedHtml = sanitizeHtml(htmlContent)
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        request?.url?.let { uri ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle case where no app can handle the intent
                            }
                        }
                        return true
                    }
                }
                settings.apply {
                    javaScriptEnabled = false
                    blockNetworkImage = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, sanitizedHtml, "text/html", "UTF-8", null)
        },
        modifier = modifier.fillMaxWidth()
    )
}

private fun sanitizeHtml(html: String): String {
    var sanitized = html
    // Remove scripts
    sanitized = sanitized.replace("(?i)<script.*?>.*?</script>".toRegex(), "")
    // Remove iframes
    sanitized = sanitized.replace("(?i)<iframe.*?>.*?</iframe>".toRegex(), "")
    // Remove forms
    sanitized = sanitized.replace("(?i)<form.*?>.*?</form>".toRegex(), "")
    
    // Basic CSS for better mobile rendering and fitting to screen
    val style = """
        <style>
            body { 
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                font-size: 16px;
                line-height: 1.5;
                color: #202124;
                margin: 0;
                padding: 12px;
                word-wrap: break-word;
                overflow-wrap: break-word;
                -webkit-text-size-adjust: 100%;
            }
            img {
                max-width: 100% !important;
                height: auto !important;
                display: block;
                margin: 8px 0;
            }
            table {
                width: 100% !important;
                max-width: 100% !important;
                border-collapse: collapse;
                table-layout: fixed;
            }
            a {
                color: #1a73e8;
                text-decoration: none;
            }
            pre, code {
                white-space: pre-wrap;
                word-break: break-all;
            }
        </style>
    """.trimIndent()
    
    val viewport = "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">"
    
    return "<html><head>$viewport$style</head><body>$sanitized</body></html>"
}
