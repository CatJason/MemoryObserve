package cat.jason.performance

import android.app.Service
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast

internal fun showCustomToast(context: Context, message: String?) {
    val inflater = context.applicationContext.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    val customToastView: View = inflater.inflate(R.layout.custom_toast, null)
    val textView = customToastView.findViewById<TextView>(R.id.custom_toast_message)
    textView.text = message

    val customToast = Toast(context.applicationContext)
    customToast.setView(customToastView)
    customToast.duration = Toast.LENGTH_LONG // 设置默认的显示时长，以避免不受控制的长时间显示

    // 设置Toast的位置为中间
    customToast.setGravity(Gravity.CENTER, 0, 0)

    customToast.show()
}

internal fun showGuideToast(context: Context) {
    showCustomToast(context,"图标拖拽，文字长按堆转储")
}

internal fun showPermissionToast(context: Context){
    showCustomToast(context,"打开悬浮窗权限\n重新开关按钮")
}