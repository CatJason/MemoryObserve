package cat.jason.performance

import android.annotation.SuppressLint
import android.app.Service
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
import com.kwai.koom.base.CommonConfig
import com.kwai.koom.base.Log
import com.kwai.koom.base.Logger
import com.kwai.koom.base.MonitorLog
import com.kwai.koom.base.MonitorManager
import com.kwai.koom.javaoom.hprof.ForkStripHeapDumper
import com.kwai.koom.javaoom.monitor.OOMHprofUploader
import com.kwai.koom.javaoom.monitor.OOMMonitorConfig
import com.kwai.koom.javaoom.monitor.OOMReportUploader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt


class MemoryService : Service() {
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
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var currentColor = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        initOOMMonitorManager()

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
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // 计算悬浮窗的新位置
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        params.x = (initialX + deltaX).toInt()
                        params.y = (initialY + deltaY).toInt()

                        // 更新悬浮窗的位置
                        windowManager.updateViewLayout(floatingView, params)
                    }
                }
                return true
            }
        })

        floatingView?.findViewById<TextView>(R.id.tvBatteryLevel)?.setOnLongClickListener {
            // 显示长时间的Toast消息
            Toast.makeText(applicationContext, "开始内存转储...", Toast.LENGTH_LONG).show()
            val internalStorageDir: File = this.filesDir

            val dumpDir = File(internalStorageDir, "dumps")
            if (!dumpDir.exists()) {
                dumpDir.mkdirs() // 如果目录不存在，则创建目录
            }

            val timestamp: String =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dumpFileName = "dump_$timestamp.hprof"
            val dumpFile = File(dumpDir, dumpFileName)

            ForkStripHeapDumper.getInstance().dump(dumpFile.absolutePath)
            true // 返回true表示消费了长按事件
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

    private fun initOOMMonitorManager() {
        initializeCommonConfig()
        val config = OOMMonitorConfig.Builder()
            .setThreadThreshold(50) //50 only for test! Please use default value!
            .setFdThreshold(300) // 300 only for test! Please use default value!
            .setHeapThreshold(0.9f) // 0.9f for test! Please use default value!
            .setVssSizeThreshold(1_000_000) // 1_000_000 for test! Please use default value!
            .setMaxOverThresholdCount(1) // 1 for test! Please use default value!
            .setAnalysisMaxTimesPerVersion(3) // Consider use default value！
            .setAnalysisPeriodPerVersion(15 * 24 * 60 * 60 * 1000) // Consider use default value！
            .setLoopInterval(5_000) // 5_000 for test! Please use default value!
            .setEnableHprofDumpAnalysis(true)
            .setHprofUploader(object : OOMHprofUploader {
                override fun upload(file: File, type: OOMHprofUploader.HprofType) {
                    MonitorLog.e("OOMMonitor", "todo, upload hprof ${file.name} if necessary")
                }
            })
            .setReportUploader(object : OOMReportUploader {
                override fun upload(file: File, content: String) {
                    MonitorLog.i("OOMMonitor", content)
                    MonitorLog.e("OOMMonitor", "todo, upload report ${file.name} if necessary")
                }
            })
            .build()

        MonitorManager.addMonitorConfig(config)
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
            .setLogger(object : Logger {
                override fun addCustomStatEvent(key: String, value: String?, realtimeReport: Boolean) {

                }

                override fun addExceptionEvent(message: String, @Logger.ExceptionType crashType: Int) {

                }

            })
            .setLog(object : Log {
                override fun v(tag: String, msg: String) = run { android.util.Log.v(tag, msg) }

                override fun i(tag: String, msg: String) = run { android.util.Log.i(tag, msg) }

                override fun d(tag: String, msg: String) = run { android.util.Log.d(tag, msg) }

                override fun w(tag: String, msg: String) = run { android.util.Log.w(tag, msg) }

                override fun e(tag: String, msg: String) = run { android.util.Log.e(tag, msg) }
            })
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
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024.0f / 1024.0f // 转换为 MB
        totalMemory = maxMemory
    }

    private fun updateFloatingWindow() {
        val view = floatingView?.findViewById<TextView>(R.id.tvBatteryLevel) ?: return
        val currentMemory = getMemoryData().roundToInt()
        view.text = "$currentMemory / $totalMemory MB"

        // Update background color based on memory usage
        updateBackground(currentMemory)
        breathing(view)
    }

    private fun breathing(view: TextView) {
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

