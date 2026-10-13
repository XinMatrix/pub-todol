package com.todol.mainPage.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.permissionx.guolindev.PermissionX
import com.todol.BaseActivity
import com.todol.R
import com.todol.ShowInfo
import com.todol.User
import com.todol.createFileInDownloadsOnText
import com.todol.database.MyDatabaseHelper
import com.todol.filePerDay
import com.todol.getGlobalData
import com.todol.getTableNames
import com.todol.importDataFromFile
import com.todol.insertUser
import com.todol.mainPage.adapter.MyFirstAdapter
import com.todol.myToast
import com.todol.myToastLong
import com.todol.putGlobalData
import com.todol.queryDatabase
import com.todol.queryUserStar
import com.todol.setViewVisible
import com.todol.startAnim
import com.todol.startMyDialog
import com.todol.windows.activity.FILE_CHOOSER_CHECK
import com.todol.windows.service.FloatingBallService
import com.todol.windows.service.INPUT_WINDOWS_POSITION_X
import com.todol.windows.service.INPUT_WINDOWS_POSITION_Y
import com.todol.windows.service.INPUT_WINDOWS_RUNNING_CHECK
import com.todol.windows.service.InputWindows
import com.todol.windows.service.SERVICE_RUNNING_CHECK
import com.todol.windows.service.ScreenOrientationReceiver
import com.todol.windows.service.addAniCheck
import com.todol.windows.service.itemList
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

private var drawerLayoutCheck = false

var ANI_AVAILABLE_CHECK = false //默认加载动画——这个不要改了，用于控制全局设置
var HINT_CHECK = true //默认每日隐藏数据

var CHOOSE_POSITION = 0 //用于标记选择位置

var SHOW_STRING = "" //用于展示提示文本

var FLOATING_MOVE_X = 0 //用于记录移动位置
var FLOATING_MOVE_Y = 0 //用于记录移动位置

val WINDOWS_WIDTH_LIST = listOf(600, 650, 700, 750, 800, 850, 900, 950, 1000, 1050)
var WINDOWS_WIDTH_INDEX = -1 //默认900
val WINDOWS_HEIGHT_LIST = listOf(800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700)
var WINDOWS_HEIGHT_INDEX = -1 //默认45
val TOUCH_HEIGHT_LIST = listOf(40, 45, 50, 55, 60)
var TOUCH_HEIGHT_INDEX = -1
val INPUT_SIZE_LIST = listOf(10F, 12F, 14F, 16F, 18F, 20F, 22F, 24F, 26F, 28F, 30F)
var INPUT_SIZE_INDEX = -1 //默认18F
val BUTTON_HEIGHT_LIST = listOf(25, 30, 35, 40, 45)
var BUTTON_HEIGHT_INDEX = -1

val FLOAT_SIZE_LIST = listOf(35, 40, 45, 50, 55)
//var FLOAT_SIZE_INDEX = -1
var FLOAT_SIZE_INDEX = 1  //部分机型不兼容，故固定为 1

var THEME_INDEX = -1 //定义主题
val THEME_LIST = listOf("红色", "橙色", "黄色", "绿色", "青色", "蓝色", "紫色", "深色")

var WINDOWS_ALPHA = -1F

private var scrollY = 0

class Main : BaseActivity() {

    private lateinit var floatingBallService: Intent

    //数据库
    private lateinit var dbHelper: MyDatabaseHelper
    private lateinit var db: SQLiteDatabase

    private lateinit var errorTV: TextView

    private lateinit var topLL: LinearLayout
    private lateinit var listLL: LinearLayout

    private lateinit var recyclerView: RecyclerView

    private lateinit var searchET: EditText

    //抽屉
    private lateinit var infoIV: ImageView
    private lateinit var settingsIV: ImageView
    private lateinit var drawerLayout: DrawerLayout

    //动画开启、关闭
    private lateinit var aniCB: CheckBox

    //服务开启、关闭
    private lateinit var serviceCB: CheckBox

    //每日隐藏笔记开启、关闭
    private lateinit var hintCB: CheckBox

    //宽度设置
    private lateinit var widthLastIV: ImageView
    private lateinit var widthTV: TextView
    private lateinit var widthNextIV: ImageView

    //高度设置
    private lateinit var heightLastIV: ImageView
    private lateinit var heightTV: TextView
    private lateinit var heightNextIV: ImageView

