package com.livevideowallpaper.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.text.format.DateUtils
import androidx.core.content.ContextCompat

data class Entry(val name: String, val number: String, val sub: String)

object ContactsRepo {

    private fun has(c: Context, p: String) =
        ContextCompat.checkSelfPermission(c, p) == PackageManager.PERMISSION_GRANTED

    fun canReadContacts(c: Context) = has(c, Manifest.permission.READ_CONTACTS)
    fun canReadLog(c: Context) = has(c, Manifest.permission.READ_CALL_LOG)

    fun nameFor(c: Context, number: String): String? {
        if (number.isEmpty() || !canReadContacts(c)) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            c.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun contacts(c: Context): List<Entry> {
        val list = ArrayList<Entry>()
        if (!canReadContacts(c)) return list
        val phone = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val nameCol = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        val numCol = ContactsContract.CommonDataKinds.Phone.NUMBER
        try {
            c.contentResolver.query(phone, arrayOf(nameCol, numCol), null, null, "$nameCol COLLATE NOCASE ASC")?.use { cur ->
                while (cur.moveToNext()) {
                    val n = cur.getString(0) ?: ""
                    val num = cur.getString(1) ?: ""
                    if (num.isNotEmpty()) list.add(Entry(n.ifEmpty { num }, num, num))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun recents(c: Context): List<Entry> {
        val list = ArrayList<Entry>()
        if (!canReadLog(c)) return list
        try {
            val proj = arrayOf(
                CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE, CallLog.Calls.TYPE
            )
            c.contentResolver.query(CallLog.Calls.CONTENT_URI, proj, null, null, "${CallLog.Calls.DATE} DESC")?.use { cur ->
                var count = 0
                while (count < 150 && cur.moveToNext()) {
                    val num = cur.getString(0) ?: ""
                    val name = cur.getString(1)
                    val date = cur.getLong(2)
                    val type = when (cur.getInt(3)) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Declined"
                        else -> ""
                    }
                    val whenText = DateUtils.getRelativeTimeSpanString(
                        date, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                    val sub = if (type.isEmpty()) whenText else "$type • $whenText"
                    if (num.isNotEmpty()) list.add(Entry(if (name.isNullOrEmpty()) num else name, num, sub))
                    count++
                }
            }
        } catch (_: Exception) {}
        return list
    }
}
