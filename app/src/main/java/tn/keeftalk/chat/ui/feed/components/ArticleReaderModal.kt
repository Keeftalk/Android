package tn.keeftalk.chat.ui.feed.components

import android.content.Intent
import android.net.Uri
import android.text.Html
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.jsoup.Jsoup
import tn.keeftalk.chat.domain.model.FeedArticle
import tn.keeftalk.chat.ui.theme.AppTheme
import tn.keeftalk.chat.ui.theme.LocalAppTheme
import tn.keeftalk.chat.ui.theme.keef.KeefGlassCard
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import tn.keeftalk.chat.ui.theme.keef.LocalKeefThemeTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArticleReaderModal(
    article: FeedArticle,
    isLiked: Boolean,
    isSaved: Boolean,
    likedSourceCount: Int,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onOpenImageInLightbox: (String, List<String>) -> Unit,
    onDownloadImage: (String) -> Unit,
    onOpenUrlInBrowser: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isKeef = false
    val keefTokens = LocalKeefThemeTokens.current

    val isRtl = remember(article.title, article.description, article.content) {
        isRtlText(article.title) || isRtlText(article.description) || isRtlText(article.content)
    }
    val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val bgContainerColor = if (isKeef) Color.Transparent else MaterialTheme.colorScheme.background
    val topBarBg = if (isKeef) keefTokens.card else MaterialTheme.colorScheme.surface
    val primaryTextColor = if (isKeef) keefTokens.ink else MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = if (isKeef) keefTokens.ink2 else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (isKeef) keefTokens.accent else MaterialTheme.colorScheme.primary

    // Extract images from article content & description
    val allImages = remember(article) {
        val list = mutableListOf<String>()
        article.thumbnailUrl?.let { if (it.isNotBlank()) list.add(it) }
        val doc = Jsoup.parse("${article.content ?: ""} ${article.description ?: ""}")
        doc.select("img").forEach { img ->
            val src = img.attr("src")
            if (src.isNotBlank() && src.startsWith("http") && !list.contains(src)) {
                list.add(src)
            }
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgContainerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Modal Top Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                color = topBarBg,
                tonalElevation = 0.dp,
                shadowElevation = if (isKeef) 0.dp else 2.dp,
                border = if (isKeef) BorderStroke(1.dp, keefTokens.cardLine) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isKeef) keefTokens.soft else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = primaryTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Action Buttons (Like & Save)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Like Button
                        Surface(
                            onClick = onToggleLike,
                            shape = RoundedCornerShape(11.dp),
                            color = if (isLiked) accentColor else if (isKeef) keefTokens.soft else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color.White else primaryTextColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isLiked) "Liked" else "Like",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isLiked) Color.White else primaryTextColor
                                )
                            }
                        }

                        // Save Button
                        Surface(
                            onClick = onToggleSave,
                            shape = RoundedCornerShape(11.dp),
                            color = if (isSaved) accentColor else if (isKeef) keefTokens.soft else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = "Save",
                                    tint = if (isSaved) Color.White else primaryTextColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isSaved) "Saved" else "Save",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSaved) Color.White else primaryTextColor
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable Content Container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Reader Gallery Grid if images exist
                        if (allImages.isNotEmpty()) {
                            ReaderImageGallery(
                                images = allImages.take(4),
                                onImageClick = { imgUrl -> onOpenImageInLightbox(imgUrl, allImages) },
                                onDownload = { imgUrl -> onDownloadImage(imgUrl) }
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // Reader Meta Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(article.sourceName ?: "Feed").uppercase()} · ${formatTime(article.pubDate)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = secondaryTextColor,
                                letterSpacing = 0.7.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Title
                        Text(
                            text = article.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryTextColor,
                            letterSpacing = (-0.7).sp,
                            lineHeight = 28.sp
                        )

                        if (likedSourceCount > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Heart",
                                    tint = Color(0xFFEC4899),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "You've liked $likedSourceCount ${if (likedSourceCount == 1) "story" else "stories"} from this source",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryTextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Render Complete Article Content & Description
                        val contentHtml = article.content?.takeIf { it.isNotBlank() } 
                            ?: article.description?.takeIf { it.isNotBlank() } 
                            ?: "<p>${article.title}. Read the complete story on the original publisher website by tapping below.</p>"
                        ReaderHtmlContent(
                            html = contentHtml,
                            description = if (article.content != null && article.content != article.description) article.description else null,
                            onImageClick = { imgUrl -> onOpenImageInLightbox(imgUrl, allImages) }
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        // Open Original Source Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor)
                                .clickable {
                                    onOpenUrlInBrowser(article.link)
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Open original article",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.NorthEast,
                                    contentDescription = "External link",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ReaderImageGallery(
    images: List<String>,
    onImageClick: (String) -> Unit,
    onDownload: (String) -> Unit
) {
    val count = images.size
    when (count) {
        1 -> {
            GalleryImageItem(
                url = images[0],
                aspectRatio = 16f / 9f,
                onImageClick = { onImageClick(images[0]) },
                onDownload = { onDownload(images[0]) }
            )
        }
        2 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    GalleryImageItem(
                        url = images[0],
                        aspectRatio = 1f,
                        onImageClick = { onImageClick(images[0]) },
                        onDownload = { onDownload(images[0]) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    GalleryImageItem(
                        url = images[1],
                        aspectRatio = 1f,
                        onImageClick = { onImageClick(images[1]) },
                        onDownload = { onDownload(images[1]) }
                    )
                }
            }
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                images.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { url ->
                            Box(modifier = Modifier.weight(1f)) {
                                GalleryImageItem(
                                    url = url,
                                    aspectRatio = 16f / 10f,
                                    onImageClick = { onImageClick(url) },
                                    onDownload = { onDownload(url) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryImageItem(
    url: String,
    aspectRatio: Float,
    onImageClick: () -> Unit,
    onDownload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE9EBF2))
            .clickable { onImageClick() }
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF0A0C19).copy(alpha = 0.35f)),
                        startY = 100f
                    )
                )
        )

        // Download mini button
        Box(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.BottomStart)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A0C19).copy(alpha = 0.55f))
                .clickable { onDownload() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download",
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }

        // Zoom hint button
        Box(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF0A0C19).copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = "Zoom",
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
fun ReaderHtmlContent(
    html: String,
    description: String? = null,
    onImageClick: (String) -> Unit
) {
    val isKeef = false
    val keefTokens = LocalKeefThemeTokens.current

    val primaryTextColor = if (isKeef) keefTokens.ink else MaterialTheme.colorScheme.onBackground
    val bodyTextColor = if (isKeef) keefTokens.ink2 else MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = if (isKeef) keefTokens.accent else MaterialTheme.colorScheme.primary

    val doc = remember(html) { Jsoup.parse(html) }
    val elements = remember(doc) {
        val selected = doc.select("p, h1, h2, h3, h4, h5, h6, blockquote, li, div:not(:has(p, div))")
        if (selected.isNotEmpty()) selected else doc.body().children()
    }
    val bodyText = doc.body().text().trim()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // First show description summary if available and distinct
        if (!description.isNullOrBlank()) {
            val cleanDesc = Html.fromHtml(description).toString().trim()
            if (cleanDesc.isNotBlank()) {
                Text(
                    text = cleanDesc,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 25.sp,
                    color = primaryTextColor
                )
            }
        }

        if (elements.isNotEmpty()) {
            elements.forEach { element ->
                when (element.tagName().lowercase()) {
                    "p", "li", "div" -> {
                        val text = Html.fromHtml(element.html()).toString().trim()
                        if (text.isNotBlank()) {
                            Text(
                                text = text,
                                fontSize = 14.5.sp,
                                lineHeight = 25.sp,
                                color = bodyTextColor,
                                letterSpacing = (-0.1).sp
                            )
                        }
                    }
                    "h1", "h2" -> {
                        Text(
                            text = element.text(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryTextColor,
                            letterSpacing = (-0.4).sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    "h3", "h4", "h5", "h6" -> {
                        Text(
                            text = element.text(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    "img" -> {
                        val src = element.attr("src")
                        if (src.isNotBlank()) {
                            AsyncImage(
                                model = src,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onImageClick(src) },
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                    "blockquote" -> {
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = Html.fromHtml(element.html()).toString().trim(),
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = bodyTextColor,
                                lineHeight = 22.sp
                            )
                        }
                    }
                    else -> {
                        val text = element.text().trim()
                        if (text.isNotBlank()) {
                            Text(
                                text = Html.fromHtml(element.html()).toString().trim(),
                                fontSize = 14.5.sp,
                                lineHeight = 25.sp,
                                color = bodyTextColor
                            )
                        }
                    }
                }
            }
        } else if (bodyText.isNotBlank()) {
            val text = Html.fromHtml(html).toString().trim().ifBlank { bodyText }
            Text(
                text = text,
                fontSize = 14.5.sp,
                lineHeight = 25.sp,
                color = bodyTextColor,
                letterSpacing = (-0.1).sp
            )
        }
    }
}

private fun formatTime(pubDateMs: Long): String {
    val diff = System.currentTimeMillis() - pubDateMs
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(pubDateMs))
    }
}