    //透明度设置
    private lateinit var alphaLastIV: ImageView
    private lateinit var alphaTV: TextView
    private lateinit var alphaNextIV: ImageView

    //按钮区高度
    private lateinit var buttonHeightLastIV: ImageView
    private lateinit var buttonHeightTV: TextView
    private lateinit var buttonHeightNextIV: ImageView

    //触控板高度
    private lateinit var touchHeightLastIV: ImageView
    private lateinit var touchHeightTV: TextView
    private lateinit var touchHeightNextIV: ImageView

    //输入框大小
    private lateinit var inputSizeLastIV: ImageView
    private lateinit var inputSizeTV: TextView
    private lateinit var inputSizeNextIV: ImageView

    //悬浮球大小
    private lateinit var floatSizeLastIV: ImageView
    private lateinit var floatSizeTV: TextView
    private lateinit var floatSizeNextIV: ImageView

    //主题配色设置
    private lateinit var themeLastIV: ImageView
    private lateinit var themeTV: TextView
    private lateinit var themeNextIV: ImageView

    //输入悬浮窗位置复原
    private lateinit var resetPositionCV: CardView

    //右侧滑动位置
    private lateinit var scrollView: ScrollView

    private lateinit var importDataTV: TextView
    private lateinit var exportDataTV: TextView
    private lateinit var starTV: TextView
    private lateinit var statisticTV: TextView

    private lateinit var contactMeTV: TextView

    private var showInfoList = ArrayList<ShowInfo>()

    private lateinit var tempItemList: MutableList<User>

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initListeners()
        initGlobalData()

        //YYDS 原来可以直接在这里启动
        //监听屏幕方向变化
        run {
            val receiver = ScreenOrientationReceiver()
            registerReceiver(receiver, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
        }

        dbHelper = MyDatabaseHelper(this)
        db = dbHelper.writableDatabase

        //初始化赋值
        ANI_AVAILABLE_CHECK = getGlobalData(this, "AniCheck", Boolean) as Boolean

        HINT_CHECK = !(getGlobalData(this, "HINT_CHECK", Boolean) as Boolean) //默认拿true

        //数据每天归档
        filePerDay(this, db, dbHelper)

        //TODO ArrayList是java类 但较为过时 可能会导致性能损失 最终需要修改
        val finalOutcome = getTableNames(dbHelper).filter { it.startsWith("_") && !it.contains("SHORT") && !it.contains("LONG") }
        //不要修改！ 不要修改！ 不要修改！
        if (finalOutcome.size == 0) showInfoList.add(ShowInfo("时间", 0, "内容", "", 0L, "", "", ""))
        else {
            val tempList = mutableListOf<String>()
            for (j in finalOutcome.indices)
                if (finalOutcome[j].isNotEmpty()) {
                    tempItemList = queryDatabase(db, finalOutcome[j])
                    //endTime只需要日期 不需要全部
                    for (i in 0 until tempItemList.size) {
                        val temp = tempItemList[i].endTime.split("_").toTypedArray()
                        //防止日期相同
                        if ("${temp[0]}年${temp[1]}月${temp[2]}日" !in tempList) {
                            showInfoList.add(
                                ShowInfo(
                                    "${temp[0]}年${temp[1]}月${temp[2]}日",
                                    tempItemList.size,
                                    tempItemList[tempItemList.size - 1].data,
                                    tempItemList[tempItemList.size - 1].startTime,
                                    0L,
                                    "",
                                    tempItemList[tempItemList.size - 1].filePath,
                                    tempItemList[tempItemList.size - 1].uid,
                                )
                            )
                            tempList.add("${temp[0]}年${temp[1]}月${temp[2]}日")
                        }
                    }
                }
        }
        val backAniCheck = intent.getBooleanExtra("AniCheck", true) //返回时不加载动画
        val adapter = MyFirstAdapter(this, showInfoList.asReversed().toMutableList() as ArrayList<ShowInfo>, backAniCheck)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.scrollToPosition(CHOOSE_POSITION)
        scrollView.post {
            scrollView.scrollTo(0, scrollY)
        }

    }

