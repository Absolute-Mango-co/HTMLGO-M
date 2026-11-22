package com.absolutemango.expressiveui.byok

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun getProviderBrand(providerType: ProviderType): Pair<Color, ImageVector> {
    return when (providerType) {
        ProviderType.GEMINI -> Pair(Color(0xFF4285F4), Icons.Default.AutoAwesome)
        ProviderType.ANTHROPIC -> Pair(Color(0xFFD97706), Icons.Default.Psychology)
        ProviderType.OPENAI -> Pair(Color(0xFF10A37F), Icons.Default.SmartToy)
        ProviderType.GROQ -> Pair(Color(0xFFF97316), Icons.Default.Bolt)
        ProviderType.OPENROUTER -> Pair(Color(0xFF6366F1), Icons.Default.Hub)
        ProviderType.CUSTOM -> Pair(Color(0xFF8B5CF6), Icons.Default.Code)
    }
}

/**
 * Compact model selector dropdown placed in the top-left below the header.
 */
@Composable
fun CompactModelDropdown(
    activeProfile: ProviderProfile?,
    onOpenModelPicker: () -> Unit,
    onSelectModel: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val provider = activeProfile?.providerType ?: ProviderType.GEMINI
    val (brandColor, brandIcon) = getProviderBrand(provider)
    val selectedModel = activeProfile?.selectedModel?.ifBlank { "gemini-3.6-flash" } ?: "gemini-3.6-flash"
    val models = activeProfile?.models.orEmpty().ifEmpty {
        listOf(selectedModel)
    }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)),
            modifier = Modifier.height(34.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = brandIcon,
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = selectedModel.split("/").last(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select model",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            // Header: Provider display
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = brandIcon,
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = brandColor
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Available Models for Active Provider
            models.forEach { modelId ->
                val isSelected = modelId == selectedModel
                DropdownMenuItem(
                    text = {
                        Text(
                            text = modelId.split("/").last(),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Spacer(Modifier.size(18.dp))
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectModel(modelId)
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // More Models (Opens full bottom sheet)
            DropdownMenuItem(
                text = { Text("More Models...", fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    expanded = false
                    onOpenModelPicker()
                }
            )

            // Provider & Keys Settings
            DropdownMenuItem(
                text = { Text("Provider Settings", fontWeight = FontWeight.Medium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    expanded = false
                    onOpenSettings()
                }
            )
        }
    }
}

/**
 * Backward-compatible delegating function for ModelSlideBar.
 */
@Composable
fun ModelSlideBar(
    activeProfile: ProviderProfile?,
    onOpenModelPicker: () -> Unit,
    onSelectModel: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompactModelDropdown(
        activeProfile = activeProfile,
        onOpenModelPicker = onOpenModelPicker,
        onSelectModel = onSelectModel,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}
