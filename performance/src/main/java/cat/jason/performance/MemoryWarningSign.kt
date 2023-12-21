package cat.jason.performance

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object MemoryWarningSign {
    private const val REQUEST_CODE = 100 // 或者任何其他整数
    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            if (context is Activity) {
                // If context is an Activity, use startActivityForResult
                context.startActivityForResult(intent, REQUEST_CODE)
            } else {
                // If it's not an Activity (like a Service or Application context), start the intent without expecting a result
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // You need this flag when starting an activity from a non-Activity context
                context.startActivity(intent)
            }
            showPermissionToast(context)
        } else {
            context.startService(Intent(context, MemoryService::class.java))
            showGuideToast(context)
        }
    }

    fun hide(context: Context) {
        context.stopService(Intent(context, MemoryService::class.java))
    }
}