    private fun initGlobalData() {

        fun initSettings(
            name: String,
            lastIV: ImageView,
            nextIV: ImageView,
            textView: TextView,
            defaultValue: Int,
            list: List<Any>
        ): Int {
            val getValue = getGlobalData(this@Main, name, Int) as Int
            val temp = if (getValue < 0) defaultValue else getValue
            if (temp == 0) lastIV.visibility = View.INVISIBLE
            if (temp == list.size - 1) nextIV.visibility = View.INVISIBLE
            textView.text = list[temp].toString().substringBefore('.')
            return temp
        }

        WINDOWS_WIDTH_INDEX =
            initSettings("WINDOWS_WIDTH_INDEX", widthLastIV, widthNextIV, widthTV, 5, WINDOWS_WIDTH_LIST)

        WINDOWS_HEIGHT_INDEX =
            initSettings("WINDOWS_HEIGHT_INDEX", heightLastIV, heightNextIV, heightTV, 2, WINDOWS_HEIGHT_LIST)

        TOUCH_HEIGHT_INDEX =
            initSettings("TOUCH_HEIGHT_INDEX", touchHeightLastIV, touchHeightNextIV, touchHeightTV, 1, TOUCH_HEIGHT_LIST)

        INPUT_SIZE_INDEX =
            initSettings("INPUT_SIZE_INDEX", inputSizeLastIV, inputSizeNextIV, inputSizeTV, 4, INPUT_SIZE_LIST)

        BUTTON_HEIGHT_INDEX =
            initSettings("BUTTON_HEIGHT_INDEX", buttonHeightLastIV, buttonHeightNextIV, buttonHeightTV, 1, BUTTON_HEIGHT_LIST)

        FLOAT_SIZE_INDEX =
            initSettings("FLOAT_SIZE_INDEX", floatSizeLastIV, floatSizeNextIV, floatSizeTV, 1, FLOAT_SIZE_LIST)

        val getAlpha = getGlobalData(this@Main, "WINDOWS_ALPHA", Float) as Float
        WINDOWS_ALPHA = if (getAlpha == 0F) 0.9F else getAlpha //好像只能用Double，Float转化失败
        themeTV.text = THEME_LIST[THEME_INDEX]
        if (THEME_INDEX == 0) themeLastIV.visibility = View.INVISIBLE
        if (THEME_INDEX == THEME_LIST.size - 1) themeNextIV.visibility = View.INVISIBLE
        alphaTV.text = WINDOWS_ALPHA.toString()

        val nowDate = LocalDate.now()
        val today = "${nowDate.year}_${nowDate.monthValue}_${nowDate.dayOfMonth}"

        if ((getGlobalData(this, "FirstUse", String) as String).isEmpty()) //记录第一次安装的日期
            putGlobalData(this, "FirstUse", today)

        val recordToday = getGlobalData(this, "RecordToday", String) as String
        if (today !in recordToday) putGlobalData(this, "RecordToday", "$today $recordToday")
        //[2023_12_30, 2023_12_29, ]
        //存入每天的日期，如果存在就不存了 2023_12_24 2023_12_25
    }

