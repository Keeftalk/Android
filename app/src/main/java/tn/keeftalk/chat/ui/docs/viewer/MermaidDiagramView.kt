package tn.keeftalk.chat.ui.docs.viewer

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidDiagramView(
    mermaidCode: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    var isInstalled by remember { mutableStateOf(MermaidDownloadManager.isMermaidInstalled(context)) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var showPrompt by remember { mutableStateOf(!isInstalled && !MermaidDownloadManager.isDeclined()) }

    val coroutineScope = rememberCoroutineScope()
    val mermaidFile = remember(isInstalled) { MermaidDownloadManager.getMermaidFile(context) }

    if (!isInstalled && showPrompt) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF8F9FA)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Download Mermaid Diagram Engine",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDark) Color.White else Color.Black
                )
                Text(
                    text = "This document contains Mermaid diagrams. Download the rendering engine (~500KB) once for secure, offline rendering.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Text(
                        text = "Downloading... ${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                } else if (downloadError != null) {
                    Text(
                        text = downloadError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Button(onClick = {
                        downloadError = null
                        isDownloading = true
                        coroutineScope.launch {
                            MermaidDownloadManager.downloadMermaid(context) { prog ->
                                downloadProgress = prog
                            }.fold(
                                onSuccess = {
                                    isInstalled = true
                                    isDownloading = false
                                    showPrompt = false
                                },
                                onFailure = { err ->
                                    downloadError = err.localizedMessage ?: "Download failed"
                                    isDownloading = false
                                }
                            )
                        }
                    }) {
                        Text("Retry Download")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            showPrompt = false
                            MermaidDownloadManager.setDeclined(true)
                        }) {
                            Text("Not Now")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            isDownloading = true
                            coroutineScope.launch {
                                MermaidDownloadManager.downloadMermaid(context) { prog ->
                                    downloadProgress = prog
                                }.fold(
                                    onSuccess = {
                                        isInstalled = true
                                        isDownloading = false
                                        showPrompt = false
                                    },
                                    onFailure = { err ->
                                        downloadError = err.localizedMessage ?: "Download failed"
                                        isDownloading = false
                                    }
                                )
                            }
                        }) {
                            Text("Download")
                        }
                    }
                }
            }
        }
    } else if (!isInstalled) {
        // If user declined or not installed, show placeholder with option to show prompt again
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(if (isDark) Color(0xFF2A2A2A) else Color(0xFFF1F1F1)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mermaid diagram (engine not downloaded)", style = MaterialTheme.typography.bodySmall, color = if (isDark) Color.LightGray else Color.DarkGray)
                OutlinedButton(onClick = { showPrompt = true }) {
                    Text("Download Mermaid Engine")
                }
            }
        }
    } else {
        val mermaidScriptUri = "file://${mermaidFile.absolutePath}"
        val htmlContent = remember(mermaidCode, isDark, mermaidFile) {
            val theme = if (isDark) "dark" else "default"
            val bgColor = if (isDark) "#1E1E1E" else "#FFFFFF"
            val textColor = if (isDark) "#FFFFFF" else "#000000"

            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes">
                <style>
                    html, body {
                        background-color: $bgColor;
                        color: $textColor;
                        margin: 0;
                        padding: 12px;
                        width: max-content;
                        min-width: 100%;
                        height: max-content;
                        min-height: 100%;
                        box-sizing: border-box;
                    }
                    .mermaid {
                        display: flex;
                        justify-content: flex-start;
                        align-items: flex-start;
                        width: max-content;
                        min-width: 100%;
                    }
                    svg {
                        width: auto !important;
                        height: auto !important;
                        max-width: none !important;
                    }
                    .error-card {
                        background-color: #ffebee;
                        color: #c62828;
                        border: 1px solid #ef9a9a;
                        padding: 12px;
                        border-radius: 8px;
                        font-family: monospace;
                        font-size: 13px;
                        white-space: pre-wrap;
                    }
                </style>
                <script src="$mermaidScriptUri"></script>
                <script>
                    window.addEventListener('DOMContentLoaded', async () => {
                        mermaid.initialize({
                            startOnLoad: false,
                            theme: '$theme',
                            securityLevel: 'loose',
                            flowchart: { htmlLabels: true, curve: 'basis' }
                        });
                        const container = document.getElementById('mermaid-container');
                        try {
                            const { svg } = await mermaid.render('mermaid-svg-' + Math.random().toString(36).substring(2, 9), container.textContent.trim());
                            container.innerHTML = svg;
                        } catch (err) {
                            console.error('Mermaid render error:', err);
                            container.innerHTML = '<div class="error-card"><strong>Mermaid Syntax Error:</strong>\n' + (err.message || err) + '</div>';
                        }
                    });
                </script>
            </head>
            <body>
                <div id="mermaid-container" class="mermaid">
                $mermaidCode
                </div>
            </body>
            </html>
            """.trimIndent()
        }

        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 500.dp),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = true
                    setBackgroundColor(if (isDark) android.graphics.Color.parseColor("#1E1E1E") else android.graphics.Color.WHITE)
                    webViewClient = WebViewClient()
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL("file://${mermaidFile.parent}/", htmlContent, "text/html", "UTF-8", null)
            }
        )
    }
}
