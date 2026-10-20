package com.qrstudio.app.ui.create

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WhatsApp
import androidx.compose.material.icons.filled.Wifi

enum class ContentType(val label: String, val icon: ImageVector) {
    URL("رابط", Icons.Filled.Link),
    SOCIAL("سوشيال", Icons.Filled.Share),
    WIFI("واي فاي", Icons.Filled.Wifi),
    WHATSAPP("واتساب", Icons.Filled.WhatsApp),
    CONTACT("جهة اتصال", Icons.Filled.Person),
    EMAIL("إيميل", Icons.Filled.Email),
    PHONE("اتصال", Icons.Filled.Phone),
    SMS("رسالة", Icons.Filled.Sms),
    EVENT("موعد", Icons.Filled.Event),
    GEO("موقع", Icons.Filled.Place),
    TEXT("نص حر", Icons.Filled.TextFields),
}
