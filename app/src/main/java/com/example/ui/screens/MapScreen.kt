package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.RouteUiState
import com.example.ScreenTab
import com.example.ZMapsViewModel
import com.example.data.AppLanguage
import com.example.data.LocationUiState
import com.example.data.RouteMode
import com.example.localization.ZStrings
import com.example.ui.components.DestinationCard
import com.example.ui.components.RouteOverviewSheet
import com.example.ui.components.ZMapsLogo
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.LocalZMapsAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.util.Locale

// Official OpenStreetMap Standard Tile Layer
private const val OSM_STANDARD_STYLE = """{
  "version": 8,
  "name": "OpenStreetMap Standard",
  "sources": {
    "osm-tiles": {
      "type": "raster",
      "tiles": [
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
      ],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors"
    }
  },
  "layers": [
    {
      "id": "osm-tiles-layer",
      "type": "raster",
      "source": "osm-tiles",
      "minzoom": 0,
      "maxzoom": 19
    }
  ]
}"""

// OpenStreetMap Cyber Dark Mode Layer (Carto Dark)
private const val OSM_DARK_STYLE = """{
  "version": 8,
  "name": "OpenStreetMap Cyber Dark",
  "sources": {
    "osm-dark-tiles": {
      "type": "raster",
      "tiles": [
        "https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png",
        "https://b.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png",
        "https://c.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png"
      ],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors, © CARTO"
    }
  },
  "layers": [
    {
      "id": "osm-dark-layer",
      "type": "raster",
      "source": "osm-dark-tiles",
      "minzoom": 0,
      "maxzoom": 20
    }
  ]
}"""

enum class OsmLayerType(val displayName: String, val styleJson: String) {
    STANDARD("OSM Standard", OSM_STANDARD_STYLE),
    DARK("OSM Cyber Dark", OSM_DARK_STYLE)
}

