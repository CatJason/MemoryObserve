package cat.jason.performance

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.kwai.android.base.BuildConfig
import com.kwai.koom.base.CommonConfig
import com.kwai.koom.base.MonitorLog
import com.kwai.koom.base.MonitorManager
import com.kwai.koom.javaoom.monitor.OOMHprofUploader
import com.kwai.koom.javaoom.monitor.OOMMonitorConfig
import com.kwai.koom.javaoom.monitor.OOMReportUploader
import java.io.File
import java.util.concurrent.Executors

internal object KOOMManager {
    fun initializeCommonConfig(
        application: Application,
        packageManager: PackageManager,
        packageName: String
    ) {
        val commonConfig =
            CommonConfig.Builder()
                .setApplication(application)
                .setDebugMode(BuildConfig.DEBUG)
                .setSdkVersionMatch(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
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

    fun initializeOOMMonitorConfig() {
        val config = OOMMonitorConfig.Builder()
            .setThreadThreshold(50) // 设置线程阈值。测试用50，通常请使用默认值。当线程数超过此值时，可能触发警报。
            .setFdThreshold(300) // 设置文件描述符阈值。测试用300，通常请使用默认值。文件描述符过多可能导致资源耗尽。
            .setHeapThreshold(0.9f) // 设置堆内存使用率阈值。测试用0.9（90%），通常请使用默认值。超过此比例可能表明内存溢出风险。
            .setVssSizeThreshold(1_000_000) // 设置虚拟内存大小阈值。测试用1,000,000，通常请使用默认值。用于检测总的内存使用量。
            .setMaxOverThresholdCount(1) // 设置超过阈值的最大次数。测试用1，通常请使用默认值。连续超过阈值这么多次后会触发分析。
            .setAnalysisMaxTimesPerVersion(3) // 设置每个版本分析的最大次数。考虑使用默认值。限制同一版本的分析次数，避免过度分析。
            .setAnalysisPeriodPerVersion(15 * 24 * 60 * 60 * 1000) // 设置每个版本的分析周期，以毫秒为单位。考虑使用默认值。定义版本更新后的分析时间窗口。
            .setLoopInterval(5_000) // 设置监控循环的间隔时间，单位为毫秒。测试用5,000，通常请使用默认值。定义检查内存状态的频率。
            .setEnableHprofDumpAnalysis(true) // 启用Hprof堆转储分析。用于深入分析内存溢出问题。
            .setHprofUploader(object: OOMHprofUploader {
                override fun upload(file: File, type: OOMHprofUploader.HprofType) {

                }
            })
            .setReportUploader(object: OOMReportUploader {
                override fun upload(file: File, content: String) {

                }
            })
            .build()

        MonitorManager.addMonitorConfig(config)
    }
}