package com.todol.windows.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.PixelFormat
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import com.todol.*
import com.todol.database.MyDatabaseHelper
import com.todol.mainPage.activity.THEME_INDEX
import com.todol.mainPage.activity.TOUCH_HEIGHT_INDEX
import com.todol.mainPage.activity.TOUCH_HEIGHT_LIST
import java.time.LocalTime
import java.util.*

class TimePicker : Service(), View.OnTouchListener {

    //悬浮窗
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

    //动态添加TimePicker
    private lateinit var timePicker: TimePicker
    private lateinit var showTV: TextView

    private lateinit var touchLL: LinearLayout

    private var itemList = listOf<User>()

    //数据库
    private lateinit var dbHelper: MyDatabaseHelper
    private lateinit var db: SQLiteDatabase

    private var timerPosition = -1
    private var myDate = ""
    private var myTime = ""

    private var oldTime = ""
    private var oldPlace = ""

    @SuppressLint("RtlHardcoded", "InflateParams", "SetTextI18n", "ResourceType")
    override fun onCreate() {
        super.onCreate()

        //数据库
        dbHelper = MyDatabaseHelper(this)
        db = dbHelper.writableDatabase

        //悬浮窗 保持不变，因为要保证大小一致
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.time_picker, null)
        layoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER
        layoutParams.x = 0
        layoutParams.y = 0

        //动态组件
        timePicker = floatingView.findViewById(R.id.timePicker)
        showTV = floatingView.findViewById(R.id.timePickerShowTV)

        touchLL = floatingView.findViewById(R.id.timePickerTouchLL)

        // 设置24小时制显示
        timePicker.setIs24HourView(true)

        // 为TimePicker控件添加监听器
        timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            myTime = "${hourOfDay}_${minute}"
        }

        windowManager.addView(floatingView, layoutParams)
        //悬浮窗监听
        floatingView.setOnTouchListener(this)

        itemList = queryDatabase(db, myTableName).reversed()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        setLLHeight(this, TOUCH_HEIGHT_LIST[TOUCH_HEIGHT_INDEX], touchLL)

        when (THEME_INDEX) {
            0 -> touchLL.setBackgroundColor(resources.getColor(R.color.redFirstColor, theme))
            1 -> touchLL.setBackgroundColor(resources.getColor(R.color.orangeFirstColor, theme))
            2 -> touchLL.setBackgroundColor(resources.getColor(R.color.yellowFirstColor, theme))
            3 -> touchLL.setBackgroundColor(resources.getColor(R.color.greenFirstColor, theme))
            4 -> touchLL.setBackgroundColor(resources.getColor(R.color.cyanFirstColor, theme))
            5 -> touchLL.setBackgroundColor(resources.getColor(R.color.blueFirstColor, theme))
            6 -> touchLL.setBackgroundColor(resources.getColor(R.color.purpleFirstColor, theme))
            else -> touchLL.setBackgroundColor(resources.getColor(R.color.darkFirstColor, theme))
        }

        timerPosition = intent?.getIntExtra("timerPosition", 0)!! //获取序号
        myDate = intent.getStringExtra("date")!!
        //第一次设置时为空，再次设置时不为空 直接填充也可以
        oldTime = intent.getStringExtra("timeTV") ?: ""
        oldPlace = intent.getStringExtra("placeTV") ?: ""
        if (oldTime.isNotEmpty() && "日" !in oldTime && oldTime != "进行中") { //TimePicker设置默认选中
            timePicker.hour = oldTime.substringBefore(":").toInt()
            timePicker.minute = oldTime.substringAfter(":").toInt()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
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
                }

                MotionEvent.ACTION_UP -> {
                    xUp = event.rawX.toInt()
                    yUp = event.rawY.toInt()
                    if (xDownNew == xUp && yDownNew == yUp) {
                        if (myTime.isEmpty()) { //防止日期未改动造成崩溃
                            val nowTime = LocalTime.now()
                            myTime = "${nowTime.hour}_${nowTime.minute}"
                        }
                        run {
                            val totalTimeList = (myDate + myTime).split("_").toTypedArray() //[2023, 12, 21, 6, 29]
                            val differ = calculateTimeDifference(totalTimeList.toList())
                            if (differ.isNotFuture()) {
                                showTV.text = "我们还不能回到过去"
                                return@let
                            }
                        }
                        updateRemind(db, myTableName, itemList[timerPosition].id, myDate + myTime)
                        run {
                            stopSelf()
                        }
                        run {
                            val intent = Intent(this, PlacePicker::class.java)
                            intent.putExtra("timerPosition", timerPosition)
                            intent.putExtra("oldPlace", oldPlace)
                            startService(intent)
                        }
                    }
                }
            }
        }
        return true
    }

}
