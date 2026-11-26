package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PlayerSheet
import com.example.ui.navigation.AccountRoute
import com.example.ui.navigation.DiscoverRoute
import com.example.ui.navigation.LibraryRoute
import com.example.ui.navigation.PlaylistDetailRoute
import com.example.ui.navigation.SearchRoute
import com.example.ui.navigation.VideosRoute
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.DiscoverViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.SearchViewModel

private data class TopLevelDestination(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_DESTINATIONS = listOf(
    TopLevelDestination(DiscoverRoute, R.string.nav_discover, Icons.Rounded.Explore, Icons.Outlined.Explore, "nav_discover"),
    TopLevelDestination(VideosRoute, R.string.nav_video, Icons.Rounded.SmartDisplay, Icons.Outlined.SmartDisplay, "nav_videos"),
    TopLevelDestination(LibraryRoute, R.string.nav_library, Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_library"),
    TopLevelDestination(AccountRoute, R.string.nav_account, Icons.Rounded.Person, Icons.Outlined.Person, "nav_account")
)

@Composable
fun AriaApp(
    widthSizeClass: WindowWidthSizeClass,
    playerViewModel: PlayerViewModel,
    discoverViewModel: DiscoverViewModel,
    searchViewModel: SearchViewModel,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val playerInstance by playerViewModel.playerFlow.collectAsStateWithLifecycle()
    val favorites by playerViewModel.favoritesFlow.collectAsStateWithLifecycle()
    val localMedia by playerViewModel.localMediaFlow.collectAsStateWithLifecycle()
    val playHistory by playerViewModel.playHistoryFlow.collectAsStateWithLifecycle()

    val discoverState by discoverViewModel.uiState.collectAsStateWithLifecycle()
    val playlistDetailState by discoverViewModel.playlistDetailState.collectAsStateWithLifecycle()

    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val searchHistory by searchViewModel.searchHistoryFlow.collectAsStateWithLifecycle()

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showStreamDialog by remember { mutableStateOf(false) }
    var streamDialogDefaultVideo by remember { mutableStateOf(false) }
    var notificationRequested by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(playerState.playback.isPlaying) {
        if (playerState.playback.isPlaying && !notificationRequested && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationRequested = true
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(playerState.extra.snackbarMessage) {
        val msg = playerState.extra.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            playerViewModel.consumeSnackbar()
        }
    }

    LaunchedEffect(playerState.playback.errorMessage) {
        val err = playerState.playback.errorMessage
        if (err != null) {
            snackbarHostState.showSnackbar(err)
            playerViewModel.clearPlaybackError()
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            playerViewModel.importLocalUri(uri, forceVideo = false)
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            playerViewModel.importLocalUri(uri, forceVideo = true)
        }
    }

    val useRail = widthSizeClass != WindowWidthSizeClass.Compact
    val showTopLevelNav = currentDestination?.let { dest ->
        TOP_DESTINATIONS.any { dest.hasRoute(it.route::class) }
    } ?: true

    val hasActiveTrack = playerState.playback.currentTrack != null
    val miniPlayerHeightPadding = if (hasActiveTrack) 84.dp else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && showTopLevelNav) {
                NavigationRail(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.weight(0.2f))
                    TOP_DESTINATIONS.forEach { item ->
                        val selected = currentDestination?.hasRoute(item.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!useRail && showTopLevelNav) {
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            TOP_DESTINATIONS.forEach { item ->
                                val selected = currentDestination?.hasRoute(item.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .statusBarsPadding()
                ) {
                    val contentBottomPadding = PaddingValues(bottom = miniPlayerHeightPadding)

                    NavHost(
                        navController = navController,
                        startDestination = DiscoverRoute,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<DiscoverRoute> {
                            DiscoverScreen(
                                uiState = discoverState,
                                currentTrackId = playerState.playback.currentTrack?.id,
                                onRefresh = { discoverViewModel.refreshAll() },
                                onOpenSearch = { navController.navigate(SearchRoute) },
                                onOpenAccount = {
                                    navController.navigate(AccountRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onOpenPlaylist = { playlist ->
                                    navController.navigate(
                                        PlaylistDetailRoute(
                                            playlistId = playlist.id,
                                            title = playlist.name,
                                            coverUrl = playlist.coverUrl
                                        )
                                    )
                                },
                                onPlayTrack = { track, queue ->
                                    playerViewModel.playTrack(track, queue)
                                },
                                onPlayMv = { mv ->
                                    playerViewModel.playMv(mv.id, mv.title, mv.artistName, mv.coverUrl, openFull = true)
                                },
                                onPickLocalAudio = {
                                    openDocumentLauncher.launch(arrayOf("audio/*", "video/*"))
                                },
                                onOpenStreamUrlDialog = {
                                    streamDialogDefaultVideo = false
                                    showStreamDialog = true
                                },
                                bottomContentPadding = contentBottomPadding
                            )
                        }

                        composable<VideosRoute> {
                            VideosScreen(
                                featuredMvs = discoverState.featuredMvs,
                                localVideos = localMedia.filter { it.isVideo },
                                isLoading = discoverState.isLoading,
                                isServerConfigured = discoverState.settings.isServerConfigured,
                                currentTrackId = playerState.playback.currentTrack?.id,
                                onPlayMv = { mv ->
                                    playerViewModel.playMv(mv.id, mv.title, mv.artistName, mv.coverUrl, openFull = true)
                                },
                                onPlayLocalVideo = { video ->
                                    playerViewModel.playTrack(video, listOf(video), openFull = true)
                                },
                                onPickLocalVideo = {
                                    pickVideoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                onOpenVideoUrlDialog = {
                                    streamDialogDefaultVideo = true
                                    showStreamDialog = true
                                },
                                onOpenAccount = {
                                    navController.navigate(AccountRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                bottomContentPadding = contentBottomPadding
                            )
                        }

                        composable<LibraryRoute> {
                            LibraryScreen(
                                favorites = favorites,
                                localMedia = localMedia,
                                history = playHistory,
                                userPlaylists = authState.userPlaylists,
                                currentTrackId = playerState.playback.currentTrack?.id,
                                onPlayTrack = { track, queue ->
                                    playerViewModel.playTrack(track, queue, openFull = track.isVideo)
                                },
                                onPlayMv = { mv ->
                                    playerViewModel.playMv(mv.id, mv.title, mv.artistName, mv.coverUrl, openFull = true)
                                },
                                onOpenPlaylist = { playlist ->
                                    navController.navigate(
                                        PlaylistDetailRoute(
                                            playlistId = playlist.id,
                                            title = playlist.name,
                                            coverUrl = playlist.coverUrl
                                        )
                                    )
                                },
                                onPickLocalAudio = {
                                    openDocumentLauncher.launch(arrayOf("audio/*", "video/*"))
                                },
                                onPickLocalVideo = {
                                    pickVideoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                onOpenStreamUrlDialog = {
                                    streamDialogDefaultVideo = false
                                    showStreamDialog = true
                                },
                                onClearHistory = { playerViewModel.clearHistory() },
                                bottomContentPadding = contentBottomPadding
                            )
                        }

                        composable<AccountRoute> {
                            AccountScreen(
                                uiState = authState,
                                onServerUrlChange = authViewModel::updateServerUrlInput,
                                onSaveAndTestServer = authViewModel::saveAndTestServerUrl,
                                onSelectLoginTab = authViewModel::selectLoginTab,
                                onCountryCodeChange = authViewModel::updateCountryCode,
                                onPhoneChange = authViewModel::updatePhoneInput,
                                onCaptchaChange = authViewModel::updateCaptchaInput,
                                onSendCaptcha = authViewModel::sendSmsCaptcha,
                                onLoginWithSms = authViewModel::loginWithSms,
                                onStartQrLogin = authViewModel::startQrLogin,
                                onCookieChange = authViewModel::updateCookieInput,
                                onLoginWithCookie = authViewModel::loginWithCookie,
                                onLogout = authViewModel::logout,
                                onSelectQuality = authViewModel::setAudioQuality,
                                onLyricScaleChange = authViewModel::setLyricFontScale,
                                onShowTranslatedChange = authViewModel::setShowTranslatedLyrics,
                                onSelectThemeMode = authViewModel::setThemeMode,
                                bottomContentPadding = contentBottomPadding
                            )
                        }

                        composable<SearchRoute> {
                            SearchScreen(
                                uiState = searchState,
                                searchHistory = searchHistory,
                                currentTrackId = playerState.playback.currentTrack?.id,
                                onBack = { navController.popBackStack() },
                                onQueryChange = searchViewModel::updateQuery,
                                onSubmitSearch = searchViewModel::submitSearch,
                                onSelectCategory = searchViewModel::selectCategory,
                                onClearHistory = searchViewModel::clearHistory,
                                onPlayTrack = { track, queue ->
                                    playerViewModel.playTrack(track, queue)
                                },
                                onOpenPlaylist = { playlist ->
                                    navController.navigate(
                                        PlaylistDetailRoute(
                                            playlistId = playlist.id,
                                            title = playlist.name,
                                            coverUrl = playlist.coverUrl
                                        )
                                    )
                                },
                                onPlayMv = { mv ->
                                    playerViewModel.playMv(mv.id, mv.title, mv.artistName, mv.coverUrl, openFull = true)
                                },
                                bottomContentPadding = contentBottomPadding
                            )
                        }

                        composable<PlaylistDetailRoute> { entry ->
                            val args = entry.toRoute<PlaylistDetailRoute>()
                            LaunchedEffect(args.playlistId) {
                                discoverViewModel.loadPlaylistDetail(args.playlistId)
                            }
                            PlaylistDetailScreen(
                                fallbackTitle = args.title,
                                fallbackCover = args.coverUrl,
                                uiState = playlistDetailState,
                                currentTrackId = playerState.playback.currentTrack?.id,
                                onBack = { navController.popBackStack() },
                                onRetry = { discoverViewModel.loadPlaylistDetail(args.playlistId) },
                                onPlayTrack = { track, queue ->
                                    playerViewModel.playTrack(track, queue)
                                },
                                onPlayMv = { mv ->
                                    playerViewModel.playMv(mv.id, mv.title, mv.artistName, mv.coverUrl, openFull = true)
                                },
                                bottomPadding = contentBottomPadding
                            )
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = hasActiveTrack && !playerState.extra.isFullPlayerOpen,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .widthIn(max = 560.dp)
                    ) {
                        MiniPlayerBar(
                            uiState = playerState,
                            onOpenFullPlayer = { playerViewModel.openFullPlayer() },
                            onTogglePlayPause = { playerViewModel.togglePlayPause() },
                            onSkipNext = { playerViewModel.skipToNext() }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = playerState.extra.isFullPlayerOpen && hasActiveTrack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerSheet(
                uiState = playerState,
                player = playerInstance,
                onClose = { playerViewModel.closeFullPlayer() },
                onSelectTab = { playerViewModel.selectPlayerTab(it) },
                onTogglePlayPause = { playerViewModel.togglePlayPause() },
                onSeekTo = { playerViewModel.seekTo(it) },
                onSkipNext = { playerViewModel.skipToNext() },
                onSkipPrevious = { playerViewModel.skipToPrevious() },
                onSkipToQueueIndex = { playerViewModel.skipToQueueIndex(it) },
                onCycleRepeat = { playerViewModel.cycleRepeatMode() },
                onToggleShuffle = { playerViewModel.toggleShuffle() },
                onToggleFavorite = { playerViewModel.toggleFavorite(it) },
                onSwitchAudioMv = { playerViewModel.switchBetweenAudioAndMv() },
                onToggleVideoFullscreen = { playerViewModel.toggleVideoFullscreen() }
            )
        }
    }

    if (showStreamDialog) {
        StreamUrlDialog(
            initialIsVideo = streamDialogDefaultVideo,
            onDismiss = { showStreamDialog = false },
            onConfirm = { url, title, isVideo ->
                showStreamDialog = false
                playerViewModel.playDirectStreamUrl(url, title, isVideo)
            }
        )
    }
}

@Composable
private fun StreamUrlDialog(
    initialIsVideo: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (url: String, title: String, isVideo: Boolean) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var isVideo by remember(initialIsVideo) { mutableStateOf(initialIsVideo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("播放网络直链") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isVideo,
                        onClick = { isVideo = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("音频流") }
                    SegmentedButton(
                        selected = isVideo,
                        onClick = { isVideo = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("视频流") }
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("媒体直链（MP3 / FLAC / MP4 / M3U8）") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_url_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("显示名称（可选）") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(url, title, isVideo) },
                enabled = url.isNotBlank(),
                modifier = Modifier.testTag("stream_url_play_button")
            ) {
                Text("立即播放")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
