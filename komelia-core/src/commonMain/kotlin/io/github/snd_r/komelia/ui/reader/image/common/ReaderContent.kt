package io.github.snd_r.komelia.ui.reader.image.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import io.github.snd_r.komelia.platform.PlatformType.MOBILE
import io.github.snd_r.komelia.ui.LocalPlatform
import io.github.snd_r.komelia.ui.LocalWindowState
import io.github.snd_r.komelia.ui.common.LoadingMaxSizeIndicator
import io.github.snd_r.komelia.ui.reader.image.ReaderState
import io.github.snd_r.komelia.ui.reader.image.ReaderType.CONTINUOUS
import io.github.snd_r.komelia.ui.reader.image.ReaderType.PAGED
import io.github.snd_r.komelia.ui.reader.image.ReaderType.PANELS
import io.github.snd_r.komelia.ui.reader.image.ScreenScaleState
import io.github.snd_r.komelia.ui.reader.image.continuous.ContinuousReaderContent
import io.github.snd_r.komelia.ui.reader.image.continuous.ContinuousReaderState
import io.github.snd_r.komelia.ui.reader.image.paged.PagedReaderContent
import io.github.snd_r.komelia.ui.reader.image.paged.PagedReaderState
import io.github.snd_r.komelia.ui.reader.image.panels.PanelsReaderContent
import io.github.snd_r.komelia.ui.reader.image.panels.PanelsReaderState
import io.github.snd_r.komelia.ui.reader.image.settings.SettingsOverlay
import io.github.snd_r.komelia.ui.settings.imagereader.onnxruntime.OnnxRuntimeSettingsState
import kotlinx.coroutines.launch

