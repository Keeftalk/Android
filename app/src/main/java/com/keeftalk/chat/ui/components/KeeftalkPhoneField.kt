package com.keeftalk.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeftalk.chat.domain.model.Country
import com.keeftalk.chat.domain.service.PhoneNumberService
import androidx.compose.ui.platform.LocalContext
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.data.service.PhoneNumberServiceImpl

@Composable
fun KeeftalkPhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    selectedCountry: Country,
    onCountryClick: () -> Unit,
    label: String = "Phone Number",
    error: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            error != null -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        label = "BorderColor"
    )

    val elevation by animateDpAsState(
        targetValue = if (isFocused) 6.dp else 0.dp,
        label = "Elevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        label = "Scale"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .onFocusChanged { isFocused = it.isFocused },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(if (isFocused) 2.dp else 1.dp, borderColor),
            shadowElevation = elevation
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCountryClick() }
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedCountry.flagEmoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedCountry.phoneCode,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Enter phone number",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    
                    val context = LocalContext.current
                    val phoneService = remember { PhoneNumberServiceImpl(context) }
                    
                    androidx.compose.foundation.text.BasicTextField(
                        value = value,
                        onValueChange = { newValue ->
                            // Only allow digits and +
                            val filtered = newValue.filter { it.isDigit() || it == '+' }
                            onValueChange(filtered)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        visualTransformation = PhoneVisualTransformation(selectedCountry.isoCode, phoneService),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

/**
 * A professional visual transformation for phone numbers that uses libphonenumber's AsYouTypeFormatter.
 */
class PhoneVisualTransformation(
    private val countryIso: String,
    private val phoneService: PhoneNumberService
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val formatted = phoneService.formatAsYouType(raw, countryIso)
        
        return TransformedText(
            text = AnnotatedString(formatted),
            offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 0) return 0
                    
                    // Simple logic: find how many characters of the formatted string 
                    // correspond to the first [offset] digits of the raw string.
                    var originalCount = 0
                    var transformedIndex = 0
                    
                    while (originalCount < offset && transformedIndex < formatted.length) {
                        val char = formatted[transformedIndex]
                        if (char.isDigit() || char == '+') {
                            originalCount++
                        }
                        transformedIndex++
                    }
                    
                    return transformedIndex
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 0) return 0
                    
                    var originalCount = 0
                    var transformedIndex = 0
                    
                    while (transformedIndex < offset && transformedIndex < formatted.length) {
                        val char = formatted[transformedIndex]
                        if (char.isDigit() || char == '+') {
                            originalCount++
                        }
                        transformedIndex++
                    }
                    
                    return originalCount
                }
            }
        )
    }
}
