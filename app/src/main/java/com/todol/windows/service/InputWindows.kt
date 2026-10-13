package com.todol.windows.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.todol.*
import com.todol.database.MyDatabaseHelper
import com.todol.mainPage.activity.BUTTON_HEIGHT_INDEX
import com.todol.mainPage.activity.BUTTON_HEIGHT_LIST
import com.todol.mainPage.activity.CHOOSE_POSITION
import com.todol.mainPage.activity.SHOW_STRING
import com.todol.mainPage.activity.THEME_INDEX
import com.todol.mainPage.activity.TOUCH_HEIGHT_INDEX
import com.todol.mainPage.activity.TOUCH_HEIGHT_LIST
import com.todol.mainPage.activity.WINDOWS_ALPHA
import com.todol.mainPage.activity.WINDOWS_HEIGHT_INDEX
import com.todol.mainPage.activity.WINDOWS_HEIGHT_LIST
import com.todol.mainPage.activity.WINDOWS_WIDTH_INDEX
import com.todol.mainPage.activity.WINDOWS_WIDTH_LIST
import com.todol.windows.activity.FILE_CHOOSER_CHECK
import com.todol.windows.activity.FileChooser
import com.todol.windows.adapter.MyListAdapter
import com.todol.windows.adapter.tempFileUrl
import com.todol.windows.view.ZoomRotateIV
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.math.abs

private var topCount = 0 //记录置顶位置

var INPUT_WINDOWS_POSITION_X = 0 //记录输入窗口位置
var INPUT_WINDOWS_POSITION_Y = 0 //记录输入窗口位置

var INPUT_WINDOWS_RUNNING_CHECK = false //输入悬浮窗默认是加载的

var addAniCheck = true //Only Use In InputWindows And MyListAdapter
var topAniCheck = true //Only Use In InputWindows And MyListAdapter
var finishAniCheck = true //Only Use In InputWindows And MyListAdapter
var setPlaceAniCheck = true //Only Use In InputWindows And MyListAdapter

var itemList = listOf<User>() //Only Use In InputWindows And MyListAdapter And a little in FileChooser

private var longClickCheck = false

private var restartCheck = false

class InputWindows : Service(), View.OnTouchListener {

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

    //设置动画的全局视图
    private lateinit var linearLayout: LinearLayout

    //空页面
    private lateinit var topLL: LinearLayout
    private lateinit var emptyLL: LinearLayout

    //动态添加EditText
    private lateinit var recyclerView: RecyclerView

    private lateinit var myListAdapter: MyListAdapter

    private lateinit var touchLL: LinearLayout

    private lateinit var aniIV: ImageView

    //添加、关闭按钮
    private lateinit var addTV: TextView
    private lateinit var topTV: TextView
    private lateinit var fileTV: TextView
    private lateinit var fileSpareLL: LinearLayout
    private lateinit var addAndTopLL: LinearLayout

    private lateinit var showTextView: TextView

    //显示放大图片
    private lateinit var imageView: ZoomRotateIV

    //显示视频播放
    private lateinit var videoView: VideoView

    //数据库
    private lateinit var dbHelper: MyDatabaseHelper
    private lateinit var db: SQLiteDatabase

    //监听长按事件
    private val longClickDuration = 1000 // 长按时间的阈值，单位为毫秒
    private var longClickStartTime = 0L // 长按事件开始时间

    private var topCheck = false

    private var addCheck = false //控制添加获取焦点

    private lateinit var topTVListener: View.OnClickListener

    @SuppressLint("RtlHardcoded", "InflateParams", "SetTextI18n", "ResourceType")
    override fun onCreate() {
        super.onCreate()
        initViews()
        initListener()

        dbHelper = MyDatabaseHelper(this)
        db = dbHelper.writableDatabase

        setLLHeight(this, TOUCH_HEIGHT_LIST[TOUCH_HEIGHT_INDEX], touchLL)
        setLLHeight(this, BUTTON_HEIGHT_LIST[BUTTON_HEIGHT_INDEX], addAndTopLL)

    }

