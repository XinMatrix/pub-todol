package com.todol.windows.service

import android.annotation.SuppressLint
import android.app.ForegroundServiceTypeException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.sqlite.SQLiteDatabase
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.view.isVisible
import com.todol.R
import com.todol.User
import com.todol.database.MyDatabaseHelper
import com.todol.mainPage.activity.FLOATING_MOVE_X
import com.todol.mainPage.activity.FLOATING_MOVE_Y
import com.todol.mainPage.activity.FLOAT_SIZE_INDEX
import com.todol.mainPage.activity.FLOAT_SIZE_LIST
import com.todol.mainPage.activity.THEME_INDEX
import com.todol.mainPage.activity.WINDOWS_ALPHA
import com.todol.myTableName
import com.todol.myToast
import com.todol.myVibrate
import com.todol.queryDatabase
import com.todol.startAnim
import java.util.Calendar

var SERVICE_RUNNING_CHECK = true //悬浮窗默认是加载的

class ScreenOrientationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
            // 屏幕方向发生变化
            val orientation = context.resources.configuration.orientation
            if (orientation == 2) {
                val stop = Intent(context, FloatingBallService::class.java)
                context.stopService(stop)
                myToast(context, "TODOL: 横屏模式已自动关闭")
            } else {
                val start = Intent(context, FloatingBallService::class.java)
                context.startService(start)
            }
        }
    }
}

class FloatingBallService : Service(), View.OnTouchListener {

