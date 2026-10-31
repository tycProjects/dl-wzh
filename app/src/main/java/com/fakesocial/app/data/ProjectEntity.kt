package com.fakesocial.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,
    val variant: Int,
    val name: String,
    val handle: String,
    val avatarPath: String?,
    val text: String,
    val time: String,
    val likes: Int,
    val comments: Int,
    val status: String,
    val items: String,
    val dark: Boolean,
    val rtl: Boolean,
    val watermark: Boolean,
    val updatedAt: Long
)

fun ProjectEntity.toData() = MockData(
    id = id,
    platform = Platform.fromName(platform),
    variant = variant,
    name = name,
    handle = handle,
    avatarPath = avatarPath,
    text = text,
    time = time,
    likes = likes,
    comments = comments,
    status = status,
    items = ItemsCodec.decode(items),
    dark = dark,
    rtl = rtl,
    watermark = watermark,
    updatedAt = updatedAt
)

fun MockData.toEntity(now: Long = System.currentTimeMillis()) = ProjectEntity(
    id = id,
    platform = platform.name,
    variant = variant,
    name = name,
    handle = handle,
    avatarPath = avatarPath,
    text = text,
    time = time,
    likes = likes,
    comments = comments,
    status = status,
    items = ItemsCodec.encode(items),
    dark = dark,
    rtl = rtl,
    watermark = watermark,
    updatedAt = now
)
