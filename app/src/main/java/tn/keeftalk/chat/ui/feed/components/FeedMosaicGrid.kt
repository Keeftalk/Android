package tn.keeftalk.chat.ui.feed.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import tn.keeftalk.chat.domain.model.FeedArticle
import java.text.SimpleDateFormat
import java.util.*

sealed class MosaicRowItem {
    data class Single(val article: FeedArticle, val type: ArticleDisplayType) : MosaicRowItem()
    data class Pair(val left: FeedArticle, val right: FeedArticle) : MosaicRowItem()
}

enum class ArticleDisplayType { LIST, BIG }

fun buildMosaicRows(articles: List<FeedArticle>): List<MosaicRowItem> {
    val rows = mutableListOf<MosaicRowItem>()
    var i = 0
    while (i < articles.size) {
        val remaining = articles.size - i
        val patternChoice = (i % 5)
        val currentArticle = articles[i]
        when {
            remaining == 1 || patternChoice == 0 || patternChoice == 3 -> {
                rows.add(MosaicRowItem.Single(currentArticle, ArticleDisplayType.LIST))
                i++
            }
            patternChoice == 1 -> {
                rows.add(MosaicRowItem.Single(currentArticle, ArticleDisplayType.BIG))
                i++
            }
            else -> {
                val hasSecond = (i + 1 < articles.size)
                if (hasSecond) {
                    val nextArticle = articles[i + 1]
                    rows.add(MosaicRowItem.Pair(currentArticle, nextArticle))
                    i += 2
                } else {
                    rows.add(MosaicRowItem.Single(currentArticle, ArticleDisplayType.LIST))
                    i++
                }
            }
        }
    }
    return rows
}

@Composable
fun LikedBannerView(sourceName: String, count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        color = Color.Unspecified
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color(0xFFFFE4E9), Color(0xFFFCE7F3)))
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFEC4899)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Heart",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }

            Text(
                text = "More from $sourceName — because you liked $count ${if (count == 1) "story" else "stories"}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFBE185D)
            )
        }
    }
}

@Composable
fun FeedMosaicGrid(
    articles: List<FeedArticle>,
    likedSourcesMap: Map<String, Int>,
    isLoading: Boolean,
    onSelectArticle: (FeedArticle) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (isLoading && articles.isEmpty()) {
            FeedMosaicSkeleton()
        }
    }
}

fun isRtlText(text: String?): Boolean {
    if (text.isNullOrBlank()) return false
    return text.any { char ->
        val block = Character.UnicodeBlock.of(char)
        block == Character.UnicodeBlock.ARABIC ||
        block == Character.UnicodeBlock.ARABIC_SUPPLEMENT ||
        block == Character.UnicodeBlock.ARABIC_EXTENDED_A ||
        block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
        block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B ||
        block == Character.UnicodeBlock.HEBREW
    }
}

@Composable
fun FeedItemList(
    article: FeedArticle,
    isLiked: Boolean,
    onClick: () -> Unit
) {
    val isRtl = remember(article.title, article.description) { isRtlText(article.title) || isRtlText(article.description) }
    val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onClick() },
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArticleImageSlideshow(
                        article = article,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${article.sourceName ?: "Feed"} · ${formatTimeAgo(article.pubDate)}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                letterSpacing = 0.4.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = article.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.25).sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }
                }

                if (isLiked) {
                    HeartBadge(modifier = Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }
}

@Composable
fun FeedItemBig(
    article: FeedArticle,
    isLiked: Boolean,
    onClick: () -> Unit
) {
    val isRtl = remember(article.title, article.description) { isRtlText(article.title) || isRtlText(article.description) }
    val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onClick() },
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ArticleImageSlideshow(
                    article = article,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0A0C19).copy(alpha = 0.15f),
                                    Color(0xFF0A0C19).copy(alpha = 0.05f),
                                    Color(0xFF0A0C19).copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0A0C19).copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = (article.sourceName ?: "FEATURED").uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                }

                if (isLiked) {
                    HeartBadge(
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.TopEnd)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = formatTimeAgo(article.pubDate).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.82f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 21.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun FeedItemSquare(
    article: FeedArticle,
    isLiked: Boolean,
    onClick: () -> Unit
) {
    val isRtl = remember(article.title, article.description) { isRtlText(article.title) || isRtlText(article.description) }
    val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onClick() },
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ArticleImageSlideshow(
                    article = article,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0A0C19).copy(alpha = 0.1f),
                                    Color(0xFF0A0C19).copy(alpha = 0.05f),
                                    Color(0xFF0A0C19).copy(alpha = 0.9f)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0A0C19).copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = (article.sourceName ?: "FEED").uppercase().take(12),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                        color = Color.White
                    )
                }

                if (isLiked) {
                    HeartBadge(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = formatTimeAgo(article.pubDate).uppercase(),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.78f),
                        letterSpacing = 0.4.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = article.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.2).sp,
                        lineHeight = 15.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HeartBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFEC4899)))
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Liked",
            tint = Color.White,
            modifier = Modifier.size(10.dp)
        )
    }
}

@Composable
fun FeedMosaicSkeleton() {
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by shimmerTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE9EBF2).copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE9EBF2).copy(alpha = alpha * 0.8f))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(145.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE9EBF2).copy(alpha = alpha))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(145.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE9EBF2).copy(alpha = alpha * 0.7f))
            )
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
