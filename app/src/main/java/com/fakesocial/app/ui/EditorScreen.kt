package com.fakesocial.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fakesocial.app.EditorViewModel
import com.fakesocial.app.R
import com.fakesocial.app.data.ChatItem
import com.fakesocial.app.data.Format
import com.fakesocial.app.data.Kind
import com.fakesocial.app.data.MockData
import com.fakesocial.app.data.Platform
import com.fakesocial.app.ui.templates.Avatar
import com.fakesocial.app.ui.templates.MockPreview
import com.fakesocial.app.util.GallerySaver
import com.fakesocial.app.util.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel, onBack: () -> Unit) {
    val data by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val graphicsLayer = rememberGraphicsLayer()

    val msgSavedProject = stringResource(R.string.saved_project)
    val msgSavedGallery = stringResource(R.string.saved_gallery)
    val msgFailed = stringResource(R.string.save_failed)
    val msgNeedPermission = stringResource(R.string.need_permission)

    // ---- profile picture pickers (no storage permission needed) ----
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.saveFromUri(context, uri) }
            if (path != null) viewModel.edit { it.copy(avatarPath = path) }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) scope.launch {
            val path = withContext(Dispatchers.IO) { ImageStore.saveBitmap(context, bmp) }
            if (path != null) viewModel.edit { it.copy(avatarPath = path) }
        }
    }

    // ---- export ----
    fun doExport(share: Boolean) {
        scope.launch {
            val uri = try {
                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                GallerySaver.save(context, bitmap)
            } catch (e: Exception) {
                null
            }
            when {
                uri == null -> snackbar.showSnackbar(msgFailed)
                share -> shareImage(context, uri)
                else -> snackbar.showSnackbar(msgSavedGallery)
            }
        }
    }

    var pendingShare by remember { mutableStateOf<Boolean?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val share = pendingShare
        pendingShare = null
        if (granted && share != null) doExport(share)
        else scope.launch { snackbar.showSnackbar(msgNeedPermission) }
    }

    fun requestExport(share: Boolean) {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingShare = share
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            doExport(share)
        }
    }

    val kind = data.effectiveKind
    val usesHandle = kind != Kind.CHAT && data.platform in
        setOf(Platform.TWITTER, Platform.INSTAGRAM, Platform.LINKEDIN, Platform.YOUTUBE)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(data.platform.label) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } }
            )
        }
    ) { inner ->
        Column(
            Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- live preview, also the exact area that gets exported ----
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Box(
                    Modifier.drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    }
                ) { MockPreview(data) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { requestExport(false) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.save_gallery))
                }
                OutlinedButton(onClick = { requestExport(true) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.share))
                }
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            viewModel.save()
                            snackbar.showSnackbar(msgSavedProject)
                        } catch (e: Exception) {
                            snackbar.showSnackbar(msgFailed)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.save_project)) }

            // ---- options ----
            if (data.platform == Platform.INSTAGRAM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = data.variant == 0,
                        onClick = { viewModel.edit { it.copy(variant = 0, items = sampleItems(dm = false)) } },
                        label = { Text(stringResource(R.string.style_post)) }
                    )
                    FilterChip(
                        selected = data.variant == 1,
                        onClick = { viewModel.edit { it.copy(variant = 1, items = sampleItems(dm = true)) } },
                        label = { Text(stringResource(R.string.style_dm)) }
                    )
                }
            }
            ToggleRow(stringResource(R.string.dark_mode), data.dark) { v -> viewModel.edit { it.copy(dark = v) } }
            ToggleRow(stringResource(R.string.rtl_mode), data.rtl) { v -> viewModel.edit { it.copy(rtl = v) } }
            ToggleRow(stringResource(R.string.watermark_option), data.watermark) { v ->
                viewModel.edit { it.copy(watermark = v) }
            }

            // ---- profile ----
            Text(stringResource(R.string.avatar), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(data.avatarPath, data.name, 56.dp, Color(data.platform.accent))
                OutlinedButton(onClick = {
                    pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.pick_gallery)) }
                OutlinedButton(onClick = { cameraLauncher.launch(null) }) { Text(stringResource(R.string.take_photo)) }
                if (data.avatarPath != null) {
                    TextButton(onClick = { viewModel.edit { it.copy(avatarPath = null) } }) {
                        Text(stringResource(R.string.remove))
                    }
                }
            }

            Field(stringResource(R.string.field_name), data.name) { v -> viewModel.edit { it.copy(name = v) } }
            if (usesHandle) {
                Field(stringResource(R.string.field_handle), data.handle) { v -> viewModel.edit { it.copy(handle = v) } }
            }
            if (kind == Kind.CHAT) {
                Field(stringResource(R.string.field_status), data.status) { v -> viewModel.edit { it.copy(status = v) } }
                Field(stringResource(R.string.field_time), data.time) { v -> viewModel.edit { it.copy(time = v) } }
            } else {
                Field(stringResource(R.string.field_text), data.text, singleLine = false) { v ->
                    viewModel.edit { it.copy(text = v) }
                }
                Field(stringResource(R.string.field_time), data.time) { v -> viewModel.edit { it.copy(time = v) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(
                        stringResource(R.string.field_likes), data.likes.toString(),
                        modifier = Modifier.weight(1f), number = true
                    ) { v -> viewModel.edit { it.copy(likes = Format.parseCount(v)) } }
                    Field(
                        stringResource(R.string.field_comments), data.comments.toString(),
                        modifier = Modifier.weight(1f), number = true
                    ) { v -> viewModel.edit { it.copy(comments = Format.parseCount(v)) } }
                }
            }

            // ---- messages / replies ----
            Text(
                stringResource(if (kind == Kind.CHAT) R.string.items_chat else R.string.items_comments),
                fontWeight = FontWeight.SemiBold
            )
            data.items.forEachIndexed { index, item ->
                ItemEditor(
                    item = item,
                    isChat = kind == Kind.CHAT,
                    onChange = { block -> viewModel.updateItem(index, block) },
                    onDelete = { viewModel.removeItem(index) }
                )
            }
            OutlinedButton(onClick = { viewModel.addItem() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_item))
            }
        }
    }
}

/** Sample messages used when switching Instagram between the post and DM layouts. */
private fun sampleItems(dm: Boolean): List<ChatItem> =
    MockData.default(if (dm) Platform.WHATSAPP else Platform.INSTAGRAM).items

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    number: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = modifier.then(if (modifier == Modifier) Modifier.fillMaxWidth() else Modifier)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEditor(
    item: ChatItem,
    isChat: Boolean,
    onChange: ((ChatItem) -> ChatItem) -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isChat) {
                Field(stringResource(R.string.item_author), item.author) { v -> onChange { it.copy(author = v) } }
            }
            Field(stringResource(R.string.item_text), item.text, singleLine = false) { v ->
                onChange { it.copy(text = v) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(
                    stringResource(R.string.field_time), item.time,
                    modifier = Modifier.weight(1f)
                ) { v -> onChange { it.copy(time = v) } }
                if (isChat) {
                    Text(stringResource(R.string.item_mine))
                    Switch(checked = item.mine, onCheckedChange = { v -> onChange { it.copy(mine = v) } })
                }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
            }
        }
    }
}

private fun shareImage(context: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
