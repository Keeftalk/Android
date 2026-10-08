package tn.keeftalk.chat.ui.books.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import tn.keeftalk.chat.domain.model.Book
import tn.keeftalk.chat.domain.model.CustomCover

fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = if (clean.length == 6) {
            (0xFF000000 or clean.toLong(16)).toInt()
        } else if (clean.length == 8) {
            clean.toLong(16).toInt()
        } else {
            return fallback
        }
        Color(colorInt)
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    overrideCover: CustomCover? = null,
    isMini: Boolean = false
) {
    val cover = overrideCover ?: book.customCover
    val primaryColor = parseHexColor(cover?.colors?.getOrNull(0), Color(0xFF6C63FF))
    val secondaryColor = parseHexColor(cover?.colors?.getOrNull(1), Color(0xFF2A1A7A))

    val shape = RoundedCornerShape(if (isMini) 3.dp else 6.dp)

    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor)
                )
            )
    ) {
        // Uploaded Cover Image
        if (!cover?.imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = cover?.imageUrl,
                contentDescription = book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Title & Author text for generated cover
            if (!isMini) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isMini) 2.dp else 7.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = book.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        lineHeight = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = book.author,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Spine Effect (left dark border strip)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.08f)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Diagonal Glossy Overlay Effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f)
                        )
                    )
                )
        )

        // Format Badge (PDF/EPUB) in top-right corner
        if (!isMini && cover?.imageUrl.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .background(Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = book.format,
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