    private fun initViews() {
        errorTV = findViewById(R.id.textViewError)
        topLL = findViewById(R.id.mainTopLL)
        listLL = findViewById(R.id.mainListLL)
        recyclerView = findViewById(R.id.mainRecycleView)
        searchET = findViewById(R.id.mainSearchET)
        errorTV.isVisible = false

        infoIV = findViewById(R.id.mainInfoIV)
        settingsIV = findViewById(R.id.mainSettingsIV)
        drawerLayout = findViewById(R.id.mainDrawerLayout)

        aniCB = findViewById(R.id.mainAniCB)
        serviceCB = findViewById(R.id.mainServiceCB)
        hintCB = findViewById(R.id.mainHintCB)

        widthLastIV = findViewById(R.id.mainWidthLastIV)
        widthTV = findViewById(R.id.mainWidthTV)
        widthNextIV = findViewById(R.id.mainWidthNextIV)

        heightLastIV = findViewById(R.id.mainHeightLastIV)
        heightTV = findViewById(R.id.mainHeightTV)
        heightNextIV = findViewById(R.id.mainHeightNextIV)

        alphaLastIV = findViewById(R.id.mainAlphaLastIV)
        alphaTV = findViewById(R.id.mainAlphaTV)
        alphaNextIV = findViewById(R.id.mainAlphaNextIV)

        buttonHeightLastIV = findViewById(R.id.mainButtonHeightLastIV)
        buttonHeightTV = findViewById(R.id.mainButtonHeightTV)
        buttonHeightNextIV = findViewById(R.id.mainButtonHeightNextIV)

        touchHeightLastIV = findViewById(R.id.mainTouchHeightLastIV)
        touchHeightTV = findViewById(R.id.mainTouchHeightTV)
        touchHeightNextIV = findViewById(R.id.mainTouchHeightNextIV)

        inputSizeLastIV = findViewById(R.id.mainInputSizeLastIV)
        inputSizeTV = findViewById(R.id.mainInputSizeTV)
        inputSizeNextIV = findViewById(R.id.mainInputSizeNextIV)

        floatSizeLastIV = findViewById(R.id.mainFloatSizeLastIV)
        floatSizeTV = findViewById(R.id.mainFloatSizeTV)
        floatSizeNextIV = findViewById(R.id.mainFloatSizeNextIV)

        themeLastIV = findViewById(R.id.mainThemeLastIV)
        themeTV = findViewById(R.id.mainThemeTV)
        themeNextIV = findViewById(R.id.mainThemeNextIV)

        resetPositionCV = findViewById(R.id.mainResetInputWindowsPositionCV)

        scrollView = findViewById(R.id.mainEndSV)

        importDataTV = findViewById(R.id.mainImportDataCV)
        exportDataTV = findViewById(R.id.mainExportDataTV)
        starTV = findViewById(R.id.mainStarTV)
        statisticTV = findViewById(R.id.mainStatisticTV)

        contactMeTV = findViewById(R.id.mainContactMeTV)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initListeners() {
        errorTV.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (scrollView.scrollY == 0) return@addOnScrollChangedListener //不能记录0
            scrollY = scrollView.scrollY //记录垂直滑动位置
        }

        if (drawerLayoutCheck) {
            drawerLayout.openDrawer(GravityCompat.END)
            drawerLayoutCheck = false
        }

        searchET.setOnFocusChangeListener { _, _ ->
            searchET.onFocusChangeListener = null
            searchET.clearFocus()
            val intent = Intent(this, Search::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        infoIV.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        settingsIV.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        aniCB.isChecked = !ANI_AVAILABLE_CHECK
        aniCB.setOnCheckedChangeListener { _, isChecked -> //这里逻辑有点绕，别动就好
            putGlobalData(this, "AniCheck", !isChecked)
            ANI_AVAILABLE_CHECK = !isChecked //忘记实时修改了
        }

        serviceCB.isChecked = SERVICE_RUNNING_CHECK //你看，没那么难吧
        serviceCB.setOnCheckedChangeListener { _, isChecked ->
            if (INPUT_WINDOWS_RUNNING_CHECK) { //防止运行中关闭
                myToast(this@Main, "悬浮窗运行中无法关闭")
                serviceCB.isChecked = true
                return@setOnCheckedChangeListener
            }
            val intent = Intent(this, FloatingBallService::class.java)
            if (isChecked) startService(intent) else stopService(intent)
        }

        hintCB.isChecked = HINT_CHECK
        hintCB.setOnCheckedChangeListener { _, isChecked ->
            putGlobalData(this, "HINT_CHECK", !isChecked)
            HINT_CHECK = isChecked //忘记实时修改了
        }

        //控制悬浮窗宽度
        fun restartInputWindows() { //重启服务 如果悬浮窗打开则加载悬浮窗，否则重启悬浮球
            if (INPUT_WINDOWS_RUNNING_CHECK) {
                val intent = Intent(this, InputWindows::class.java)
                stopService(intent)
                startService(intent)
                addAniCheck = false
            } else {
                val intent = Intent(this, FloatingBallService::class.java)
                stopService(intent)
                startService(intent)
            }
        }

        //修改 InputWindows And FloatWindows's Alpha
        alphaLastIV.setOnClickListener { //这种不应该设置动画，会导致 INVISIBLE 失效
            alphaNextIV.isVisible = true
            val currentValue = BigDecimal(alphaTV.text.toString())
            val newValue = currentValue.subtract(BigDecimal("0.1")).setScale(1, BigDecimal.ROUND_HALF_UP)
            alphaTV.text = newValue.toString()
            WINDOWS_ALPHA = alphaTV.text.toString().toFloat()
            putGlobalData(this@Main, "WINDOWS_ALPHA", WINDOWS_ALPHA)
            restartInputWindows()
            if (alphaTV.text.toString() == "0.1") it.visibility = View.INVISIBLE
        }

        alphaNextIV.setOnClickListener { //这种不应该设置动画，会导致 INVISIBLE 失效
            alphaLastIV.isVisible = true
            val currentValue = BigDecimal(alphaTV.text.toString())
            val newValue = currentValue.add(BigDecimal("0.1")).setScale(1, BigDecimal.ROUND_HALF_UP)
            alphaTV.text = newValue.toString()
            WINDOWS_ALPHA = alphaTV.text.toString().toFloat()
            putGlobalData(this@Main, "WINDOWS_ALPHA", WINDOWS_ALPHA)
            restartInputWindows()
            if (alphaTV.text.toString() == "1.0") it.visibility = View.INVISIBLE
        }

        fun initSettingListeners(lastIV: ImageView, nextIV: ImageView, name: String, textView: TextView) {
            lastIV.setOnClickListener { //这种不应该设置动画，会导致 INVISIBLE 失效
                nextIV.isVisible = true
                when (name) {
                    "WINDOWS_WIDTH_INDEX" -> {
                        val tempIndex = --WINDOWS_WIDTH_INDEX
                        textView.text = WINDOWS_WIDTH_LIST[tempIndex].toString()
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == 0) it.visibility = View.INVISIBLE
                    }

                    "WINDOWS_HEIGHT_INDEX" -> {
                        val tempIndex = --WINDOWS_HEIGHT_INDEX
                        textView.text = WINDOWS_HEIGHT_LIST[tempIndex].toString()
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == 0) it.visibility = View.INVISIBLE
                    }

                    "BUTTON_HEIGHT_INDEX" -> {
                        val tempIndex = --BUTTON_HEIGHT_INDEX
                        textView.text = BUTTON_HEIGHT_LIST[tempIndex].toString()
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == 0) it.visibility = View.INVISIBLE
                    }

                    "TOUCH_HEIGHT_INDEX" -> {
                        val tempIndex = --TOUCH_HEIGHT_INDEX
                        textView.text = TOUCH_HEIGHT_LIST[tempIndex].toString()
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == 0) it.visibility = View.INVISIBLE
                    }

                    "INPUT_SIZE_INDEX" -> {
                        val tempIndex = --INPUT_SIZE_INDEX
                        textView.text = INPUT_SIZE_LIST[tempIndex].toString().substringBefore('.')
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == 0) it.visibility = View.INVISIBLE
                    }

                    "FLOAT_SIZE_INDEX" -> {
                        val tempIndex = --FLOAT_SIZE_INDEX
                        textView.text = FLOAT_SIZE_LIST[tempIndex].toString()
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == 0) it.visibility = View.INVISIBLE
                    }
                }
                restartInputWindows()
            }
            nextIV.setOnClickListener { //这种不应该设置动画，会导致 INVISIBLE 失效
                lastIV.isVisible = true
                when (name) {
                    "WINDOWS_WIDTH_INDEX" -> {
                        val tempIndex = ++WINDOWS_WIDTH_INDEX
                        val list = WINDOWS_WIDTH_LIST
                        textView.text = list[tempIndex].toString()
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == list.size - 1) it.visibility = View.INVISIBLE
                    }

                    "WINDOWS_HEIGHT_INDEX" -> {
                        val tempIndex = ++WINDOWS_HEIGHT_INDEX
                        val list = WINDOWS_HEIGHT_LIST
                        textView.text = list[tempIndex].toString()
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == list.size - 1) it.visibility = View.INVISIBLE
                    }

                    "BUTTON_HEIGHT_INDEX" -> {
                        val tempIndex = ++BUTTON_HEIGHT_INDEX
                        val list = BUTTON_HEIGHT_LIST
                        textView.text = list[tempIndex].toString()
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == list.size - 1) it.visibility = View.INVISIBLE
                    }

                    "TOUCH_HEIGHT_INDEX" -> {
                        val tempIndex = ++TOUCH_HEIGHT_INDEX
                        val list = TOUCH_HEIGHT_LIST
                        textView.text = list[tempIndex].toString()
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == list.size - 1) it.visibility = View.INVISIBLE
                    }

                    "INPUT_SIZE_INDEX" -> {
                        val tempIndex = ++INPUT_SIZE_INDEX
                        val list = INPUT_SIZE_LIST
                        textView.text = list[tempIndex].toString().substringBefore('.')
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == list.size - 1) it.visibility = View.INVISIBLE
                    }

                    "FLOAT_SIZE_INDEX" -> {
                        val tempIndex = ++FLOAT_SIZE_INDEX
                        val list = FLOAT_SIZE_LIST
                        textView.text = list[tempIndex].toString()
                        putGlobalData(this@Main, name, tempIndex)
                        if (tempIndex == list.size - 1) it.visibility = View.INVISIBLE
                    }
                }
                restartInputWindows()
            }
        }

        initSettingListeners(widthLastIV, widthNextIV, "WINDOWS_WIDTH_INDEX", widthTV)

        initSettingListeners(heightLastIV, heightNextIV, "WINDOWS_HEIGHT_INDEX", heightTV)

        initSettingListeners(buttonHeightLastIV, buttonHeightNextIV, "BUTTON_HEIGHT_INDEX", buttonHeightTV)

        initSettingListeners(touchHeightLastIV, touchHeightNextIV, "TOUCH_HEIGHT_INDEX", touchHeightTV)

        initSettingListeners(inputSizeLastIV, inputSizeNextIV, "INPUT_SIZE_INDEX", inputSizeTV)

        initSettingListeners(floatSizeLastIV, floatSizeNextIV, "FLOAT_SIZE_INDEX", floatSizeTV)

        //绿色 橙色 深色
        themeLastIV.setOnClickListener {
            themeNextIV.isVisible = true
            themeTV.text = THEME_LIST[--THEME_INDEX]
            putGlobalData(this, "THEME_INDEX", THEME_INDEX)
            val intent = Intent(this, Main::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            restartInputWindows()
            drawerLayoutCheck = true //自动弹开右侧
            if (THEME_INDEX == 0) themeLastIV.visibility = View.INVISIBLE
            finish()
        }

        themeNextIV.setOnClickListener {
            themeLastIV.isVisible = true
            themeTV.text = THEME_LIST[++THEME_INDEX]
            putGlobalData(this, "THEME_INDEX", THEME_INDEX)
            val intent = Intent(this, Main::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
            restartInputWindows()
            drawerLayoutCheck = true //自动弹开右侧
            if (THEME_INDEX == THEME_LIST.size - 1) themeNextIV.visibility = View.INVISIBLE
            finish()
        }

        //重置悬浮窗位置
        resetPositionCV.setOnClickListener { view ->
            startAnim(this, view) //resetInputWindowsPositionCV
            INPUT_WINDOWS_POSITION_X = 0
            INPUT_WINDOWS_POSITION_Y = 0
            myToast(this, "已重置")
        }

        importDataTV.setOnClickListener { view ->  //导入数据 更新表
            startAnim(this, view) //importCV
            val title = "导入笔记 ?"
            val content =
                "导入会新增数据且无法撤销\n将旧设备 下载/TODOL_SP_DATA 文件夹中的所有内容复制到新设备 下载/TODOL_SP_DATA 中\n导入操作可重复进行，请确保正确使用"
            startMyDialog(this, title, content) {
                val filePath = run {
                    val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    downloadDirectory.absolutePath + "/TODOL_SP_DATA/EXPORT.txt"
                }
                val importData = importDataFromFile(filePath)
                val userDataMap = mutableMapOf<String, MutableList<User>>()
                if (importData != null) {
                    userDataMap.putAll(importData)
                    val userDataKeys = userDataMap.map { it.key } // [SHORT, LONG]
                    for (tableName in userDataKeys) for (user in userDataMap[tableName]!!)
                        insertUser(
                            //导入旧表
                            db,
                            tableName,
                            user.uid,
                            user.data,
                            user.state,
                            user.remindTime,
                            user.child,
                            user.startTime,
                            user.endTime,
                            user.place,
                            user.filePath,
                            user.dayCheck,
                            user.starCheck,
                        ).also { itemList = queryDatabase(db, tableName) }
                    myToastLong(this, "导入成功\n更改文件名以备份")
                    //手动改名算了
                } else myToastLong(this, "导入失败，请检查导入文件是否正确")
            }
        }

        exportDataTV.setOnClickListener { view -> //导出数据
            startAnim(this, view) //exportCV
            startMyDialog(this, "导出笔记 ?", "导出路径\n下载/TODOL_SP_DATA/EXPORT.txt") {
                val tableNames = getTableNames(dbHelper).filter { it.startsWith('S') || it.startsWith('L') } //过滤系统表
                val userDataMap = mutableMapOf<String, MutableList<User>>()
                for (i in tableNames) userDataMap[i] = queryDatabase(db, i)
                createFileInDownloadsOnText(this, userDataMap) { isSuccess, _ ->
                    if (isSuccess) myToastLong(this, "导出成功！")
                    else myToastLong(this, "导出失败！")
                }
            }
        }

        starTV.setOnClickListener {
            startAnim(this, it)
            val showInfoList = queryUserStar(db)
            if (showInfoList.isEmpty()) {
                myToastLong(this, "暂无收藏笔记")
                return@setOnClickListener
            }
            val tempTitle = showInfoList.first().endTime.split("_").toTypedArray()
            val title = "${tempTitle[0]}年${tempTitle[1]}月${tempTitle[2]}日"
            val intent = Intent(this, Second::class.java)
            intent.putExtra("title", title)
            intent.putExtra("number", showInfoList.size.toString())
            intent.putExtra("starCheck", true)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        statisticTV.setOnClickListener {  //查看统计数据
            startAnim(this, it)
            val intent = Intent(this, Statistic::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        contactMeTV.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:15229220972")
            startActivity(intent)
        }
    }

    //这里控制悬浮窗
    override fun onStart() {
        super.onStart()

//        throw Exception("自定义测试崩溃信息")

        //如果用户本来就打开了怎么办?
        if (FILE_CHOOSER_CHECK) { //避免加载时出现首页，我直接返回
            finish()
            overridePendingTransition(0, 0)
        }
        //默认显示
        setViewVisible(listOf(errorTV), true)
        setViewVisible(listOf(topLL, listLL), false)
        PermissionX.init(this).permissions(
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).request { allGranted, _, _ ->
            if (allGranted) {
                run { //初始化文件夹
                    val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val filePath = downloadDirectory.absolutePath + "/TODOL_SP_DATA"
                    val tempFile = File(filePath)
                    if (!tempFile.exists()) { //仅当文件夹不存在时创建
                        tempFile.mkdirs()
                        val readme = File("$filePath/README.txt")
                        if (!readme.exists()) readme.createNewFile()
                        val image = File("$filePath/image")
                        if (!image.exists()) image.mkdir()
                        val audio = File("$filePath/audio")
                        if (!audio.exists()) audio.mkdir()
                        val video = File("$filePath/video")
                        if (!video.exists()) video.mkdir()
                        readme.writeText("这个文件夹用来存储引用文件、导出数据文件、导入数据文件(需要手动转移)，如无必要请勿删除，如果引用文件内存过大可在TODOL中逐个删除")
                    }
                }
                setViewVisible(listOf(topLL, listLL), true)
                setViewVisible(listOf(errorTV), false)
                run { //这里定义悬浮窗
                    if (!SERVICE_RUNNING_CHECK) return@run //控制悬浮窗存在时不要出现球形悬浮窗
                    floatingBallService = Intent(this, FloatingBallService::class.java)
                    startService(floatingBallService)
                }
            }
        }
    }

    //为什么onRestart需要重启服务
    override fun onRestart() {
        super.onRestart()
        //重置监听
        searchET.setOnFocusChangeListener { _, _ ->
            searchET.onFocusChangeListener = null
            searchET.clearFocus()
            val intent = Intent(this, Search::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        recyclerView.scrollToPosition(CHOOSE_POSITION)
    }

    override fun onBackPressed() {
        val isDrawerOpenStart = drawerLayout.isDrawerOpen(GravityCompat.START)
        val isDrawerOpenEnd = drawerLayout.isDrawerOpen(GravityCompat.END)
        if (isDrawerOpenStart) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }
        if (isDrawerOpenEnd) {
            drawerLayout.closeDrawer(GravityCompat.END)
            return
        }
        super.onBackPressed()
        finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }

}