@Composable
fun ReaderContent(
    commonReaderState: ReaderState,
    pagedReaderState: PagedReaderState,
    continuousReaderState: ContinuousReaderState,
    panelsReaderState: PanelsReaderState?,
    onnxRuntimeSettingsState: OnnxRuntimeSettingsState?,
    screenScaleState: ScreenScaleState,

    isColorCorrectionActive: Boolean,
    onColorCorrectionClick: () -> Unit,
    onExit: () -> Unit,
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    if (LocalPlatform.current == MOBILE) {
        val windowState = LocalWindowState.current
        DisposableEffect(showSettingsMenu) {
            if (showSettingsMenu) {
                windowState.setFullscreen(false)
            } else {
                windowState.setFullscreen(true)
            }
            onDispose {
                windowState.setFullscreen(false)
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        screenScaleState.composeScope = coroutineScope
    }

    val topLevelFocus = remember { FocusRequester() }
    val volumeKeysNavigation = commonReaderState.volumeKeysNavigation.collectAsState().value
    val swipeGesturesEnabled = commonReaderState.swipeGesturesEnabled.collectAsState().value
    var hasFocus by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged {
                screenScaleState.setAreaSize(it)
            }
            .focusable()
            .focusRequester(topLevelFocus)
            .onFocusChanged { hasFocus = it.hasFocus }
            .onKeyEvent { event ->
                if (event.type != KeyUp) return@onKeyEvent false

                var consumed = true
                when (event.key) {
                    Key.M -> showSettingsMenu = !showSettingsMenu
                    Key.Escape -> showSettingsMenu = false
                    Key.H -> showHelpDialog = true
                    Key.DirectionLeft -> if (event.isAltPressed) onExit() else consumed = false
                    Key.Back -> if (showSettingsMenu) showSettingsMenu = false else onExit()
                    Key.U -> commonReaderState.onStretchToFitCycle()
                    Key.C -> if (event.isAltPressed) commonReaderState.onColorCorrectionDisable() else consumed = false
                    else -> consumed = false
                }
                consumed
            }
    ) {
        val areaSize = screenScaleState.areaSize.collectAsState()
        if (areaSize.value == IntSize.Zero) {
            LoadingMaxSizeIndicator()
            return
        }

        when (commonReaderState.readerType.collectAsState().value) {
            PAGED -> {
                PagedReaderContent(
                    showHelpDialog = showHelpDialog,
                    onShowHelpDialogChange = { showHelpDialog = it },
                    showSettingsMenu = showSettingsMenu,
                    onShowSettingsMenuChange = { showSettingsMenu = it },
                    screenScaleState = screenScaleState,
                    pagedReaderState = pagedReaderState,
                    volumeKeysNavigation = volumeKeysNavigation
                )
            }

            CONTINUOUS -> {
                ContinuousReaderContent(
                    showHelpDialog = showHelpDialog,
                    onShowHelpDialogChange = { showHelpDialog = it },
                    showSettingsMenu = showSettingsMenu,
                    onShowSettingsMenuChange = { showSettingsMenu = it },
                    screenScaleState = screenScaleState,
                    continuousReaderState = continuousReaderState,
                    volumeKeysNavigation = volumeKeysNavigation
                )
            }

            PANELS -> {
                check(panelsReaderState != null)
                PanelsReaderContent(
                    showHelpDialog = showHelpDialog,
                    onShowHelpDialogChange = { showHelpDialog = it },
                    showSettingsMenu = showSettingsMenu,
                    onShowSettingsMenuChange = { showSettingsMenu = it },
                    screenScaleState = screenScaleState,
                    panelsReaderState = panelsReaderState,
                    volumeKeysNavigation = volumeKeysNavigation
                )
            }

        }

        SettingsOverlay(
            show = showSettingsMenu,
            commonReaderState = commonReaderState,
            pagedReaderState = pagedReaderState,
            continuousReaderState = continuousReaderState,
            panelsReaderState = panelsReaderState,
            onnxRuntimeSettingsState = onnxRuntimeSettingsState,
            screenScaleState = screenScaleState,
            isColorCorrectionsActive = isColorCorrectionActive,
            onColorCorrectionClick = onColorCorrectionClick,
            onBackPress = onExit,
            ohShowHelpDialogChange = { showHelpDialog = it },
        )

        EInkFlashOverlay(
            enabled = commonReaderState.flashOnPageChange.collectAsState().value,
            pageChangeFlow = commonReaderState.pageChangeFlow,
            flashEveryNPages = commonReaderState.flashEveryNPages.collectAsState().value,
            flashWith = commonReaderState.flashWith.collectAsState().value,
            flashDuration = commonReaderState.flashDuration.collectAsState().value
        )
    }
    LaunchedEffect(hasFocus) {
        if (!hasFocus) topLevelFocus.requestFocus()
    }
}

@Composable
fun ReaderControlsOverlay(
    readingDirection: LayoutDirection,
    onNexPageClick: suspend () -> Unit,
    onPrevPageClick: suspend () -> Unit,
    isSettingsMenuOpen: Boolean,
    onSettingsMenuToggle: () -> Unit,
    contentAreaSize: IntSize,
    modifier: Modifier,
    swipeGesturesEnabled: Boolean = false,
    screenScaleState: ScreenScaleState? = null,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val leftAction = {
        if (isSettingsMenuOpen) onSettingsMenuToggle()
        else if (readingDirection == LayoutDirection.Ltr) coroutineScope.launch { onPrevPageClick() }
        else coroutineScope.launch { onNexPageClick() }
    }
    val centerAction = { onSettingsMenuToggle() }
    val rightAction = {
        if (isSettingsMenuOpen) onSettingsMenuToggle()
        else if (readingDirection == LayoutDirection.Ltr) coroutineScope.launch { onNexPageClick() }
        else coroutineScope.launch { onPrevPageClick() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable()
            .pointerInput(
                contentAreaSize,
                readingDirection,
                onSettingsMenuToggle,
                isSettingsMenuOpen,
                swipeGesturesEnabled
            ) {
                // Only enable tap gestures if swipe gestures are disabled
                if (!swipeGesturesEnabled) {
                    detectTapGestures { offset ->
                        val actionWidth = contentAreaSize.width.toFloat() / 3
                        when (offset.x) {
                            in 0f..<actionWidth -> leftAction()
                            in actionWidth..actionWidth * 2 -> centerAction()
                            else -> rightAction()
                        }
                    }
                } else {
                    // When swipe gestures are enabled, only handle center tap
                    detectTapGestures { offset ->
                        val actionWidth = contentAreaSize.width.toFloat() / 3
                        if (offset.x in actionWidth..actionWidth * 2) {
                            centerAction()
                        }
                    }
                }
            }
            .pointerInput(
                readingDirection,
                isSettingsMenuOpen,
                swipeGesturesEnabled,
                screenScaleState
            ) {
                // Only handle horizontal swipe gestures if enabled
                if (swipeGesturesEnabled && screenScaleState != null) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDrag = Offset.Zero
                        var isHorizontalSwipe = false
                        
                        // Check if image is zoomed at the start of gesture
                        val currentZoom = screenScaleState.zoom.value
                        val minZoom = screenScaleState.scaleForFullVisibility() / screenScaleState.scaleFor100PercentZoom()
                        val isImageZoomed = currentZoom > minZoom * 1.01f // Small threshold for floating point comparison
                        
                        // Only handle single-finger gestures and when image is not zoomed
                        if (currentEvent.changes.size == 1 && !isImageZoomed) {
                            drag(down.id) { change ->
                                val dragDelta = change.positionChange()
                                totalDrag += dragDelta
                                
                                // Determine if this is a horizontal swipe based on initial movement
                                if (!isHorizontalSwipe && (abs(totalDrag.x) > 20f || abs(totalDrag.y) > 20f)) {
                                    isHorizontalSwipe = abs(totalDrag.x) > abs(totalDrag.y) * 1.5f
                                }
                                
                                // Don't consume the event to allow other gestures to work
                            }
                            
                            // Check if we should trigger a page change after drag ends
                            if (isHorizontalSwipe && !isSettingsMenuOpen) {
                                val swipeThreshold = 100f
                                
                                if (abs(totalDrag.x) > swipeThreshold) {
                                    // Swipe left (negative x) = next page
                                    // Swipe right (positive x) = previous page
                                    if (totalDrag.x < 0) {
                                        // Swipe left → next page
                                        coroutineScope.launch { onNexPageClick() }
                                    } else {
                                        // Swipe right → previous page
                                        coroutineScope.launch { onPrevPageClick() }
                                    }
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
