package vn.example.khoamanhinh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Android có thể giới hạn tự khởi chạy; người dùng cần mở ứng dụng lần đầu.
            val launch = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(launch)
            } catch (_: Exception) {
                // Nếu Android chặn khởi chạy nền, mở ứng dụng thủ công.
            }
        }
    }
}
