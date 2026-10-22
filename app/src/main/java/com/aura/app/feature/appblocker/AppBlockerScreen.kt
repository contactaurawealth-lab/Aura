package com.aura.app.feature.appblocker

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.aura.app.R
import com.aura.app.core.database.FocusRuleEntity
import com.aura.app.core.designsystem.*
import com.aura.app.core.permissions.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isRestricted: Boolean
)

@Composable
fun AppBlockerScreen(
    restrictedRules: List<FocusRuleEntity>,
    onToggleRule: (String, String, Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager

    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val hasAccessibility = remember {
        PermissionHelper.isAccessibilityServiceEnabled(context, AppBlockerAccessibilityService::class.java)
    }
    val hasUsageStats = remember {
        PermissionHelper.isUsageStatsGranted(context)
    }

    LaunchedEffect(restrictedRules) {
        withContext(Dispatchers.IO) {
            val launchableApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }
                .map { appInfo ->
                    val name = pm.getApplicationLabel(appInfo).toString()
                    val isRestricted = restrictedRules.any { it.packageName == appInfo.packageName && it.isRestricted }
                    InstalledAppItem(appInfo.packageName, name, isRestricted)
                }
                .sortedBy { it.appName }

            withContext(Dispatchers.Main) {
                installedApps = launchableApps
                isLoading = false
            }
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "APP RESTRICTOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = StoneNeutral
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Restricted Applications",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Permission status banner if missing
            if (!hasAccessibility || !hasUsageStats) {
                AuraCard(
                    backgroundColor = SoftIvory,
                    borderColor = MutedAmber
                ) {
                    Text(
                        text = "System Access Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = MutedAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To gently intercept restricted apps during focus sessions, enable Accessibility and Usage Access in Android Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!hasAccessibility) {
                            AuraOutlinedButton(
                                text = "Enable Accessibility",
                                onClick = { PermissionHelper.openAccessibilitySettings(context) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (!hasUsageStats) {
                            AuraOutlinedButton(
                                text = "Usage Access",
                                onClick = { PermissionHelper.openUsageAccessSettings(context) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Search installed applications...", style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SecondaryText)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = SecondaryText)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SoftIvory,
                    unfocusedContainerColor = SoftIvory,
                    focusedBorderColor = SageBrand,
                    unfocusedBorderColor = BorderDivider
                ),
                singleLine = true
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SageBrand)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AuraCard(
                        onClick = { onToggleRule(app.packageName, app.appName, !app.isRestricted) },
                        borderColor = if (app.isRestricted) SageBrand else BorderDivider
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText
                                )
                            }
                            Switch(
                                checked = app.isRestricted,
                                onCheckedChange = { isChecked ->
                                    onToggleRule(app.packageName, app.appName, isChecked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SoftIvory,
                                    checkedTrackColor = SageBrand
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