    @SuppressLint("InflateParams")
    private fun initViews() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.input_windows, null)
        layoutParams = WindowManager.LayoutParams(
            WINDOWS_WIDTH_LIST[WINDOWS_WIDTH_INDEX], //最小可用宽度
            WINDOWS_HEIGHT_LIST[WINDOWS_HEIGHT_INDEX],
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER
        layoutParams.alpha = WINDOWS_ALPHA
        layoutParams.x = INPUT_WINDOWS_POSITION_X
        layoutParams.y = INPUT_WINDOWS_POSITION_Y

        linearLayout = floatingView.findViewById(R.id.inputWindowsLL)

        topLL = floatingView.findViewById(R.id.inputWindowsTopLL) //默认可见
        emptyLL = floatingView.findViewById(R.id.inputWindowsEmptyLL) //默认不可见

        //动态组件
        recyclerView = floatingView.findViewById(R.id.inputWindowsRV)
        showTextView = floatingView.findViewById(R.id.showTextView)

        imageView = floatingView.findViewById(R.id.inputWindowsIV)
        videoView = floatingView.findViewById(R.id.inputWindowsVV)

        addTV = floatingView.findViewById(R.id.addTV)
        topTV = floatingView.findViewById(R.id.topTV)
        fileTV = floatingView.findViewById(R.id.fileTV)
        fileSpareLL = floatingView.findViewById(R.id.fileSpareLL)
        addAndTopLL = floatingView.findViewById(R.id.inputWindowsAddAndTopLL)

        touchLL = floatingView.findViewById(R.id.inputWindowsTouchLL)
        aniIV = floatingView.findViewById(R.id.inputWindowsAniIV)

        setViewVisible(listOf(fileTV, fileSpareLL), false)

        windowManager.addView(floatingView, layoutParams)
        floatingView.setOnTouchListener(this) //悬浮窗监听

    }

    private fun initListener() {
        //这个东西好容易出问题
        addTV.setOnClickListener {
            for (i in itemList.indices) if (itemList[i].data.isEmpty()) { //这个判定必须写在 addCheck = true 前面
                recyclerView.smoothScrollToPosition(i)
                showTextView.text = "请先输入笔记再添加！"
                return@setOnClickListener
            }
            addAniCheck = false //暂停动画
            addCheck = true //获取焦点
            topLL.isVisible = true
            emptyLL.isVisible = false
            val nowTime = LocalTime.now()
            val nowDate = LocalDate.now()
            val fullTime = "${nowDate.year}_${nowDate.monthValue}_${nowDate.dayOfMonth}_${nowTime.hour}_${nowTime.minute}"
            if (myTableName.isSon()) insertUser(db, myTableName.split('I').last(), getUUID(), "", 0, "", 0, fullTime, "", "", "", "0", "0")
            else insertUser(db, myTableName, getUUID(), "", 0, "", 0, fullTime, "", "", "", "0", "0")
            val intent = Intent(this, InputWindows::class.java)
            startService(intent)
        }

        topTVListener = View.OnClickListener {
            topAniCheck = false //暂停动画
            if (topTV.text == "置顶") {
                topTV.text = "完成"
                topCheck = true
                val intentAgain = Intent(this@InputWindows, InputWindows::class.java)
                startService(intentAgain)
            } else {
                topTV.text = "置顶"
                topCheck = false
                val intentAgain = Intent(this@InputWindows, InputWindows::class.java)
                startService(intentAgain)
            }
        }
        topTV.setOnClickListener(topTVListener)
        fileTV.setOnClickListener {
            FILE_CHOOSER_CHECK = true
            val intent = Intent(applicationContext, FileChooser::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        run { //重定义主题
            val colorIdList = when (THEME_INDEX) {
                0 -> {
                    listOf(
                        R.color.redSecondColor,
                        R.color.redFirstColor,
                        R.color.redFirstColor,
                        R.color.redFirstColor,
                        R.color.redTextColor,
                        R.color.redFirstColor
                    )
                }

                1 -> {
                    listOf(
                        R.color.orangeSecondColor,
                        R.color.orangeFirstColor,
                        R.color.orangeFirstColor,
                        R.color.orangeFirstColor,
                        R.color.orangeTextColor,
                        R.color.orangeFirstColor
                    )
                }

                2 -> {
                    listOf(
                        R.color.yellowSecondColor,
                        R.color.yellowFirstColor,
                        R.color.yellowFirstColor,
                        R.color.yellowFirstColor,
                        R.color.yellowTextColor,
                        R.color.yellowFirstColor
                    )
                }

                3 -> {
                    listOf(
                        R.color.greenSecondColor,
                        R.color.greenFirstColor,
                        R.color.greenFirstColor,
                        R.color.greenFirstColor,
                        R.color.greenTextColor,
                        R.color.greenFirstColor
                    )
                }

                4 -> {
                    listOf(
                        R.color.cyanSecondColor,
                        R.color.cyanFirstColor,
                        R.color.cyanFirstColor,
                        R.color.cyanFirstColor,
                        R.color.cyanTextColor,
                        R.color.cyanFirstColor
                    )
                }

                5 -> {
                    listOf(
                        R.color.blueSecondColor,
                        R.color.blueFirstColor,
                        R.color.blueFirstColor,
                        R.color.blueFirstColor,
                        R.color.blueTextColor,
                        R.color.blueFirstColor
                    )
                }

                6 -> {
                    listOf(
                        R.color.purpleSecondColor,
                        R.color.purpleFirstColor,
                        R.color.purpleFirstColor,
                        R.color.purpleFirstColor,
                        R.color.purpleTextColor,
                        R.color.purpleFirstColor
                    )
                }

                else -> {
                    listOf(
                        R.color.darkSecondColor,
                        R.color.darkFirstColor,
                        R.color.darkFirstColor,
                        R.color.darkFirstColor,
                        R.color.darkTextColor,
                        R.color.darkFirstColor
                    )
                }
            }
            addAndTopLL.setBackgroundColor(resources.getColor(colorIdList[0], theme))
            addTV.setTextColor(resources.getColor(colorIdList[1], theme))
            topTV.setTextColor(resources.getColor(colorIdList[2], theme))
            fileTV.setTextColor(resources.getColor(colorIdList[3], theme))
            showTextView.setTextColor(resources.getColor(colorIdList[4], theme))
            touchLL.setBackgroundColor(resources.getColor(colorIdList[5], theme))
        }

        INPUT_WINDOWS_RUNNING_CHECK = true
        itemList = queryDatabase(db, myTableName.split('I').last()).reversed()
        //加载完置空
        //判定数据是否为空
        if (itemList.isEmpty()) {
            emptyLL.isVisible = true
            topLL.isVisible = false
        } else { //防止返回时空数据图片挡住列表
            emptyLL.isVisible = false
            topLL.isVisible = true
        }

        if (tempFileUrl.isNotEmpty())
            if (tempFileUrl.isImageFile()) { //注意先后顺序 //这个是图片
                setViewVisible(listOf(topLL, addAndTopLL), false)
                initImage(this, tempFileUrl, imageView)
            } else if (tempFileUrl.isVideoFile()) { //Show Videos
                setViewVisible(listOf(topLL, addAndTopLL), false)
                videoView.isVisible = true
                videoView.setVideoPath(tempFileUrl)
                videoView.start()
            }

        //添加不需要动画，完成不需要动画，置顶不需要动画，设置定时不需要动画 这个动画要加到两个地方
        if (addAniCheck && finishAniCheck && topAniCheck && setPlaceAniCheck) startAnim(this, linearLayout, 5) else {
            Handler(mainLooper).postDelayed({ //延迟复原
                addAniCheck = true
                finishAniCheck = true
                topAniCheck = true
                setPlaceAniCheck = true
            }, 500)
        } //重启动画
        //通过startService()启动一个已经在运行中的服务 系统将不会创建新的实例 而是调用onStartCommand()
        if (!myTableName[myTableName.length - 1].isDigit()) SHOW_STRING = "".also { showTextView.text = "" }
        else showTextView.text = SHOW_STRING
        if (longClickCheck) { //设置提示文本 注意先后顺序 避免被覆盖
            showTextView.text = "已切换(长按)"
            setViewVisible(listOf(fileTV, fileSpareLL), false)
            longClickCheck = false
        }
        myListAdapter = MyListAdapter(this, showTextView, topCheck, topTV, fileTV, fileSpareLL, aniIV)

        class CustomScrollListener : RecyclerView.OnScrollListener() { //用于判定是用户滑动还是系统滑动
            private var isUserScrolling = false
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) { // 如果滚动状态为 SCROLL_STATE_IDLE，表示滚动已停止
                    if (isUserScrolling) { //这个应该是用户手动滑动，此时应该回收键盘
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val firstPosition = layoutManager.findFirstVisibleItemPosition()
                        val lastPosition = layoutManager.findLastVisibleItemPosition()
                        for (i in firstPosition..lastPosition) {
                            val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? MyListAdapter.MyViewHolder
                            viewHolder?.let {
                                //只要滑动就会执行以下代码
                                it.editText.clearFocus()
                                dropKeyBoard(this@InputWindows, it.editText)
                                //重设 ClickListener
                                if (topTV.text.startsWith("粘贴")) topTV.text = "置顶"
                                topTV.setOnClickListener(topTVListener)
                            }
                        }
                    }
                    isUserScrolling = false // 重置滚动标志
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) { // 如果滚动状态为 SCROLL_STATE_DRAGGING，表示用户正在手动滑动
                    isUserScrolling = true
                }
            }
        }
        recyclerView.addOnScrollListener(CustomScrollListener())
        recyclerView.adapter = myListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.post { //防止初次加载时执行
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastPosition = layoutManager.findLastVisibleItemPosition()
                    for (i in firstPosition..lastPosition) {
                        val viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? MyListAdapter.MyViewHolder
                        viewHolder?.let {  //这东西真不好搞，还好有GPT
                            if (restartCheck) {
                                restartCheck = false
                                Handler(mainLooper).postDelayed({
                                    dropKeyBoard(this@InputWindows, it.editText)
                                }, 50) //Nice 延迟切换可以解决一些不符合逻辑的问题
                            }
                            topCount = firstPosition
                            setViewVisible(listOf(it.clockIB, fileTV, fileSpareLL), false)
                            //添加时起作用
                            if (addCheck)  //默认添加焦点
                                if (topCount == 0) { //如果第零个元素可见应该直接获取焦点而非执行scroll
                                    addCheck = false //我知道了，应该是不让点击导致的
                                    Handler(mainLooper).postDelayed({
                                        showKeyBoard(this@InputWindows, it.editText)
                                    }, 150) //Nice
                                } else recyclerView.smoothScrollToPosition(0)  //滑到顶部
                            //希望这个世界对你好点
                        }
                    }
                }

            })
        }
        recyclerView.scrollToPosition(CHOOSE_POSITION)
        CHOOSE_POSITION = 0 //Nice 记录置顶前位置
        recyclerView.scrollToPosition(topCount)
        topCount = 0
        setViewVisible(listOf(fileSpareLL, fileTV), false) //防止切换后还有插入的情况
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        INPUT_WINDOWS_RUNNING_CHECK = false
        db.close()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView) //不太明白这句话的作用
    }

    override fun onBind(intent: Intent?) = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event?.let {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    longClickStartTime = System.currentTimeMillis() // 记录点击时间
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
                    INPUT_WINDOWS_POSITION_X = layoutParams.x
                    INPUT_WINDOWS_POSITION_Y = layoutParams.y
                    windowManager.updateViewLayout(floatingView, layoutParams)
                    xDown = xMove
                    yDown = yMove
                }

                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - longClickStartTime // 计算时间间隔
                    xUp = event.rawX.toInt()
                    yUp = event.rawY.toInt()
                    if (abs(xDownNew - xUp) <= 10 && abs(yDownNew - yUp) <= 10 && duration < longClickDuration) {
                        //这里是正常返回
                        if (tempFileUrl.isNotEmpty()) {
                            tempFileUrl = "" //加载完再重置
                            setViewVisible(listOf(topLL, addAndTopLL), true)
                            setViewVisible(listOf(imageView, videoView), false)
                            addAniCheck = false
                            return@let
                        }
                        //点击返回上一层
                        if (myTableName[myTableName.length - 1].isDigit()) {
                            //SHORT_2024_1_10_19_13_50_542
                            //如果父级为空则取前几个字母，否则遍历查找父级
                            myTableName = myTableName.split('I').lastTwo()
                            restartCheck = true //Nice
                            val intent = Intent(this, InputWindows::class.java)
                            startService(intent)
                        } else { //close
                            val intent = Intent(this, FloatingBallService::class.java)
                            startService(intent)
                            stopSelf()
                        }
                    }
                    //50的误差 防止无法切换 长按返回顶层
                    if (abs(xDownNew - xUp) <= 10 && abs(yDownNew - yUp) <= 10 && duration >= longClickDuration) {
                        if (tempFileUrl.isNotEmpty()) { //图片显示不允许长按
                            tempFileUrl = "" //加载完再重置
                            setViewVisible(listOf(topLL, addAndTopLL), true)
                            setViewVisible(listOf(imageView, videoView), false)
                            addAniCheck = false
                            return@let
                        }
                        //这段代码的意思是检查是否为第一级
                        //再看是SHORT还是LONG 如果是SHORT就返回SHORT顶层
                        //如果是第一级就交换SHORT和LONG
                        myTableName = if (myTableName != "SHORT" && myTableName != "LONG")
                            if (myTableName.startsWith('S')) "SHORT" else "LONG"
                        else if (myTableName == "SHORT") "LONG" else "SHORT"
                        longClickCheck = true
                        val intent = Intent(this, InputWindows::class.java)
                        startService(intent)
                        restartCheck = true
                    }
                }
            }
        }
        return true
    }

}