    companion object {
        const val CHANNEL_ID = "MyForegroundServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var floatingView: View
    private var xDown = 0
    private var yDown = 0
    private var xDownNew = 0
    private var yDownNew = 0
    private var xUp = 0
    private var yUp = 0
    private var xMove = 0
    private var yMove = 0

    //数据库
    private lateinit var dbHelper: MyDatabaseHelper
    private lateinit var db: SQLiteDatabase

    //控件
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView

    //数据类
    private lateinit var itemList: MutableList<User>

    //定时任务
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    private lateinit var linearLayout: LinearLayout

    @SuppressLint("RtlHardcoded", "InflateParams", "UnspecifiedImmutableFlag", "SetTextI18n")
    override fun onCreate() {
        super.onCreate()

        dbHelper = MyDatabaseHelper(this)
        db = dbHelper.writableDatabase

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.circle_window, null)
        layoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        layoutParams.x = FLOATING_MOVE_X - screenWidth / 2
        layoutParams.y = FLOATING_MOVE_Y - screenHeight / 2

        layoutParams.alpha = WINDOWS_ALPHA

        windowManager.addView(floatingView, layoutParams)
        floatingView.setOnTouchListener(this)

        imageView = floatingView.findViewById(R.id.circleImageView)
        textView = floatingView.findViewById(R.id.circleTextView)

        linearLayout = floatingView.findViewById(R.id.circleWindowLinearLayout)
        startAnim(this, imageView, 5) //出现动画

        handler = Handler(Looper.getMainLooper())
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        //设置悬浮球大小
        run {
            val size = FLOAT_SIZE_LIST[FLOAT_SIZE_INDEX]
            val density = resources.displayMetrics.density
            val sizeDp = (size * density).toInt()
            val layoutParams = LinearLayout.LayoutParams(
                sizeDp,
                sizeDp
            )
            imageView.layoutParams = layoutParams
        }

        when (THEME_INDEX) {
            0 -> imageView.setImageResource(R.drawable.shape_red)
            1 -> imageView.setImageResource(R.drawable.shape_orange)
            2 -> imageView.setImageResource(R.drawable.shape_yellow)
            3 -> imageView.setImageResource(R.drawable.shape_green)
            4 -> imageView.setImageResource(R.drawable.shape_cyan)
            5 -> imageView.setImageResource(R.drawable.shape_blue)
            6 -> imageView.setImageResource(R.drawable.shape_purple)
            else -> imageView.setImageResource(R.drawable.shape_dark)
        }

        SERVICE_RUNNING_CHECK = true
        runnable = object : Runnable {
            override fun run() {
                handler.postDelayed(this, 60 * 1000)
                calTime() //函数尽量放在后面 保证任务的周期性
            }
        }
        handler.post(runnable)
        itemList = queryDatabase(db, myTableName)
        var myCount = 0
        for (i in 0 until itemList.size) if (itemList[i].state == 0 && itemList[i].data.isNotEmpty()) myCount++
        val channel = NotificationChannel(
            CHANNEL_ID,
            "默认前台通知",
            NotificationManager.IMPORTANCE_MIN //设置为低优先级，即没有提示音
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val showText = if (myCount == 0) "暂无待办" else "目前共有${myCount}条待办"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TODOL")
            .setContentText(showText)
            .setSmallIcon(R.drawable.todo)
            .build()
        startForeground(NOTIFICATION_ID, notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
        SERVICE_RUNNING_CHECK = false
        handler.removeCallbacksAndMessages(null)
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }

    override fun onBind(intent: Intent?) = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event?.let {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    xDown = event.rawX.toInt()
                    yDown = event.rawY.toInt()
                    xDownNew = xDown
                    yDownNew = yDown
                }

                MotionEvent.ACTION_MOVE -> {
                    xMove = event.rawX.toInt()
                    yMove = event.rawY.toInt()
                    layoutParams.x += xMove - xDown
                    layoutParams.y += yMove - yDown
                    windowManager.updateViewLayout(floatingView, layoutParams)
                    xDown = xMove
                    yDown = yMove
                    //一针见血 只记录移动后的值
                    FLOATING_MOVE_X = xMove
                    FLOATING_MOVE_Y = yMove
                }

                //UP表示用户离开屏幕
                MotionEvent.ACTION_UP -> {
                    xUp = event.rawX.toInt()
                    yUp = event.rawY.toInt()
                    if (xDownNew == xUp && yDownNew == yUp) {
                        val intent = Intent(this, InputWindows::class.java)
                        startService(intent)
                        stopSelf()
                    }
                }
            }
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    private fun calTime() {

        //返回到悬浮窗会立即执行  每分钟也会自动执行
        //因为要计算时间 所以就没有直接用LocalData
        val calNow = Calendar.getInstance().apply {
            get(Calendar.YEAR)
            get(Calendar.MONTH)
            get(Calendar.DAY_OF_MONTH)
            get(Calendar.HOUR_OF_DAY)
            get(Calendar.MINUTE)
        }.time.time

        //遍历相减
        itemList = queryDatabase(db, myTableName)
        for (i in 0 until itemList.size)
            if (itemList[i].remindTime.isNotEmpty() && itemList[i].state == 0) {
                //[2023, 5, 30, 6, 30]
                val outputArray = itemList[i].remindTime.replace("_", " ").split(" ").toTypedArray()
                //获取计划时间
                val calPlan = Calendar.getInstance().apply {
                    set(Calendar.YEAR, outputArray[0].toInt())
                    set(Calendar.MONTH, outputArray[1].toInt() - 1) //5月份对应的月份值为4
                    set(Calendar.DAY_OF_MONTH, outputArray[2].toInt())
                    set(Calendar.HOUR_OF_DAY, outputArray[3].toInt())
                    set(Calendar.MINUTE, outputArray[4].toInt())
                }.time.time
                val differenceInMinutes = ((calPlan - calNow) / (1000 * 60)) // 毫秒转换为分钟
                //10分钟的时候提醒一下 然后开始显示文字
                if (differenceInMinutes == 0L) myVibrate(this, 2000) //如果时间到了也要振动2秒
                //1-11 minute
                if (differenceInMinutes in 2..11) {
                    textView.isVisible = true
                    textView.text = "$differenceInMinutes 分钟"
                } else if (differenceInMinutes == 1L) {
                    textView.isVisible = true
                    textView.text = "小于1分钟"
                } else if (differenceInMinutes < 0) {
                    textView.isVisible = false
                }
            }
    }
}