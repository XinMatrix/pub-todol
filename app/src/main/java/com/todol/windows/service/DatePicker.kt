package com.todol.windows.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.view.*
import android.widget.DatePicker
import android.widget.LinearLayout
import com.todol.*
import com.todol.mainPage.activity.THEME_INDEX
import com.todol.mainPage.activity.TOUCH_HEIGHT_INDEX
import com.todol.mainPage.activity.TOUCH_HEIGHT_LIST
import java.util.*

class DatePicker : Service(), View.OnTouchListener {

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
    private lateinit var datePicker: DatePicker

    private lateinit var touchLL: LinearLayout

    private var timerPosition = -1
    private var myDate = ""

    private var oldPlace = ""

    @SuppressLint("RtlHardcoded", "InflateParams", "SetTextI18n", "ResourceType")
    override fun onCreate() {
        super.onCreate()

        //悬浮窗 保持不变，因为要保证大小一致
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.date_picker, null)
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
        datePicker = floatingView.findViewById(R.id.datePicker)
        touchLL = floatingView.findViewById(R.id.datePickerTouchLL)

        windowManager.addView(floatingView, layoutParams)
        //悬浮窗监听
        floatingView.setOnTouchListener(this)

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
        myDate = intent.getStringExtra("date")!! //2023_12_21_
        oldPlace = intent.getStringExtra("placeTV") ?: ""
        //因为 myDate 默认填入今天，所以直接无脑初始化 月份要减一才行
        val tempInitData = myDate.split("_").toTypedArray()
        datePicker.init(tempInitData[0].toInt(), tempInitData[1].toInt() - 1, tempInitData[2].toInt()) { _, year, monthOfYear, dayOfMonth ->
            myDate = "${year}_${monthOfYear + 1}_${dayOfMonth}_" //少了个+1
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
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
                        run {
                            stopSelf()
                        }
                        run {
                            val intent = Intent(this, TimePicker::class.java)
                            intent.putExtra("timerPosition", timerPosition)
                            intent.putExtra("date", myDate)
                            intent.putExtra("placeTV", oldPlace)
                            startService(intent)
                        }

                    }
                }
            }
        }
        return true
    }

}
