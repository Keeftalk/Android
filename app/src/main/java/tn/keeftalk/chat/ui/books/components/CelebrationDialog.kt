package tn.keeftalk.chat.ui.books.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ConfettiPiece(
    val x: Float,
    val y: Float,
    val color: Color,
    val size: Float,
    val speed: Float,
    val rotation: Float
)

@Composable
fun CelebrationDialog(
    goalMinutes: Int,
    todayMinutes: Int,
    streakDays: Int,
    monthHours: Float,
    onDismiss: () -> Unit,
    onKeepReading: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6C63FF), Color(0xFF7D5CFF), Color(0xFF00CCCC))
    )

    // Trophy scale pop-in animation
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyScale"
    )

    // Confetti simulation
    val colors = listOf(Color(0xFFFFD43B), Color.White, Color(0xFF00CCCC), Color(0xFFFF5D73), Color(0xFF2ECC71))
    var confetti by remember {
        mutableStateOf(
            List(30) {
                ConfettiPiece(
                    x = Random.nextFloat() * 1000f,
                    y = Random.nextFloat() * -500f,
                    color = colors.random(),
                    size = Random.nextFloat() * 10f + 6f,
                    speed = Random.nextFloat() * 4f + 2f,
                    rotation = Random.nextFloat() * 360f
                )
            }
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30)
            confetti = confetti.map { piece ->
                val newY = piece.y + piece.speed
                if (newY > 2000f) {
                    piece.copy(y = -100f, x = Random.nextFloat() * 1000f)
                } else {
                    piece.copy(y = newY, rotation = piece.rotation + 2f)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Confetti canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            confetti.forEach { piece ->
                drawRect(
                    color = piece.color,
                    topLeft = androidx.compose.ui.geometry.Offset(piece.x * (size.width / 1000f), piece.y * (size.height / 2000f)),
                    size = androidx.compose.ui.geometry.Size(piece.size * 1.4f, piece.size * 0.8f)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🏆",
                fontSize = 84.sp,
                modifier = Modifier
                    .scale(scaleAnim)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Daily goal crushed!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$goalMinutes minutes of reading today. Mission complete.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Three tiles row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    label = "Today",
                    value = "${todayMinutes}m",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Streak",
                    value = "🔥 $streakDays",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "This month",
                    value = "${monthHours}h",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Buttons
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onDismiss() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Awesome",
                        color = Color(0xFF6C63FF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onKeepReading() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "▶ Keep reading",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(72.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
