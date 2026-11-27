package com.example.ui.screens.store

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.model.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StoreScreen(
    listings: List<MarketplaceListing>,
    onOpenListing: (String) -> Unit,
    onOpenPublishListing: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(StoreCategory.ALL) }
    var sortLowToHigh by remember { mutableStateOf(false) }

    val filtered = remember(listings, query, selectedCategory, sortLowToHigh) {
        val base = listings.filter { item ->
            (selectedCategory == StoreCategory.ALL || item.category == selectedCategory) &&
                (query.isBlank() || item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true))
        }
        if (sortLowToHigh) base.sortedBy { it.discountedPricePts } else base
    }

    Scaffold(
        containerColor = NexoraBlack,
        bottomBar = {
            Surface(color = NexoraCharcoal.copy(alpha = 0.95f), border = BorderStroke(1.dp, DividerDark)) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    NexoraPurpleButton(
                        text = "Publish Listing",
                        icon = Icons.Default.Add,
                        onClick = onOpenPublishListing,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "publish_listing_button"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Store Marketplace", style = MaterialTheme.typography.headlineMedium, color = SoftWhite)
                    Text("Permitted Gaming Gear, Art & Coaching", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                }
                IconButton(
                    onClick = { sortLowToHigh = !sortLowToHigh },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (sortLowToHigh) NeonPurple.copy(alpha = 0.25f) else NexoraSurface)
                        .border(1.dp, if (sortLowToHigh) ElectricViolet else DividerDark, CircleShape)
                ) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort by price", tint = SoftWhite)
                }
            }

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                NexoraSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search triggers, coolers, overlays, coaching..."
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoreCategory.entries.forEach { cat ->
                    val selected = cat == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonPurple,
                            selectedLabelColor = SoftWhite,
                            containerColor = NexoraSurface,
                            labelColor = CoolGrey
                        ),
                        shape = RoundedCornerShape(50)
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filtered.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NexoraEmptyState(
                            icon = Icons.Default.Storefront,
                            title = "No Listings Found",
                            description = "Try clearing your search or publish a permitted gaming gear listing."
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { listing ->
                        ListingGridCard(listing = listing, onClick = { onOpenListing(listing.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingGridCard(
    listing: MarketplaceListing,
    onClick: () -> Unit
) {
    val fmt = remember { NumberFormat.getNumberInstance(Locale.US) }
    NexoraGlassCard(
        onClick = onClick,
        borderColor = if (listing.isFeatured) GoldBorder else VioletBorder,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
        ) {
            if (listing.uploadedImageUris.isNotEmpty()) {
                AsyncImage(
                    model = listing.uploadedImageUris.first(),
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = listing.imageRes),
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (listing.approvalStatus != ListingApprovalStatus.APPROVED) {
                NexoraStatusBadge(
                    text = "PENDING REVIEW",
                    containerColor = WarningContainer,
                    contentColor = WarningAmber,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
            }
        }
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleSmall,
                color = SoftWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${fmt.format(listing.discountedPricePts)} Pts",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrightGold,
                    fontWeight = FontWeight.Bold
                )
                if (listing.originalPricePts > listing.discountedPricePts) {
                    Text(
                        text = "${fmt.format(listing.originalPricePts)}",
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                        color = MutedGrey
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = BrightGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${listing.sellerRating}", style = MaterialTheme.typography.labelSmall, color = SoftWhite)
                }
                Text(listing.category.label, style = MaterialTheme.typography.labelSmall, color = ElectricViolet)
            }
        }
    }
}

@Composable
fun ListingDetailsScreen(
    listing: MarketplaceListing,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onReportListing: (String) -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("Suspicious or misleading item") }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.US) }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = {
            NexoraTopBar(
                title = "Listing Details",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Default.Flag, contentDescription = "Report Listing", tint = ErrorRed)
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = NexoraCharcoal, border = BorderStroke(1.dp, DividerDark)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    NexoraPurpleButton(
                        text = "Chat Privately with Seller",
                        icon = Icons.Default.Chat,
                        onClick = onOpenChat,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "chat_with_seller_button"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, VioletBorder, RoundedCornerShape(20.dp))
            ) {
                if (listing.uploadedImageUris.isNotEmpty()) {
                    AsyncImage(
                        model = listing.uploadedImageUris.first(),
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = listing.imageRes),
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (listing.uploadedImageUris.size > 1) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listing.uploadedImageUris.forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Listing preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, VioletBorder, RoundedCornerShape(12.dp))
                        )
                    }
                }
            }

            NexoraGlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NexoraStatusBadge(listing.category.label, VioletSubtle, ElectricViolet)
                    if (listing.bargainingAvailable) {
                        NexoraStatusBadge("Bargaining Open", SuccessContainer, SuccessGreen)
                    }
                    NexoraStatusBadge(listing.approvalStatus.label, GoldContainer, BrightGold)
                }
                Spacer(Modifier.height(10.dp))
                Text(listing.title, style = MaterialTheme.typography.headlineMedium, color = SoftWhite)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${fmt.format(listing.discountedPricePts)} Pts", style = MaterialTheme.typography.headlineSmall, color = BrightGold)
                    if (listing.originalPricePts > listing.discountedPricePts) {
                        Text(
                            "${fmt.format(listing.originalPricePts)} Pts",
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                            color = MutedGrey
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(listing.description, style = MaterialTheme.typography.bodyLarge, color = CoolGrey)
            }

            NexoraClayCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NexoraAvatar(avatarUri = null, contentDescription = listing.sellerName, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(listing.sellerName, style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                            Text("Verified Community Seller", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = BrightGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${listing.sellerRating} (${listing.reviewCount})", style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                    }
                }
            }

            if (listing.reviews.isNotEmpty()) {
                NexoraGlassCard {
                    Text("Verified Buyer Reviews", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                    Spacer(Modifier.height(10.dp))
                    listing.reviews.forEach { rev ->
                        Surface(
                            color = NexoraSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(rev.reviewerName, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                                    Text("★ ${rev.rating}/5", style = MaterialTheme.typography.labelMedium, color = BrightGold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(rev.comment, style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = NexoraCharcoal,
            title = { Text("Report Listing", color = SoftWhite) },
            text = {
                NexoraTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = "Reason for report",
                    minLines = 2,
                    singleLine = false
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReportListing(reportReason)
                        showReportDialog = false
                    }
                ) { Text("Submit Report", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = CoolGrey)
                }
            }
        )
    }
}

@Composable
fun PublishListingScreen(
    onBack: () -> Unit,
    onSubmitListing: (
        title: String,
        description: String,
        category: StoreCategory,
        originalPricePts: Int,
        discountedPricePts: Int,
        bargainingAvailable: Boolean,
        imageUris: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(StoreCategory.ACCESSORIES) }
    var originalPrice by remember { mutableStateOf("") }
    var discountedPrice by remember { mutableStateOf("") }
    var bargaining by remember { mutableStateOf(true) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = (selectedUris + uris).distinct().take(5)
        }
    }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = { NexoraTopBar(title = "Publish Listing", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NexoraGlassCard(borderColor = GoldBorder) {
                Text("Permitted Marketplace Guidelines", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "List gaming accessories, custom overlays, coaching, or pro gear. Free Fire account selling or diamond trading is strictly prohibited. All listings require admin approval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolGrey
                )
            }

            // Minimum 3 Images Photo Picker
            NexoraGlassCard {
                Text("Product Images (Min 3 Required)", style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    selectedUris.forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, ElectricViolet, RoundedCornerShape(14.dp))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NexoraSurface)
                            .border(1.dp, VioletBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("pick_listing_images_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add images", tint = ElectricViolet)
                            Text("${selectedUris.size}/3+", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                        }
                    }
                }
            }

            NexoraTextField(
                value = title,
                onValueChange = { title = it },
                label = "Listing Title",
                placeholder = "e.g. Pro Gaming Cooling Fan",
                testTag = "listing_title_input"
            )
            NexoraTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                placeholder = "Condition, specs, and delivery details...",
                singleLine = false,
                minLines = 3,
                testTag = "listing_description_input"
            )

            Text("Category", style = MaterialTheme.typography.titleSmall, color = SoftWhite)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoreCategory.entries.filter { it != StoreCategory.ALL }.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonPurple,
                            selectedLabelColor = SoftWhite,
                            containerColor = NexoraSurface,
                            labelColor = CoolGrey
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NexoraTextField(
                    value = originalPrice,
                    onValueChange = { originalPrice = it.filter(Char::isDigit) },
                    label = "Original Pts",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                NexoraTextField(
                    value = discountedPrice,
                    onValueChange = { discountedPrice = it.filter(Char::isDigit) },
                    label = "Offer Pts",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            NexoraClayCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bargaining Available", style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                    Switch(
                        checked = bargaining,
                        onCheckedChange = { bargaining = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = SoftWhite, checkedTrackColor = NeonPurple)
                    )
                }
            }

            if (errorMessage != null) {
                Text(errorMessage!!, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
            }

            NexoraPurpleButton(
                text = "Submit for Admin Approval",
                onClick = {
                    val orig = originalPrice.toIntOrNull() ?: 0
                    val disc = discountedPrice.toIntOrNull() ?: orig
                    when {
                        title.isBlank() -> errorMessage = "Listing title is required."
                        description.isBlank() -> errorMessage = "Listing description is required."
                        selectedUris.size < 3 -> errorMessage = "Please select at least 3 product images using the Photo Picker."
                        disc <= 0 -> errorMessage = "Enter a valid points value."
                        else -> {
                            errorMessage = null
                            onSubmitListing(
                                title,
                                description,
                                category,
                                orig.coerceAtLeast(disc),
                                disc,
                                bargaining,
                                selectedUris.map { it.toString() },
                                onBack,
                                { err -> errorMessage = err }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "submit_listing_button"
            )
        }
    }
}

@Composable
fun PrivateChatScreen(
    listing: MarketplaceListing,
    conversation: Conversation?,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onToggleBlock: () -> Unit,
    onReportUser: () -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    val messages = conversation?.messages ?: emptyList()
    val isBlocked = conversation?.isBlocked == true

    Scaffold(
        containerColor = NexoraBlack,
        topBar = {
            NexoraTopBar(
                title = listing.sellerName,
                onBack = onBack,
                actions = {
                    IconButton(onClick = onToggleBlock) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = "Block user",
                            tint = if (isBlocked) ErrorRed else CoolGrey
                        )
                    }
                    IconButton(onClick = onReportUser) {
                        Icon(Icons.Default.Flag, contentDescription = "Report user", tint = WarningAmber)
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = NexoraCharcoal, border = BorderStroke(1.dp, DividerDark)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NexoraTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = if (isBlocked) "User blocked" else "Message ${listing.sellerName}...",
                        modifier = Modifier.weight(1f),
                        testTag = "chat_message_input"
                    )
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank() && !isBlocked) {
                                onSendMessage(messageInput)
                                messageInput = ""
                            }
                        },
                        enabled = !isBlocked && messageInput.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (!isBlocked && messageInput.isNotBlank()) NeonPurple else NexoraSurface)
                            .testTag("send_chat_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = SoftWhite)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(color = NexoraSurface, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Listing: ${listing.title}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricViolet,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (isBlocked) {
                Surface(color = ErrorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "You blocked this user. Tap the block icon to unblock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (msg.isFromMe) NeonPurple else NexoraClay,
                            border = BorderStroke(1.dp, if (msg.isFromMe) ElectricViolet else DividerDark),
                            modifier = Modifier.fillMaxWidth(0.82f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = SoftWhite)
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(msg.timestamp, style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                                    if (msg.isFromMe && msg.isRead) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.DoneAll, contentDescription = "Read", tint = BrightGold, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
