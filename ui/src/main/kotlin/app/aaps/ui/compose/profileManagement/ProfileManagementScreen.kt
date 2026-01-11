package app.aaps.ui.compose.profileManagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.aaps.core.data.model.EPS
import app.aaps.core.data.time.T
import app.aaps.core.interfaces.profile.ProfileSource
import app.aaps.core.ui.compose.AapsTheme
import app.aaps.core.ui.compose.OkCancelDialog
import app.aaps.core.ui.compose.icons.ProfileSwitch
import app.aaps.ui.R
import app.aaps.ui.compose.components.ContentContainer
import app.aaps.ui.compose.profileManagement.viewmodels.ProfileManagementViewModel
import kotlin.math.absoluteValue

/**
 * Screen for managing local profiles.
 * Displays profiles in a carousel with profile viewer below and action buttons.
 *
 * @param viewModel ViewModel managing profile state and operations
 * @param onNavigateBack Callback to navigate back
 * @param onEditProfile Callback when user wants to edit a profile (receives profile index)
 * @param onActivateProfile Callback when user wants to activate a profile (receives profile index)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileManagementScreen(
    viewModel: ProfileManagementViewModel,
    onNavigateBack: () -> Unit = {},
    onEditProfile: (Int) -> Unit = {},
    onActivateProfile: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Dialog states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<Int?>(null) }
    var showCloneDialog by remember { mutableStateOf(false) }
    var profileToClone by remember { mutableStateOf<Int?>(null) }

    // Delete confirmation dialog
    if (showDeleteDialog && profileToDelete != null) {
        val profileName = uiState.profiles.getOrNull(profileToDelete!!)?.name ?: ""
        OkCancelDialog(
            title = viewModel.rh.gs(app.aaps.core.ui.R.string.removerecord),
            message = viewModel.rh.gs(R.string.confirm_remove_profile, profileName),
            onConfirm = {
                profileToDelete?.let { viewModel.removeProfile(it) }
                showDeleteDialog = false
                profileToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                profileToDelete = null
            }
        )
    }

    // Clone confirmation dialog
    if (showCloneDialog && profileToClone != null) {
        val profileName = uiState.profiles.getOrNull(profileToClone!!)?.name ?: ""
        OkCancelDialog(
            title = viewModel.rh.gs(R.string.clone_label),
            message = viewModel.rh.gs(R.string.confirm_clone_profile, profileName),
            onConfirm = {
                profileToClone?.let { viewModel.cloneProfile(it) }
                showCloneDialog = false
                profileToClone = null
            },
            onDismiss = {
                showCloneDialog = false
                profileToClone = null
            }
        )
    }

    // Track current page for floating toolbar actions
    var currentPage by remember { mutableStateOf(uiState.currentProfileIndex) }

    AapsTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(app.aaps.core.ui.R.string.profile_management)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(app.aaps.core.ui.R.string.back)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ContentContainer(
                    isLoading = uiState.isLoading,
                    isEmpty = uiState.profiles.isEmpty()
                ) {
                    // Calculate initial page based on active profile - store stable value once computed
                    var stableInitialPage by remember { mutableIntStateOf(-1) }
                    if (stableInitialPage == -1 && uiState.activeProfileName != null && uiState.profiles.isNotEmpty()) {
                        val index = uiState.profiles.indexOfFirst { it.name == uiState.activeProfileName }
                        stableInitialPage = if (index >= 0) index else 0
                    }
                    val initialPage = if (stableInitialPage >= 0) stableInitialPage else 0

                    // Use key to force pager recreation when we first get valid initial page
                    key(stableInitialPage) {
                        val pagerState = rememberPagerState(
                            initialPage = initialPage,
                            pageCount = { uiState.profiles.size }
                        )

                        // Sync pager with selected profile (for programmatic selection)
                        LaunchedEffect(uiState.currentProfileIndex) {
                            if (pagerState.currentPage != uiState.currentProfileIndex) {
                                pagerState.animateScrollToPage(uiState.currentProfileIndex)
                            }
                        }

                    // Update selected profile when pager changes
                    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                        if (!pagerState.isScrollInProgress && pagerState.currentPage != uiState.currentProfileIndex) {
                            viewModel.selectProfile(pagerState.currentPage)
                        }
                        currentPage = pagerState.currentPage
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Profile Carousel
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentPadding = PaddingValues(horizontal = 64.dp),
                            pageSpacing = 16.dp
                        ) { page ->
                            val profile = uiState.profiles.getOrNull(page)
                            val basalSum = uiState.basalSums.getOrNull(page) ?: 0.0
                            val isActive = profile?.name == uiState.activeProfileName
                            val hasErrors = uiState.profileErrors.getOrNull(page)?.isNotEmpty() == true

                            ProfileCarouselCard(
                                profile = profile,
                                basalSum = basalSum,
                                isActive = isActive,
                                hasErrors = hasErrors,
                                activeProfileSwitch = if (isActive) uiState.activeProfileSwitch else null,
                                nextProfileName = if (isActive) uiState.nextProfileName else null,
                                formatBasalSum = viewModel::formatBasalSum,
                                modifier = Modifier
                                    .graphicsLayer {
                                        val pageOffset = (
                                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                            ).absoluteValue
                                        // Scale effect for carousel
                                        lerp(
                                            start = 0.85f,
                                            stop = 1f,
                                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                        ).also { scale ->
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        // Alpha effect
                                        alpha = lerp(
                                            start = 0.5f,
                                            stop = 1f,
                                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                        )
                                    }
                            )
                        }

                        // Page indicator dots
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(uiState.profiles.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .width(if (isSelected) 24.dp else 8.dp)
                                        .height(8.dp)
                                        .graphicsLayer {
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            clip = true
                                        }
                                        .then(
                                            Modifier.padding(0.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        drawRoundRect(
                                            color = if (isSelected)
                                                Color(0xFF6200EE)
                                            else
                                                Color(0xFFBDBDBD),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }

                        // Profile Viewer
                        uiState.selectedProfile?.let { profile ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                ProfileSingleContent(
                                    profile = profile,
                                    getIcList = viewModel::getIcList,
                                    getIsfList = viewModel::getIsfList,
                                    getBasalList = viewModel::getBasalList,
                                    getTargetList = viewModel::getTargetList,
                                    formatDia = viewModel::formatDia,
                                    formatBasalSum = viewModel::formatBasalSum
                                )
                                // Extra space for floating toolbar
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                    } // end key
                }

                // Floating Toolbar with FAB (M3 style)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Floating Toolbar (M3 specs: pill shape, elevation, surfaceContainerHigh)
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 6.dp,
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.addNewProfile() }) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.add_new_profile)
                                )
                            }
                            IconButton(onClick = { onEditProfile(currentPage) }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.edit_label)
                                )
                            }
                            IconButton(onClick = {
                                profileToClone = currentPage
                                showCloneDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.clone_label)
                                )
                            }
                            IconButton(
                                onClick = {
                                    profileToDelete = currentPage
                                    showDeleteDialog = true
                                },
                                enabled = uiState.profiles.size > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.remove_label),
                                    tint = if (uiState.profiles.size > 1)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }

                    // FAB for primary action (Activate)
                    FloatingActionButton(
                        onClick = { onActivateProfile(currentPage) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.activate_label),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

            }
        }
    }
}

/**
 * Card displayed in the carousel for a single profile.
 */
