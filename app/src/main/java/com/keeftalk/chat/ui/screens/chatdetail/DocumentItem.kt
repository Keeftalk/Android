package com.keeftalk.chat.ui.screens.chatdetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.domain.model.DocumentModel
import com.keeftalk.chat.domain.model.DocumentType

@Composable
fun DocumentItem(
    document: DocumentModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 0.98f else 1f, label = "scale")
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        label = "borderColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp, 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(getDocumentIconColor(document.type).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getDocumentIcon(document.type),
                contentDescription = null,
                tint = getDocumentIconColor(document.type),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = document.size,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = " • ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    fontSize = 12.sp
                )
                Text(
                    text = document.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(
                    2.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    CircleShape
                )
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun getDocumentIcon(type: DocumentType): ImageVector {
    return when (type) {
        DocumentType.PDF -> Icons.Default.PictureAsPdf
        DocumentType.WORD -> Icons.Default.Description
        DocumentType.EXCEL -> Icons.Default.TableChart
        DocumentType.PPT -> Icons.Default.Slideshow
        DocumentType.ZIP, DocumentType.ARCHIVE -> Icons.Default.FolderZip
        DocumentType.CODE -> Icons.Default.Code
        DocumentType.EBOOK -> Icons.Default.Book
        DocumentType.SECURITY -> Icons.Default.Https
        DocumentType.LOCATION -> Icons.Default.Place
        DocumentType.CAD -> Icons.Default.Architecture
        DocumentType.OTHER -> Icons.Default.Description
    }
}

fun getDocumentIconColor(type: DocumentType): Color {
    return when (type) {
        DocumentType.PDF -> Color(0xFFF87171)
        DocumentType.WORD -> Color(0xFF60A5FA)
        DocumentType.EXCEL -> Color(0xFF4ADE80)
        DocumentType.PPT -> Color(0xFFFBBF24)
        DocumentType.ZIP, DocumentType.ARCHIVE -> Color(0xFF9CA3AF)
        DocumentType.CODE -> Color(0xFFA78BFA)
        DocumentType.EBOOK -> Color(0xFFD2B48C)
        DocumentType.SECURITY -> Color(0xFF34D399)
        DocumentType.LOCATION -> Color(0xFFF43F5E)
        DocumentType.CAD -> Color(0xFFB0C4DE)
        DocumentType.OTHER -> Color(0xFF94A3B8)
    }
}
