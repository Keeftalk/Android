package com.keeftalk.chat.ui.screens.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.keeftalk.chat.domain.model.MediaItem

@Composable
fun SelectedMediaBar(
    items: List<MediaItem>,
    onRemove: (MediaItem) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(items, key = { it.id }) { item ->
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.LightGray, MaterialTheme.shapes.small)
                ) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    IconButton(
                        onClick = { onRemove(item) },
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
