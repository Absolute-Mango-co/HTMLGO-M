package com.absolutemango.expressiveui.byok

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlin.math.roundToInt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.UUID

private val ActiveGreen = Color(0xFF22C55E)
private val StandbyGray = Color(0xFF9CA3AF)
private val ErrorRed = Color(0xFFEF4444)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyStorehouseSection(
    repository: ProviderProfileRepository,
    keyStorehouse: SecureKeyStorehouse,
    scraperEngine: ModelScraperEngine,
    modifier: Modifier = Modifier
) {
    val profiles by repository.profiles.collectAsState()
    val activeProfile by repository.activeProfile.collectAsState()
    val isSystemInstructionEnabled by repository.isSystemInstructionEnabled.collectAsState()
    val currentPreset by repository.systemInstructionPreset.collectAsState()
    val customSystemPrompt by repository.customSystemPrompt.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<ProviderProfile?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "BYOK VAULT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Profile", style = MaterialTheme.typography.labelMedium)
                }
            }

            Text(
                text = "Manage multi-provider API keys with hardware-backed Keystore encryption and automatic rate-limit failover.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // System Instruction Intensity & Capability Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Mr. Forger System Instruction",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (customSystemPrompt.isNotBlank()) {
                                    "Active: Custom System Prompt (${customSystemPrompt.length} chars)"
                                } else if (isSystemInstructionEnabled) {
                                    "Active: ${currentPreset.title}"
                                } else {
                                    "Turned off. Raw API behavior preserved."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSystemInstructionEnabled,
                            onCheckedChange = { repository.setSystemInstructionEnabled(it) }
                        )
                    }

                    AnimatedVisibility(visible = isSystemInstructionEnabled) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Intensity Meter",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentPreset.title,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Slider(
                                value = currentPreset.level.toFloat(),
                                onValueChange = { newVal ->
                                    val target = SystemInstructionPreset.fromLevel(newVal.roundToInt())
                                    repository.setSystemInstructionPreset(target)
                                },
                                valueRange = 0f..2f,
                                steps = 1,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Persona",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (currentPreset == SystemInstructionPreset.PERSONA_ONLY) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (currentPreset == SystemInstructionPreset.PERSONA_ONLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "ZIP Only",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (currentPreset == SystemInstructionPreset.ZIP_ONLY) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (currentPreset == SystemInstructionPreset.ZIP_ONLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "ZIP + Strict",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (currentPreset == SystemInstructionPreset.ZIP_STRICT) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (currentPreset == SystemInstructionPreset.ZIP_STRICT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = currentPreset.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = currentPreset.detail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Profiles List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                profiles.forEach { profile ->
                    val isActive = profile.id == activeProfile?.id
                    ProfileCard(
                        profile = profile,
                        isActive = isActive,
                        hasPrimary = !keyStorehouse.getPrimaryKey(profile.id).isNullOrBlank(),
                        hasBackup = !keyStorehouse.getBackupKey(profile.id).isNullOrBlank(),
                        canDelete = profiles.size > 1,
                        onActivate = { repository.setActiveProfile(profile.id) },
                        onEdit = { editingProfile = profile },
                        onDelete = { repository.deleteProfile(profile.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddProfileDialog(
            scraperEngine = scraperEngine,
            onDismiss = { showAddDialog = false },
            onSave = { name, providerType, baseUrl, primaryKey, backupKey, models, selectedModel ->
                val newProfileId = UUID.randomUUID().toString()
                keyStorehouse.storeKeys(newProfileId, primaryKey, backupKey.ifBlank { null })
                val profile = ProviderProfile(
                    id = newProfileId,
                    name = name,
                    providerType = providerType,
                    baseUrl = baseUrl.ifBlank { providerType.defaultBaseUrl },
                    models = models,
                    selectedModel = selectedModel.ifBlank { models.firstOrNull() ?: "" },
                    isActive = true
                )
                repository.saveProfile(profile)
                showAddDialog = false
            }
        )
    }

    editingProfile?.let { profileToEdit ->
        val existingPrimary = keyStorehouse.getPrimaryKey(profileToEdit.id) ?: ""
        val existingBackup = keyStorehouse.getBackupKey(profileToEdit.id) ?: ""
        EditProfileDialog(
            profile = profileToEdit,
            initialPrimaryKey = existingPrimary,
            initialBackupKey = existingBackup,
            scraperEngine = scraperEngine,
            onDismiss = { editingProfile = null },
            onSave = { name, providerType, baseUrl, primaryKey, backupKey, models, selectedModel ->
                keyStorehouse.storeKeys(profileToEdit.id, primaryKey, backupKey.ifBlank { null })
                val updated = profileToEdit.copy(
                    name = name,
                    providerType = providerType,
                    baseUrl = baseUrl.ifBlank { providerType.defaultBaseUrl },
                    models = models,
                    selectedModel = selectedModel.ifBlank { models.firstOrNull() ?: "" }
                )
                repository.saveProfile(updated)
                editingProfile = null
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: ProviderProfile,
    isActive: Boolean,
    hasPrimary: Boolean,
    hasBackup: Boolean,
    canDelete: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderStroke = if (isActive) {
        BorderStroke(2.dp, ActiveGreen)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { if (!isActive) onActivate() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Tripartite Active State: CheckCircle icon + "ACTIVE" badge
                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ActiveGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ActiveGreen)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ActiveGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ACTIVE",
                                    color = ActiveGreen,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "${profile.providerType.displayName} • ${profile.selectedModel.ifBlank { "No model selected" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Key Status Indicator Dots
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val pColor = when {
                        !hasPrimary -> StandbyGray
                        profile.usingBackupKey -> ErrorRed
                        else -> ActiveGreen
                    }
                    val bColor = when {
                        !hasBackup -> StandbyGray
                        profile.usingBackupKey -> ActiveGreen
                        else -> StandbyGray
                    }
                    Surface(shape = CircleShape, color = pColor, modifier = Modifier.size(7.dp)) {}
                    Text(
                        text = if (hasPrimary) "Primary Key" else "Default/No Key",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )

                    if (hasBackup) {
                        Spacer(Modifier.width(4.dp))
                        Surface(shape = CircleShape, color = bColor, modifier = Modifier.size(7.dp)) {}
                        Text(
                            text = if (profile.usingBackupKey) "Backup (Active)" else "Backup (Standby)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Profile",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddProfileDialog(
    scraperEngine: ModelScraperEngine,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        providerType: ProviderType,
        baseUrl: String,
        primaryKey: String,
        backupKey: String,
        models: List<String>,
        selectedModel: String
    ) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(ProviderType.OPENROUTER) }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf(ProviderType.OPENROUTER.defaultBaseUrl ?: "") }
    var primaryKey by remember { mutableStateOf("") }
    var backupKey by remember { mutableStateOf("") }
    var showPrimarySecret by remember { mutableStateOf(false) }
    var showBackupSecret by remember { mutableStateOf(false) }

    val scrapedModels = remember { mutableStateListOf<String>() }
    var selectedModel by remember { mutableStateOf("") }
    var manualModelInput by remember { mutableStateOf("") }
    var isScraping by remember { mutableStateOf(false) }
    var scrapeError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add AI Provider Profile",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. My OpenRouter, Groq Fast") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Provider Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedProvider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = {
                            IconButton(onClick = { providerMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = providerMenuExpanded,
                        onDismissRequest = { providerMenuExpanded = false }
                    ) {
                        ProviderType.values().forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.displayName) },
                                onClick = {
                                    selectedProvider = provider
                                    baseUrl = provider.defaultBaseUrl ?: ""
                                    providerMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Base URL Override
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Primary API Key with prefix heuristic auto-selection
                OutlinedTextField(
                    value = primaryKey,
                    onValueChange = { newKey ->
                        primaryKey = newKey
                        val detected = ModelScraperEngine.detectProvider(newKey)
                        if (detected != null) {
                            selectedProvider = detected
                            baseUrl = detected.defaultBaseUrl ?: ""
                        }
                    },
                    label = { Text("Primary API Key") },
                    singleLine = true,
                    visualTransformation = if (showPrimarySecret) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPrimarySecret = !showPrimarySecret }) {
                            Icon(
                                if (showPrimarySecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Backup API Key
                OutlinedTextField(
                    value = backupKey,
                    onValueChange = { backupKey = it },
                    label = { Text("Backup API Key (Optional)") },
                    singleLine = true,
                    visualTransformation = if (showBackupSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showBackupSecret = !showBackupSecret }) {
                            Icon(
                                if (showBackupSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Explicit Fetch Models Button with Progress Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CATALOG DISCOVERY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            isScraping = true
                            scrapeError = null
                            coroutineScope.launch {
                                try {
                                    val list = scraperEngine.scrapeModels(
                                        providerType = selectedProvider,
                                        apiKey = primaryKey,
                                        baseUrl = baseUrl.ifBlank { null }
                                    )
                                    scrapedModels.clear()
                                    scrapedModels.addAll(list)
                                    if (selectedModel.isBlank() && list.isNotEmpty()) {
                                        selectedModel = list.first()
                                    }
                                } catch (e: Exception) {
                                    scrapeError = e.message ?: "Failed to scrape models"
                                } finally {
                                    isScraping = false
                                }
                            }
                        },
                        enabled = !isScraping
                    ) {
                        if (isScraping) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Fetching...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Fetch Models", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (scrapeError != null) {
                    Text(
                        text = scrapeError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Scraped Models Preview Chips
                if (scrapedModels.isNotEmpty()) {
                    Text(
                        text = "Scraped Models (${scrapedModels.size}) - Click to select default:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        scrapedModels.forEach { m ->
                            val isChosen = m == selectedModel
                            InputChip(
                                selected = isChosen,
                                onClick = { selectedModel = m },
                                label = { Text(m, fontSize = 11.sp) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            scrapedModels.remove(m)
                                            if (selectedModel == m) {
                                                selectedModel = scrapedModels.firstOrNull() ?: ""
                                            }
                                        },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                // Manual Model ID Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualModelInput,
                        onValueChange = { manualModelInput = it },
                        label = { Text("Manual Model ID") },
                        placeholder = { Text("e.g. deepseek/deepseek-r1") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val trimmed = manualModelInput.trim()
                            if (trimmed.isNotEmpty() && !scrapedModels.contains(trimmed)) {
                                scrapedModels.add(0, trimmed)
                                selectedModel = trimmed
                                manualModelInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add custom model")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = name.ifBlank { selectedProvider.displayName }
                    val finalModels = if (scrapedModels.isNotEmpty()) scrapedModels.toList() else listOf(selectedModel).filter { it.isNotBlank() }
                    onSave(
                        finalName,
                        selectedProvider,
                        baseUrl,
                        primaryKey,
                        backupKey,
                        finalModels,
                        selectedModel
                    )
                }
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileDialog(
    profile: ProviderProfile,
    initialPrimaryKey: String,
    initialBackupKey: String,
    scraperEngine: ModelScraperEngine,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        providerType: ProviderType,
        baseUrl: String,
        primaryKey: String,
        backupKey: String,
        models: List<String>,
        selectedModel: String
    ) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(profile.name) }
    var selectedProvider by remember { mutableStateOf(profile.providerType) }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf(profile.baseUrl ?: profile.providerType.defaultBaseUrl ?: "") }
    var primaryKey by remember { mutableStateOf(initialPrimaryKey) }
    var backupKey by remember { mutableStateOf(initialBackupKey) }
    var showPrimarySecret by remember { mutableStateOf(false) }
    var showBackupSecret by remember { mutableStateOf(false) }

    val scrapedModels = remember { mutableStateListOf<String>().apply { addAll(profile.models) } }
    var selectedModel by remember { mutableStateOf(profile.selectedModel) }
    var manualModelInput by remember { mutableStateOf("") }
    var isScraping by remember { mutableStateOf(false) }
    var scrapeError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Vault Profile",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Provider Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedProvider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = {
                            IconButton(onClick = { providerMenuExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = providerMenuExpanded,
                        onDismissRequest = { providerMenuExpanded = false }
                    ) {
                        ProviderType.values().forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.displayName) },
                                onClick = {
                                    selectedProvider = provider
                                    baseUrl = provider.defaultBaseUrl ?: ""
                                    providerMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Base URL Override
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Primary API Key
                OutlinedTextField(
                    value = primaryKey,
                    onValueChange = { newKey ->
                        primaryKey = newKey
                    },
                    label = { Text("Primary API Key") },
                    singleLine = true,
                    visualTransformation = if (showPrimarySecret) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPrimarySecret = !showPrimarySecret }) {
                            Icon(
                                if (showPrimarySecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Backup API Key
                OutlinedTextField(
                    value = backupKey,
                    onValueChange = { backupKey = it },
                    label = { Text("Backup API Key (Optional)") },
                    singleLine = true,
                    visualTransformation = if (showBackupSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showBackupSecret = !showBackupSecret }) {
                            Icon(
                                if (showBackupSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Fetch Models Refresh Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CATALOG DISCOVERY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            isScraping = true
                            scrapeError = null
                            coroutineScope.launch {
                                try {
                                    val list = scraperEngine.scrapeModels(
                                        providerType = selectedProvider,
                                        apiKey = primaryKey,
                                        baseUrl = baseUrl.ifBlank { null }
                                    )
                                    // Append newly fetched models preserving unique entries
                                    list.forEach { m ->
                                        if (!scrapedModels.contains(m)) {
                                            scrapedModels.add(m)
                                        }
                                    }
                                    if (selectedModel.isBlank() && list.isNotEmpty()) {
                                        selectedModel = list.first()
                                    }
                                } catch (e: Exception) {
                                    scrapeError = e.message ?: "Failed to scrape models"
                                } finally {
                                    isScraping = false
                                }
                            }
                        },
                        enabled = !isScraping
                    ) {
                        if (isScraping) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Fetching...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Fetch Models", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (scrapeError != null) {
                    Text(
                        text = scrapeError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Scraped / Configured Models Preview Chips
                if (scrapedModels.isNotEmpty()) {
                    Text(
                        text = "Configured Models (${scrapedModels.size}) - Click to select default:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        scrapedModels.forEach { m ->
                            val isChosen = m == selectedModel
                            InputChip(
                                selected = isChosen,
                                onClick = { selectedModel = m },
                                label = { Text(m, fontSize = 11.sp) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            scrapedModels.remove(m)
                                            if (selectedModel == m) {
                                                selectedModel = scrapedModels.firstOrNull() ?: ""
                                            }
                                        },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                // Manual Model ID Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualModelInput,
                        onValueChange = { manualModelInput = it },
                        label = { Text("Add Model ID Manually") },
                        placeholder = { Text("e.g. deepseek/deepseek-r1") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val trimmed = manualModelInput.trim()
                            if (trimmed.isNotEmpty() && !scrapedModels.contains(trimmed)) {
                                scrapedModels.add(0, trimmed)
                                selectedModel = trimmed
                                manualModelInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add custom model")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = name.ifBlank { profile.name }
                    val finalModels = if (scrapedModels.isNotEmpty()) scrapedModels.toList() else listOf(selectedModel).filter { it.isNotBlank() }
                    onSave(
                        finalName,
                        selectedProvider,
                        baseUrl,
                        primaryKey,
                        backupKey,
                        finalModels,
                        selectedModel
                    )
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