@Composable
private fun ProfileCarouselCard(
    profile: ProfileSource.SingleProfile?,
    basalSum: Double,
    isActive: Boolean,
    hasErrors: Boolean,
    activeProfileSwitch: EPS?,
    nextProfileName: String?,
    formatBasalSum: (Double) -> String,
    modifier: Modifier = Modifier
) {
    // Check if active profile has modifications (percentage, timeshift, or duration)
    val isModified = activeProfileSwitch?.let { eps ->
        eps.originalPercentage != 100 || eps.originalTimeshift != 0L || eps.originalDuration != 0L
    } ?: false

    // Calculate progress for temporary profiles with auto-update
    val hasDuration = activeProfileSwitch?.originalDuration?.let { it > 0L } ?: false
    var progress by remember { mutableFloatStateOf(0f) }

    // Auto-update progress every 30 seconds
    LaunchedEffect(isActive, hasDuration, activeProfileSwitch?.timestamp) {
        if (isActive && hasDuration) {
            val eps = activeProfileSwitch!!
            while (true) {
                val now = System.currentTimeMillis()
                val elapsed = now - eps.timestamp
                val remaining = eps.originalDuration - elapsed
                progress = if (remaining > 0) {
                    (elapsed.toFloat() / eps.originalDuration.toFloat()).coerceIn(0f, 1f)
                } else 1f
                delay(30_000L) // Update every 30 seconds
            }
        } else {
            progress = 0f
        }
    }

    val containerColor = when {
        hasErrors -> MaterialTheme.colorScheme.errorContainer
        isActive && isModified -> AapsTheme.generalColors.inProgress
        isActive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        hasErrors -> MaterialTheme.colorScheme.onErrorContainer
        isActive && isModified -> AapsTheme.generalColors.onInProgress
        isActive -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 8.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Profile icon in top left corner
            Icon(
                imageVector = ProfileSwitch,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(24.dp),
                tint = contentColor.copy(alpha = 0.6f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Profile name
                Text(
                    text = profile?.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Basal sum
                Text(
                    text = "∑ ${formatBasalSum(basalSum)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )

                // Error indicator
                if (hasErrors) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(app.aaps.core.ui.R.string.invalid),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active indicator and percentage/timeshift
                if (isActive && !hasErrors) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Show "ACTIVE -> Next: profile" when there's a next profile, otherwise just "ACTIVE"
                    val activeText = if (hasDuration && nextProfileName != null) {
                        "${stringResource(R.string.active_profile_indicator)} → $nextProfileName"
                    } else {
                        stringResource(R.string.active_profile_indicator)
                    }
                    Text(
                        text = activeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isModified) contentColor else AapsTheme.generalColors.activeInsulinText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Percentage and timeshift for active profile
                    activeProfileSwitch?.let { eps ->
                        val details = buildString {
                            if (eps.originalPercentage != 100) {
                                append("${eps.originalPercentage}%")
                            }
                            val timeshiftHours = T.msecs(eps.originalTimeshift).hours().toInt()
                            if (timeshiftHours != 0) {
                                if (isNotEmpty()) append(" ")
                                append(if (timeshiftHours > 0) "+${timeshiftHours}h" else "${timeshiftHours}h")
                            }
                        }
                        if (details.isNotEmpty()) {
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor
                            )
                        }
                    }
                }
            }

            // Progress indicator for temporary profiles at bottom
            if (isActive && hasDuration && progress < 1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .height(4.dp),
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}
