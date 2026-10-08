package tn.keeftalk.chat.ui.feed.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.VerseInfo

@Composable
fun VerseCardExact(
    verse: VerseInfo?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goldColor = Color(0xFFE8C766)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() },
        color = Color.Transparent,
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF131A30), Color(0xFF201A3D), Color(0xFF2C1A3E))
                    )
                )
        ) {
            // Background Islamic Pattern Draw
            Canvas(modifier = Modifier.matchParentSize()) {
                val step = 60.dp.toPx()
                val goldAlpha = goldColor.copy(alpha = 0.14f)
                var x = 0f
                while (x < size.width) {
                    var y = 0f
                    while (y < size.height) {
                        val path = Path().apply {
                            moveTo(x + step / 2f, y)
                            lineTo(x + step, y + step / 2f)
                            lineTo(x + step / 2f, y + step)
                            lineTo(x, y + step / 2f)
                            close()
                        }
                        drawPath(path, goldAlpha, style = Stroke(width = 1.dp.toPx()))
                        y += step
                    }
                    x += step
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 15.dp)
            ) {
                // Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(goldColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Verse icon",
                                tint = goldColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "VERSE OF THE DAY",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp,
                            color = goldColor
                        )
                    }

                    Text(
                        text = verse?.hijriDate ?: "14 Shawwal 1445 AH",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }

                Spacer(modifier = Modifier.height(13.dp))

                // Arabic Text (Amiri / Serif, RTL)
                Text(
                    text = verse?.arabic ?: "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    style = LocalTextStyle.current.copy(
                        textDirection = TextDirection.Rtl,
                        lineHeight = 42.sp
                    )
                )

                // Gold Rule Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, goldColor.copy(alpha = 0.55f), Color.Transparent)
                            )
                        )
                )

                // English Translation
                Text(
                    text = verse?.english ?: "Verily, in the remembrance of Allah do hearts find rest.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.82f),
                    letterSpacing = (-0.1).sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(9.dp))

                // Reference
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(1.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(goldColor.copy(alpha = 0.7f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = verse?.reference ?: "Ar-Ra'd 13:28",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = goldColor,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}
