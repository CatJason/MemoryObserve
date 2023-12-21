package cat.jason.performance

import android.annotation.SuppressLint
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.kwai.android.base.BuildConfig
import com.kwai.koom.base.CommonConfig
import com.kwai.koom.base.MonitorManager
import com.kwai.koom.javaoom.hprof.ForkStripHeapDumper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt


internal class MemoryService : Service() {
    companion object {
        const val GREEN = 0
        const val BLUE = 1
        const val RED = 2
    }

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var flag = true
    private var time = 1000L
    private var totalMemory = 0f
    private var initialY: Int = 0
    private var initialTouchY: Float = 0f
    private var currentColor = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        initializeCommonConfig()

        // 设置悬浮窗布局参数
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END // 设置为顶部右侧
        params.x = 0 // 悬浮窗距离右边的距离
        params.y = 0 // 悬浮窗距离顶部的距离
        // 加载悬浮窗布局
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_window_layout, null)

        initMaxJavaMemory()
        windowManager.addView(floatingView, params)
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 记录初始位置和触摸点的位置
                        initialY = params.y
                        initialTouchY = event.rawY
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // 计算悬浮窗的新位置
                        val deltaY = event.rawY - initialTouchY
                        params.y = (initialY + deltaY).toInt()

                        // 更新悬浮窗的位置
                        windowManager.updateViewLayout(floatingView, params)
                    }
                }
                return true
            }
        })

        val textView = floatingView?.findViewById<TextView>(R.id.tvBatteryLevel)
        textView?.setOnLongClickListener {
            val internalStorageDir: File = this.filesDir

            val dumpDir = File(internalStorageDir, "dumps")
            if (!dumpDir.exists()) {
                dumpDir.mkdirs() // 如果目录不存在，则创建目录
            }

            val timestamp: String =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dumpFileName = "dump_$timestamp.hprof"
            val dumpFile = File(dumpDir, dumpFileName)

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val dumpFilePath = dumpFile.absolutePath

            val clipData = ClipData.newPlainText("DumpFilePath", dumpFilePath)
            clipboard.setPrimaryClip(clipData)
            ForkStripHeapDumper.getInstance().dump(dumpFilePath)
            showCustomToast(this,"开始堆转储...\n路径已经复制到粘贴板")
            true // 返回true表示消费了长按事件
        }

        textView?.setOnClickListener {
            showGuideToast(this)
        }

        val handler = Handler()
        val runnable: Runnable = object : Runnable {
            override fun run() {
                updateFloatingWindow()
                handler.postDelayed(this, time) // 每秒调用一次
            }
        }

        // 启动定时任务
        handler.post(runnable)
    }

    private fun initializeCommonConfig() {
        val commonConfig = CommonConfig.Builder()
            .setApplication(application)
            .setDebugMode(BuildConfig.DEBUG)
            .setSdkVersionMatch(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            .setVersionNameInvoker {
                packageManager.getPackageInfo(packageName, 0).versionName
            }
            .setRootFileInvoker { fileName ->
                File(application.getExternalFilesDir(null), fileName)
            }
            .setSharedPreferencesInvoker { name ->
                application.getSharedPreferences(name, Context.MODE_PRIVATE)
            }
            .setSharedPreferencesKeysInvoker { sharedPreferences ->
                sharedPreferences.all.keys
            }
            .setLoadSoInvoker { soName ->
                System.loadLibrary(soName)
            }
            .setExecutorServiceInvoker {
                Executors.newSingleThreadExecutor()
            }
            .setLoopHandlerInvoker {
                Handler(Looper.getMainLooper())
            }
            .build()

            MonitorManager.initCommonConfig(commonConfig)
    }

    private fun initMaxJavaMemory() {
        val maxMemory = Runtime.getRuntime().maxMemory()  // 转换为 MB
        totalMemory = if(maxMemory > 0) {
            maxMemory / 1024.0f / 1024.0f
        } else {
            0f
        }
    }

    private fun getJavaUsedMemory(): Float {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        return if (usedMemory >= 0) {
            usedMemory / 1024.0f / 1024.0f
        } else {
            0f
        }
    }

    private fun updateFloatingWindow() {
        val view = floatingView?.findViewById<TextView>(R.id.tvBatteryLevel) ?: return
        val currentMemory = getJavaUsedMemory().roundToInt()
        view.text = "$currentMemory / $totalMemory MB"

        // Update background color based on memory usage
        updateBackground(currentMemory)
        breathingAnim(view)
    }

    private fun breathingAnim(view: TextView) {
        if (flag) {
            // If flag is true, start heartbeat animation and set to visible
            view.alpha = 0.7f
            time = 200L // Duration for heartbeat
        } else {
            // If flag is false, just show without animation
            view.alpha = 1f
            time = 1000L // Duration for static display
        }
        // Toggle the flag
        flag = !flag
    }

    private fun updateBackground(currentMemory: Int) {
        when {
            totalMemory - currentMemory < (totalMemory / 3) && currentColor != RED -> {
                currentColor = RED
                floatingView?.setBackgroundColor(Color.parseColor("#ffff4444"))
            }

            totalMemory - currentMemory < (totalMemory / 3 * 2) && currentColor != BLUE -> {
                currentColor = BLUE
                floatingView?.setBackgroundColor(Color.parseColor("#ff33b5e5"))
            }

            else -> {
                if (currentColor != GREEN) {
                    currentColor = GREEN
                    floatingView?.setBackgroundColor(Color.parseColor("#ff99cc00"))
                }
            }
        }
    }

    private fun getMemoryData(): Float {
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)
        val totalPss = memInfo.totalPss
        return if (totalPss >= 0) totalPss / 1024.0f else 0f // 将PSS值转换为MB
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager.removeView(floatingView)
            floatingView = null
        }
    }
}