@Composable
fun MapScreen(
    viewModel: ZMapsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val accent = LocalZMapsAccent.current
    val language by viewModel.languageFlow.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val manualStartLocation by viewModel.manualStartLocation.collectAsStateWithLifecycle()
    val selectedDestination by viewModel.selectedDestination.collectAsStateWithLifecycle()
    val selectedRouteMode by viewModel.selectedRouteMode.collectAsStateWithLifecycle()
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarksFlow.collectAsStateWithLifecycle()
    val isNavigating by viewModel.isNavigating.collectAsStateWithLifecycle()
    val activeStepIndex by viewModel.activeStepIndex.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPermissionRationale by remember { mutableStateOf(false) }
    var currentLayer by remember { mutableStateOf(OsmLayerType.DARK) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.fetchLocation()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Location permission denied. You can tap and hold on map to set start location manually.")
            }
        }
    }

    // Default position (Metro Manila or globally recognized coordinates)
    val defaultLocation = remember { LatLng(14.5995, 120.9842) }

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var activeMarker by remember { mutableStateOf<Marker?>(null) }
    var startMarker by remember { mutableStateOf<Marker?>(null) }
    var activePolyline by remember { mutableStateOf<Polyline?>(null) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mapView.onDestroy()
            } catch (e: Exception) {
                // Ignore destruction error
            }
        }
    }

    // Update style when currentLayer changes
    LaunchedEffect(currentLayer, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        try {
            map.setStyle(Style.Builder().fromJson(currentLayer.styleJson)) {
                // Restore destination marker
                selectedDestination?.let { dest ->
                    activeMarker?.let { map.removeMarker(it) }
                    activeMarker = map.addMarker(
                        MarkerOptions()
                            .position(dest.latLng)
                            .title(dest.name)
                            .snippet(dest.address)
                    )
                }
                // Restore start marker
                manualStartLocation?.let { start ->
                    startMarker?.let { map.removeMarker(it) }
                    startMarker = map.addMarker(
                        MarkerOptions()
                            .position(start)
                            .title("Start Point (Manual)")
                            .snippet("Starting origin")
                    )
                }
                // Restore polyline if present
                if (routeState is RouteUiState.Success) {
                    val route = (routeState as RouteUiState.Success).route
                    if (route.points.isNotEmpty()) {
                        activePolyline?.let { map.removePolyline(it) }
                        activePolyline = map.addPolyline(
                            PolylineOptions()
                                .addAll(route.points)
                                .color(accent.color.toArgb())
                                .width(5f)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    // Animate camera when userLocation is first resolved
    LaunchedEffect(userLocation, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        userLocation?.let { loc ->
            if (selectedDestination == null) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15.0))
            }
        }
    }

    // Handle manual start location marker
    LaunchedEffect(manualStartLocation, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        startMarker?.let { map.removeMarker(it) }
        startMarker = null
        manualStartLocation?.let { start ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(start)
                    .title("Start Point (Manual)")
                    .snippet("Starting origin")
            )
            startMarker = marker
        }
    }

    // Animate camera and manage marker when selectedDestination changes
    LaunchedEffect(selectedDestination, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        activeMarker?.let { map.removeMarker(it) }
        activeMarker = null
        selectedDestination?.let { dest ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(dest.latLng)
                    .title(dest.name)
                    .snippet(dest.address)
            )
            activeMarker = marker
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(dest.latLng, 15.0))
        }
    }

    // Draw route polyline and fit camera bounds when route is available
    LaunchedEffect(routeState, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        activePolyline?.let { map.removePolyline(it) }
        activePolyline = null
        if (routeState is RouteUiState.Success) {
            val route = (routeState as RouteUiState.Success).route
            if (route.points.isNotEmpty()) {
                val polyline = map.addPolyline(
                    PolylineOptions()
                        .addAll(route.points)
                        .color(accent.color.toArgb())
                        .width(5f)
                )
                activePolyline = polyline
                try {
                    val boundsBuilder = LatLngBounds.Builder()
                    route.points.forEach { boundsBuilder.include(it) }
                    (userLocation ?: manualStartLocation)?.let { boundsBuilder.include(it) }
                    val bounds = boundsBuilder.build()
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                } catch (e: Exception) {
                    // Ignore empty bounds
                }
            }
        }
    }

    // Focus camera during active navigation when step changes
    LaunchedEffect(isNavigating, activeStepIndex, routeState, mapInstance) {
        val map = mapInstance ?: return@LaunchedEffect
        if (isNavigating && routeState is RouteUiState.Success) {
            val route = (routeState as RouteUiState.Success).route
            if (route.points.isNotEmpty()) {
                val targetPoint = if (activeStepIndex < route.points.size) {
                    route.points[activeStepIndex]
                } else {
                    route.points.first()
                }
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(targetPoint, 17.0))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // MapLibre Native Map with official OpenStreetMap tiles
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapInstance = map
                        try {
                            map.setStyle(Style.Builder().fromJson(currentLayer.styleJson))
                        } catch (e: Exception) {
                            try {
                                map.setStyle("https://demotiles.maplibre.org/style.json")
                            } catch (e2: Exception) {
                                // fallback
                            }
                        }
                        map.cameraPosition = CameraPosition.Builder()
                            .target(defaultLocation)
                            .zoom(13.0)
                            .build()

                        // Map Click: Clear destination if not navigating
                        map.addOnMapClickListener { _ ->
                            if (!isNavigating && routeState !is RouteUiState.Success) {
                                viewModel.clearDestination()
                            }
                            true
                        }

                        // Long-press: Set manual start location
                        map.addOnMapLongClickListener { point ->
                            viewModel.setManualStartLocation(point)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Start point set: ${String.format(Locale.US, "%.4f, %.4f", point.latitude, point.longitude)}"
                                )
                            }
                            true
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("osm_map_canvas")
        )

        // Top Overlay: Turn-by-Turn HUD (during navigation) OR Search & Brand Header (normal mode)
        if (isNavigating && routeState is RouteUiState.Success) {
            val route = (routeState as RouteUiState.Success).route
            val steps = route.steps
            val currentStep = steps.getOrNull(activeStepIndex)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Active Navigation Cockpit Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberSurface.copy(alpha = 0.98f))
                        .border(1.5.dp, accent.color, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .testTag("navigation_turn_by_turn_hud")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(accent.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val maneuverText = currentStep?.instruction?.lowercase(Locale.US) ?: ""
                                    val maneuverIcon = when {
                                        maneuverText.contains("left") -> Icons.AutoMirrored.Filled.ArrowBack
                                        maneuverText.contains("right") -> Icons.AutoMirrored.Filled.ArrowForward
                                        maneuverText.contains("arrive") -> Icons.Default.LocationOn
                                        else -> Icons.Default.Navigation
                                    }
                                    Icon(
                                        imageVector = maneuverIcon,
                                        contentDescription = "Maneuver",
                                        tint = CyberBackground,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (steps.isNotEmpty()) "STEP ${activeStepIndex + 1} OF ${steps.size}" else "NAVIGATION ACTIVE",
                                            color = accent.color,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (currentStep != null && currentStep.distanceText.isNotBlank()) {
                                            Text(
                                                text = " • ${currentStep.distanceText}",
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    Text(
                                        text = currentStep?.instruction ?: "Proceed along route",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Stepper controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.previousManeuverStep() },
                                    enabled = activeStepIndex > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous step",
                                        tint = if (activeStepIndex > 0) accent.color else TextMuted
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.nextManeuverStep() },
                                    enabled = activeStepIndex < steps.lastIndex,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next step",
                                        tint = if (activeStepIndex < steps.lastIndex) accent.color else TextMuted
                                    )
                                }
                            }
                        }

                        // Next maneuver teaser if available
                        if (activeStepIndex + 1 < steps.size) {
                            val nextStep = steps[activeStepIndex + 1]
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberBackground)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "THEN: ",
                                    color = accent.color,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = nextStep.instruction,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Normal Top Header: Logo, OSM Status, Search Input Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header Row: ZMAPS Logo & Author credit + OSM & GPS Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ZMapsLogo(
                        iconSize = 38.dp,
                        showSubtitle = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // OSM Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(CyberSurface.copy(alpha = 0.9f))
                                .border(1.dp, accent.color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "OPENSTREETMAP",
                                color = accent.color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // GPS Status Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(CyberSurface.copy(alpha = 0.9f))
                                .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                locationState is LocationUiState.Success -> accent.color
                                                manualStartLocation != null -> Color(0xFF00E5FF)
                                                locationState is LocationUiState.Locating -> Color(0xFFFFB300)
                                                else -> TextMuted
                                            }
                                        )
                                    )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        locationState is LocationUiState.Success -> "GPS"
                                        manualStartLocation != null -> "MANUAL"
                                        locationState is LocationUiState.Locating -> "FIX..."
                                        else -> "STANDBY"
                                    },
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Floating Search Input Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberSurface.copy(alpha = 0.95f))
                        .border(1.dp, accent.color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable { viewModel.switchTab(ScreenTab.SEARCH) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("floating_search_bar")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = accent.color,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = selectedDestination?.name ?: ZStrings.searchPlaceholder(language),
                            color = if (selectedDestination != null) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedDestination != null) {
                            IconButton(
                                onClick = { viewModel.clearDestination() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Buttons: OSM Layer Switcher + Centering on Current Location
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 18.dp,
                    bottom = if (isNavigating || selectedDestination != null || routeState is RouteUiState.Success) 220.dp else 24.dp
                ),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // OSM Layer Toggle Button (Dark vs Standard OSM tiles)
            FloatingActionButton(
                onClick = {
                    currentLayer = if (currentLayer == OsmLayerType.STANDARD) {
                        OsmLayerType.DARK
                    } else {
                        OsmLayerType.STANDARD
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("osm_layer_toggle_button"),
                containerColor = CyberSurface.copy(alpha = 0.95f),
                contentColor = accent.color,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Switch OSM Layer (${currentLayer.displayName})",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Location Centering FAB
            FloatingActionButton(
                onClick = {
                    if (locationState is LocationUiState.Success) {
                        val loc = (locationState as LocationUiState.Success).latLng
                        mapInstance?.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 16.0))
                    } else if (manualStartLocation != null) {
                        mapInstance?.animateCamera(CameraUpdateFactory.newLatLngZoom(manualStartLocation!!, 16.0))
                    } else {
                        showPermissionRationale = true
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .testTag("center_location_fab"),
                containerColor = CyberSurface.copy(alpha = 0.95f),
                contentColor = accent.color,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (locationState is LocationUiState.Locating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = accent.color,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (locationState is LocationUiState.Success) Icons.Default.GpsFixed else Icons.Default.NearMe,
                        contentDescription = ZStrings.myLocation(language),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Bottom Controls: Active Navigation Bar OR Destination Card OR Route Sheet OR Error Banner
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Case 1: Active Navigation Bottom Bar
            AnimatedVisibility(
                visible = isNavigating && routeState is RouteUiState.Success,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                if (routeState is RouteUiState.Success) {
                    val route = (routeState as RouteUiState.Success).route
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberSurface.copy(alpha = 0.98f))
                            .border(1.2.dp, accent.color, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                            .testTag("active_navigation_bottom_bar")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = route.durationText,
                                    color = accent.color,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "${route.distanceText} remaining • ${selectedDestination?.name ?: "Destination"}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { viewModel.endNavigation() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF3344),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("end_navigation_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ZStrings.endNavigation(language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Case 2: Destination Card (Place selected, preview route not yet calculated or sheet dismissed)
            AnimatedVisibility(
                visible = !isNavigating && selectedDestination != null && routeState !is RouteUiState.Success,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                selectedDestination?.let { dest ->
                    val isBookmarked = bookmarks.any { it.id == dest.id }
                    DestinationCard(
                        place = dest,
                        isBookmarked = isBookmarked,
                        language = language,
                        onRoutePreview = {
                            if (userLocation == null && manualStartLocation == null) {
                                showPermissionRationale = true
                            } else {
                                viewModel.calculateDrivingRoute()
                            }
                        },
                        onToggleBookmark = { viewModel.toggleBookmark(dest) },
                        onDismiss = { viewModel.clearDestination() }
                    )
                }
            }

            // Case 3: Route Overview Sheet (Calculated route ready for start)
            AnimatedVisibility(
                visible = !isNavigating && routeState is RouteUiState.Success,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                if (routeState is RouteUiState.Success) {
                    val route = (routeState as RouteUiState.Success).route
                    RouteOverviewSheet(
                        route = route,
                        language = language,
                        selectedMode = selectedRouteMode,
                        onModeSelected = { mode -> viewModel.setRouteMode(mode) },
                        onStartNavigation = { viewModel.startNavigation() },
                        onDismiss = { viewModel.clearRoute() }
                    )
                }
            }

            // Case 4: Route Loading Indicator
            AnimatedVisibility(
                visible = routeState is RouteUiState.Loading,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurface)
                        .border(1.dp, accent.color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = accent.color,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Calculating route via OSRM...",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Case 5: Route Error Banner with Retry & Clear
            AnimatedVisibility(
                visible = routeState is RouteUiState.Error,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                val errorMsg = (routeState as? RouteUiState.Error)?.message ?: "Route computation error"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurface)
                        .border(1.dp, Color(0xFFFF3344), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFFF3344)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMsg,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearRoute() }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Close",
                                    tint = TextMuted
                                )
                            }
                        }

                        if (userLocation == null && manualStartLocation == null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        val center = mapInstance?.cameraPosition?.target ?: defaultLocation
                                        viewModel.setManualStartLocation(center)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Start point set to current map center.")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent.color,
                                        contentColor = CyberBackground
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = ZStrings.useMapCenterStart(language),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Permission Rationale & Manual Start Location Dialog
        if (showPermissionRationale) {
            AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                title = {
                    Text(
                        text = ZStrings.myLocation(language),
                        color = accent.color,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = ZStrings.permissionExplanation(language),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tip: You can also tap and hold anywhere on the map to set a starting point manually.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                val center = mapInstance?.cameraPosition?.target ?: defaultLocation
                                viewModel.setManualStartLocation(center)
                                showPermissionRationale = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Starting point set to current map center.")
                                }
                            }
                        ) {
                            Text(
                                text = ZStrings.useMapCenterStart(language),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        TextButton(
                            onClick = {
                                showPermissionRationale = false
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Text(
                                text = ZStrings.grantPermission(language),
                                color = accent.color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionRationale = false }) {
                        Text(
                            text = ZStrings.close(language),
                            color = TextMuted
                        )
                    }
                },
                containerColor = CyberSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
