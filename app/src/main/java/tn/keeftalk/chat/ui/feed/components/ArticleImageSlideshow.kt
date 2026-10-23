package tn.keeftalk.chat.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import tn.keeftalk.chat.domain.model.FeedArticle

fun extractArticleImages(article: FeedArticle): List<String> {
    val list = mutableListOf<String>()
    article.thumbnailUrl?.let { if (it.isNotBlank()) list.add(it) }
    val doc = Jsoup.parse("${article.content ?: ""} ${article.description ?: ""}")
    doc.select("img").forEach { img ->
        val src = img.attr("src")
        if (src.isNotBlank() && src.startsWith("http") && !list.contains(src)) {
            list.add(src)
        }
    }
    return list
}

@Composable
fun ArticleImageSlideshow(
    article: FeedArticle,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val images = remember(article) { extractArticleImages(article) }
    val pagerState = rememberPagerState(pageCount = { images.size })

    if (images.size > 1) {
        LaunchedEffect(images) {
            while (true) {
                delay(4000)
                val nextPage = (pagerState.currentPage + 1) % images.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(modifier = modifier) {
        if (images.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
        } else if (images.size == 1) {
            AsyncImage(
                model = images[0],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = images[page],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }

            // Indicator dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(images.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 6.dp else 4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
