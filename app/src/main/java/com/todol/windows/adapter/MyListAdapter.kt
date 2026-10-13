package com.todol.windows.adapter

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.CompoundButtonCompat
import androidx.recyclerview.widget.RecyclerView
import com.todol.R
import com.todol.calculateTimeDifference
import com.todol.copyToClipboard
import com.todol.createSheetIfNotExists
import com.todol.database.MyDatabaseHelper
import com.todol.deleteByData
import com.todol.dropKeyBoard
import com.todol.getClipBoardContent
import com.todol.initImage
import com.todol.initTVFile
import com.todol.insertUser
import com.todol.isAudioFile
import com.todol.isImageFile
import com.todol.isNotFuture
import com.todol.isSon
import com.todol.isVideoFile
import com.todol.mainPage.activity.CHOOSE_POSITION
import com.todol.mainPage.activity.INPUT_SIZE_INDEX
import com.todol.mainPage.activity.INPUT_SIZE_LIST
import com.todol.mainPage.activity.SHOW_STRING
import com.todol.mainPage.activity.THEME_INDEX
import com.todol.myTableName
import com.todol.myVibrate
import com.todol.queryDatabase
import com.todol.setViewVisible
import com.todol.startAnim
import com.todol.swapToBottom
import com.todol.swapToTop
import com.todol.updateChild
import com.todol.updateDayCheck
import com.todol.updateEndTime
import com.todol.updateInputData
import com.todol.updateRemind
import com.todol.updateState
import com.todol.windows.activity.FILE_CHOOSER_CHECK
import com.todol.windows.activity.FileChooser
import com.todol.windows.service.DatePicker
import com.todol.windows.service.INPUT_WINDOWS_RUNNING_CHECK
import com.todol.windows.service.InputWindows
import com.todol.windows.service.TimePicker
import com.todol.windows.service.addAniCheck
import com.todol.windows.service.finishAniCheck
import com.todol.windows.service.itemList
import com.todol.windows.service.setPlaceAniCheck
import com.todol.windows.service.topAniCheck
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime

var choosePosition = -1 //默认-1 用于标记用户焦点EditText

var tempFileUrl = ""

class MyListAdapter(
    private val context: Context,
    private val showTextView: TextView,
    private val topCheck: Boolean,
    private val topTV: TextView,
    private val fileTV: TextView,
    private val fileSpareLL: LinearLayout,
    private val aniIV: ImageView,
) : RecyclerView.Adapter<MyListAdapter.MyViewHolder>() {

    //这个没法onDestroy
    private val db = MyDatabaseHelper(context).writableDatabase

    private val myDate = "${LocalDate.now().year}_${LocalDate.now().monthValue}_${LocalDate.now().dayOfMonth}_"

    //不加这行代码真的乱成一锅粥
    override fun getItemViewType(position: Int) = position.hashCode()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) = holder.bind(position)

    override fun getItemCount() = itemList.size

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val editText: EditText = itemView.findViewById(R.id.ListItemET)
        val clockIB: ImageView = itemView.findViewById(R.id.listItemClockIB)
        private val checkBox: CheckBox = itemView.findViewById(R.id.listItemCB)

        private val timeTV: TextView = itemView.findViewById(R.id.listItemTimeTV)
        private val placeTV: TextView = itemView.findViewById(R.id.listItemPlaceTV)
        private val remindLL: LinearLayout = itemView.findViewById(R.id.listItemRemindLL)

        private val aniLL: LinearLayout = itemView.findViewById(R.id.listItemLL)

        //缩略图
        private val smallIV: ImageView = itemView.findViewById(R.id.listItemSmallIV)
        private val smallTV: TextView = itemView.findViewById(R.id.listItemSmallTV)

        @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
        fun bind(position: Int) {

            run { //设置主题
                val tempThemeId = when (THEME_INDEX) {
                    0 -> R.color.redFirstColor
                    1 -> R.color.orangeFirstColor
                    2 -> R.color.yellowFirstColor
                    3 -> R.color.greenFirstColor
                    4 -> R.color.cyanFirstColor
                    5 -> R.color.blueFirstColor
                    6 -> R.color.purpleFirstColor
                    else -> R.color.darkFirstColor
                }
                val color = ContextCompat.getColor(context, tempThemeId)
                ViewCompat.setBackgroundTintList(editText, ColorStateList.valueOf(color))
                CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(color))
                editText.textSize = INPUT_SIZE_LIST[INPUT_SIZE_INDEX] //设置EditText字号大小 10F -> 30F
            }

            val tempCornerBackground = when (THEME_INDEX) {
                0 -> listOf(R.drawable.rounded_corner_red_big, R.color.redFirstColor, R.drawable.rounded_corner_red_small)
                1 -> listOf(R.drawable.rounded_corner_orange_big, R.color.orangeFirstColor, R.drawable.rounded_corner_orange_small)
                2 -> listOf(R.drawable.rounded_corner_yellow_big, R.color.yellowFirstColor, R.drawable.rounded_corner_yellow_small)
                3 -> listOf(R.drawable.rounded_corner_green_big, R.color.greenFirstColor, R.drawable.rounded_corner_green_small)
                4 -> listOf(R.drawable.rounded_corner_cyan_big, R.color.cyanFirstColor, R.drawable.rounded_corner_cyan_small)
                5 -> listOf(R.drawable.rounded_corner_blue_big, R.color.blueFirstColor, R.drawable.rounded_corner_blue_small)
                6 -> listOf(R.drawable.rounded_corner_purple_big, R.color.purpleFirstColor, R.drawable.rounded_corner_purple_small)
                else -> listOf(R.drawable.rounded_corner_dark_big, R.color.darkFirstColor, R.drawable.rounded_corner_dark_small)
            }
            smallTV.setBackgroundResource(tempCornerBackground.first())
            smallIV.setBackgroundResource(tempCornerBackground.last())
            smallTV.setTextColor(context.resources.getColor(tempCornerBackground[1], context.theme))

            //加载文件缩略图
            val filePath = itemList[position].filePath
            if (filePath.isNotEmpty())
                if (filePath.isImageFile()) initImage(context, filePath, smallIV) //加载特定条目的缩略图
                else if (filePath.isAudioFile()) initTVFile(filePath, smallTV)
                else if (filePath.isVideoFile()) { //用方形（因为视频不旋转）
                    smallTV.background = ContextCompat.getDrawable(context, tempCornerBackground.last())
                    initTVFile(filePath, smallTV)
                } //smallAudioTV和smallVideoTV可以只用一个TV

            //完成、添加、置顶的时候不应该有动画
            if (addAniCheck && topAniCheck && finishAniCheck && setPlaceAniCheck) startAnim(context, aniLL, 1)

            //设置CheckBox 的选择情况
            checkBox.isChecked = itemList[position].state == 1

            //有时间预约的会变成蓝色 选中时为灰色
            if (itemList[position].remindTime.isNotEmpty()) clockIB.setBackgroundResource(R.drawable.clock_background2)

            //字体颜色和背景相同我是服的

            //获取当前年月日并显示时间
            if (itemList[position].remindTime.isNotEmpty()) {
                val nowDate = LocalDate.now()
                val outputArray = itemList[position].remindTime
                    .replace("_", " ").split(" ").toTypedArray()
                //条件判定是今天  意味着不显示年月日
                var tempString = if (outputArray[0] == nowDate.year.toString()
                    && outputArray[1] == nowDate.monthValue.toString()
                    && outputArray[2] == nowDate.dayOfMonth.toString()
                ) "${outputArray[3]}:${outputArray[4]}" //年月日相同 只有小时数据
                else if (outputArray[0] == nowDate.year.toString()
                    && outputArray[1] == nowDate.monthValue.toString()
                ) "${outputArray[2]}日${outputArray[3]}:${outputArray[4]}" //年月相同 只有日、小时数据  日后面加'\n'
                else if (outputArray[0] == nowDate.year.toString())
                    "${outputArray[1]}月${outputArray[2]}日${outputArray[3]}:${outputArray[4]}" //年相同 只有月、日、小时数据
                else "${outputArray[0]}年${outputArray[1]}月${outputArray[2]}日${outputArray[3]}:${outputArray[4]}" //年月日都不同
                //年月日都不同时默认显示源数据 加零显示
                if (tempString.substringAfter(":") == "0" && tempString.length == 1)
                    tempString = tempString.replace("0", "00")
                else if (tempString.substringAfter(":").length == 1)
                    tempString = tempString.replace(":", ":0") //很聪明

                val differTime = calculateTimeDifference(outputArray.toList()) //秒钟
                if (differTime > 0) {
                    val today = nowDate.dayOfMonth //能简单就简单
                    val formedText = //这里格式化结果
                        if ("年" in tempString) tempString.subSequence(0, tempString.indexOf("年") + 1).toString()
                        else if ("月" in tempString) tempString.subSequence(0, tempString.indexOf("日") + 1).toString()
                        else if ("月" !in tempString && "日" in tempString &&
                            tempString.split("日").toTypedArray().first() == (today + 1).toString()
                        ) tempString.replace("${today + 1}日", "明天")
                        else if ("月" !in tempString && "日" in tempString &&
                            tempString.split("日").toTypedArray().first() == (today + 2).toString()
                        ) tempString.replace("${today + 2}日", "后天")
                        else tempString
                    timeTV.text = formedText
                } else if (differTime.isNotFuture()) timeTV.text = "已超时" //如果时间已经过去了就应该显示 进行中
                else timeTV.text = "时间到" //这里处理分钟时间相等的情况
            } else timeTV.text = "" //莫名其妙的Bug，还是会显示——这个是不是要isVisible = false 不然会占地方

            if (itemList[position].place.isNotEmpty()) placeTV.text = itemList[position].place else placeTV.text = ""

            //标记完成
            if (checkBox.isChecked) {
                editText.setTextColor(Color.GRAY)
                editText.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                editText.isEnabled = false
            } else {
                editText.setTextColor(Color.BLACK)
                editText.paintFlags = 0
            }

            //注意先后顺序
            if (topCheck) { //置顶标记 置顶时不能修改内容
                setViewVisible(listOf(placeTV, timeTV), false)
                editText.isEnabled = false
                editText.setTextColor(Color.GRAY)
            } else setViewVisible(listOf(placeTV, timeTV), true)

            // 设置 EditText 的文本和监听器 dayCheck默认为1，点击后为0
            if (myTableName == "SHORT" && itemList[position].dayCheck == "1") { //TODO 设置为不可完成，文字透明度降低
                editText.setText("点击查看")
                editText.setTextColor(Color.GRAY)
                checkBox.isEnabled = true
            } else {
                editText.setText(itemList[position].data)
                checkBox.isEnabled = true
            }

            //注意代码顺序顺序 含有子集区分显示
            if (itemList[position].child == 1) {
                var check = 0
                placeTV.isVisible = false
                val temp = queryDatabase(db, itemList[position].uid)
                for (i in 0 until temp.size) if (temp[i].state == 1) check++
                if (check == temp.size && temp.size != 0) timeTV.text = "已完成\n所有笔记"
                else if (temp.size == 0) timeTV.text = "暂无\n笔记"
                else timeTV.text = "${temp.size - check}  条\n笔记"
            }

            //处理 设置删除线 字体颜色
            //完成一次重启一次
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (itemList[position].data.isEmpty()) { //检查空数据
                    showTextView.text = "空数据不能完成!"
                    checkBox.isChecked = false
                    return@setOnCheckedChangeListener
                }
                finishAniCheck = false
                val sonTableName = myTableName.substringAfter("I")
//                myTableName = myTableName.substringAfter("I") //不能直接改名字，返回会出问题
                itemList = queryDatabase(db, sonTableName).reversed()
                val reversedId = itemList[position].id
                if (isChecked) {
                    if (itemList[position].data.isEmpty()) {
                        showTextView.text = "请填入笔记后标记完成"
                        checkBox.isChecked = false
                        return@setOnCheckedChangeListener
                    }
                    CHOOSE_POSITION = position
                    if (topCheck) { //置顶功能 //选中的id
                        itemList = queryDatabase(db, sonTableName).reversed()
                        swapToTop(db, sonTableName, itemList[position].id)
                        Handler(context.mainLooper).postDelayed({
                            //启动的前提是还在运行 Nice
                            if (!INPUT_WINDOWS_RUNNING_CHECK) return@postDelayed
                            val intent = Intent(context, InputWindows::class.java)
                            context.startService(intent)
                        }, 500) //感觉慢一点好
                    } else { //完成功能  //为什么显示不出来了——因为没给宽度
                        run { //执行动画
                            aniIV.isVisible = true
                            val aniColor = when (THEME_INDEX) {
                                0 -> R.color.redSecondColor
                                1 -> R.color.orangeSecondColor
                                2 -> R.color.yellowSecondColor
                                3 -> R.color.greenSecondColor
                                4 -> R.color.cyanSecondColor
                                5 -> R.color.blueSecondColor
                                6 -> R.color.purpleSecondColor
                                else -> R.color.darkSecondColor
                            }
                            aniIV.setImageResource(aniColor)
                            val animation = TranslateAnimation(
                                Animation.RELATIVE_TO_PARENT, -1f,
                                Animation.RELATIVE_TO_PARENT, 1f,
                                Animation.RELATIVE_TO_PARENT, 0f,
                                Animation.RELATIVE_TO_PARENT, 0f
                            )
                            animation.duration = 1500 // 动画持续时间，单位为毫秒
                            animation.interpolator = AccelerateDecelerateInterpolator() // 插值器，可以根据需要选择合适的插值器
                            aniIV.startAnimation(animation)
                            Handler(context.mainLooper).postDelayed({ aniIV.isVisible = false }, 1500)
                        }
                        myVibrate(context, 200) //点击完成振动200ms 置顶不震动
                        val nowTime = LocalTime.now()
                        val nowDate = LocalDate.now()
                        val fullTime = "${nowDate.year}_${nowDate.monthValue}_${nowDate.dayOfMonth}_${nowTime.hour}_${nowTime.minute}"
                        updateEndTime(db, sonTableName, reversedId, fullTime)
                        editText.isEnabled = false
                        editText.setTextColor(Color.GRAY)
                        editText.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                        if (itemList[position].child == 1) { //说明有子集
                            val tempTableName = itemList[position].uid
                            val temp = queryDatabase(db, tempTableName)
                            for (id in temp.map { it.id }) {
                                updateState(db, tempTableName, id, 1)
                                updateEndTime(db, tempTableName, id, fullTime) //忘改endTime了 忘改tableName了
                            }
                        }
                        updateState(db, sonTableName, reversedId, 1)
                        swapToBottom(db, sonTableName, reversedId)
                        //延迟500ms执行能看到完成动画
                        Handler(context.mainLooper).postDelayed({
                            //启动的前提是还在运行 Nice
                            if (!INPUT_WINDOWS_RUNNING_CHECK) return@postDelayed
                            val intent = Intent(context, InputWindows::class.java)
                            context.startService(intent)
                        }, 1000) //感觉慢一点好
                    }
                } else {
                    updateEndTime(db, sonTableName, reversedId, "")
                    editText.setTextColor(Color.BLACK)
                    editText.paintFlags = 0
                    //放到最底下
                    updateState(db, sonTableName, reversedId, 0)
                    swapToTop(db, sonTableName, reversedId)
                    Handler(context.mainLooper).postDelayed({
                        val intent = Intent(context, InputWindows::class.java)
                        context.startService(intent)
                    }, 200)
                }
            }

            //数据为空的时候不显示不就好了 第一次进入没有记录，所以不写入地点信息
            clockIB.setOnClickListener { //点击进入——设置今天提醒日期
                itemList = queryDatabase(db, myTableName).reversed()
                //先结束再运行新的可以保持独立性
                run { //关闭服务
                    val stopIntent = Intent(context, InputWindows::class.java)
                    context.stopService(stopIntent)
                }
                run { //启动新服务
                    val intent = Intent(context, TimePicker::class.java)
                    intent.putExtra("timerPosition", position)
                    intent.putExtra("date", myDate)
                    context.startService(intent)
                }
            }

            clockIB.setOnLongClickListener { //长按进入——设置未来几天提醒日期
                itemList = queryDatabase(db, myTableName).reversed()
                updateRemind(db, myTableName, itemList[position].id, "")
                run { //关闭服务
                    val stopIntent = Intent(context, InputWindows::class.java)
                    context.stopService(stopIntent)
                }
                run {
                    val intent = Intent(context, DatePicker::class.java) //小心导包错误
                    intent.putExtra("timerPosition", position)
                    intent.putExtra("date", myDate)
                    context.startService(intent)
                }
                true
            }

            remindLL.setOnClickListener { //现在需要做的是点击更新数据
                if ("笔记" in timeTV.text) return@setOnClickListener
                itemList = queryDatabase(db, myTableName).reversed()
                run { //关闭服务
                    val stopIntent = Intent(context, InputWindows::class.java)
                    context.stopService(stopIntent)
                }
                run { //启动新服务
                    val intent = Intent(context, TimePicker::class.java)
                    intent.putExtra("timerPosition", position)
                    intent.putExtra("date", myDate)
                    intent.putExtra("timeTV", timeTV.text.toString())
                    intent.putExtra("placeTV", placeTV.text.toString())
                    context.startService(intent)
                }
            }

            remindLL.setOnLongClickListener {
                if ("笔记" in timeTV.text) return@setOnLongClickListener true
                itemList = queryDatabase(db, myTableName).reversed()
                updateRemind(db, myTableName, itemList[position].id, "") //只要长按进去remindTime就为空
                run { //关闭服务
                    val stopIntent = Intent(context, InputWindows::class.java)
                    context.stopService(stopIntent)
                }
                run { //启动新服务
                    val intent = Intent(context, DatePicker::class.java)
                    intent.putExtra("timerPosition", position)
                    intent.putExtra("date", itemList[position].remindTime.substringBeforeLast("_").substringBeforeLast("_") + "_")
                    intent.putExtra("placeTV", placeTV.text.toString())
                    context.startService(intent)
                }
                true
            }

            //是不是这个影响了复制粘贴
            editText.setOnLongClickListener {
                if (itemList[itemList.size - 1 - position].data.isEmpty()) return@setOnLongClickListener false
                //标记有子集
                updateChild(db, myTableName, itemList[position].id, 1)
                //创建表格      _2023_5_20_SHORT_2
                Log.d("myTableName A", myTableName)
                myTableName = "${myTableName}I${itemList[position].uid}" //Split By I
                Log.d("myTableName B", myTableName)
                //不能数字开头
                Log.d("myTableName C", itemList[position].uid)
                createSheetIfNotExists(db, itemList[position].uid)
                //感觉可以不用新建表，直接标记是哪个的数据行不行
                //能不能通过表内关系确定父级
                //瞬间刷新
                val intent = Intent(context, InputWindows::class.java)
                context.startService(intent)
                //设置文本
                showTextView.text = SHOW_STRING + itemList[position].data + "->"
                SHOW_STRING = showTextView.text.toString()
                true
            }

            // 设置触摸监听器
            var tempOffSet = 0
            var tempTwicePoint = false
            editText.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    // 获取焦点位置
                    val x = event.x
                    val y = event.y
                    // 获取焦点相对于字符的位置
                    val offset = editText.getOffsetForPosition(x, y)
                    tempTwicePoint = tempOffSet == offset
                    tempOffSet = offset
                }
                false
            }

            var clickCounter = 0
            editText.setOnClickListener {
                clickCounter++
                if (clickCounter > 1) return@setOnClickListener
                Handler(context.mainLooper).postDelayed({
                    if (!tempTwicePoint) return@postDelayed //如果是false则直接return 用于判定是不是同一个位置
                    if (clickCounter == 2) {
                        context.copyToClipboard(editText.text.toString(), "文本内容")
                        showTextView.text = "已复制到剪切板"
                    } else if (clickCounter == 3) {
                        if (myTableName == "SHORT") {
                            insertUser(
                                db,
                                "LONG",
                                itemList[position].uid,
                                itemList[position].data,
                                itemList[position].state,
                                itemList[position].remindTime,
                                itemList[position].child,
                                itemList[position].startTime,
                                itemList[position].endTime,
                                itemList[position].place,
                                itemList[position].filePath,
                                itemList[position].dayCheck,
                                itemList[position].starCheck,
                            )
                            deleteByData(db, "SHORT", itemList[position].data)
                            showTextView.text = "已移动到长期任务"
                            val intent = Intent(context, InputWindows::class.java)
                            context.startService(intent)
                        } else if (myTableName == "LONG") {
                            insertUser(
                                db,
                                "SHORT",
                                itemList[position].uid,
                                itemList[position].data,
                                itemList[position].state,
                                itemList[position].remindTime,
                                itemList[position].child,
                                itemList[position].startTime,
                                itemList[position].endTime,
                                itemList[position].place,
                                itemList[position].filePath,
                                itemList[position].dayCheck,
                                itemList[position].starCheck,
                            )
                            deleteByData(db, "LONG", itemList[position].data)
                            showTextView.text = "已移动到短期任务"
                            val intent = Intent(context, InputWindows::class.java)
                            context.startService(intent)
                        }
                    }
                    clickCounter = 0
                }, 500)
            }

            //粘贴按键、计时按钮出现
            editText.setOnFocusChangeListener { _, _ -> //应该只给主页的消息看
                if (myTableName == "SHORT" && itemList[position].dayCheck == "1") {
                    updateDayCheck(db, myTableName, itemList[position].id, "0")
                    startAnim(context, editText)
                    editText.setText(itemList[position].data)
                    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(editText.windowToken, 0)
                    dropKeyBoard(context, editText)
                    editText.setTextColor(Color.BLACK)
                }
                choosePosition = position
                fileTV.isVisible = true //在这里可视化的，现在还没有太好的办法
                fileSpareLL.isVisible = true
                if (editText.text.isNotEmpty() && timeTV.text.isEmpty()) clockIB.isVisible = true //初始化显示，防止TextWatcher检测不到
                //更新文本，设置监听
                val clipBoardData = getClipBoardContent(context)
                val tempText = if (clipBoardData.length > 2) clipBoardData.take(2) + ".." else clipBoardData
                topTV.text = "粘贴 ($tempText)"
                //把粘贴放里面就好了——解决了只给最后一个粘贴的问题
                topTV.setOnClickListener { //粘贴按键功能
                    editText.text.append(clipBoardData)
                    showTextView.text = "已粘贴"
                }
            }

            smallIV.setOnClickListener { //放大查看图片
                tempFileUrl = itemList[position].filePath
                val intent = Intent(context, InputWindows::class.java)
                context.startService(intent)
                addAniCheck = false
            }

            smallIV.setOnLongClickListener {  //重新选择图片
                choosePosition = position //Nice
                FILE_CHOOSER_CHECK = true
                val intent = Intent(context, FileChooser::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("Update", "Image")
                context.startActivity(intent)
                val stopIntent = Intent(context, InputWindows::class.java)
                context.stopService(stopIntent)
                true
            }

            val mediaPlayer = MediaPlayer()
            var isPlaying = false
            var rotationAnimator: ObjectAnimator? = null

            smallTV.setOnClickListener {
                if (filePath.isAudioFile()) {
                    if (isPlaying) {
                        mediaPlayer.reset() //暂停后只能重新播放
                        rotationAnimator?.cancel()
                    } else {
                        try {
                            mediaPlayer.setDataSource(filePath)
                            mediaPlayer.prepare()
                            mediaPlayer.start()
                            rotationAnimator = ObjectAnimator.ofFloat(smallTV, "rotation", 0f, 360f)
                            rotationAnimator?.duration = 8000 // 调整旋转一圈所需时间
                            rotationAnimator?.repeatCount = Animation.INFINITE
                            rotationAnimator?.interpolator = LinearInterpolator()
                            rotationAnimator?.start()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    isPlaying = !isPlaying
                } else if (filePath.isVideoFile()) { //Show New Views And Play Video
                    tempFileUrl = itemList[position].filePath
                    val intent = Intent(context, InputWindows::class.java)
                    context.startService(intent)
                    addAniCheck = false
                }
            }

            smallTV.setOnLongClickListener {  //重新选择图片
                choosePosition = position //Nice
                FILE_CHOOSER_CHECK = true
                val intent = Intent(context, FileChooser::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (filePath.isAudioFile()) intent.putExtra("Update", "Audio")
                else if (filePath.isVideoFile()) intent.putExtra("Update", "Video")
                context.startActivity(intent)
                val stopIntent = Intent(context, InputWindows::class.java)
                context.stopService(stopIntent)
                true
            }

            //滑动的时候取消文本监听
            editText.addTextChangedListener(object : TextWatcher {
                //文本没有变化为什么还会出发这个
                override fun afterTextChanged(inputText: Editable?) {
                    //这个是用来做什么的
                    if (inputText.toString() == "点击查看") return //这是个大问题
                    //如果已经定时了就不应该显示这个了
                    if (timeTV.text.isEmpty()) clockIB.isVisible = SHOW_STRING.isNotEmpty() //Nice
                    val tempTableName = if (myTableName.isSon()) myTableName.split('I').last() else myTableName
                    updateInputData(db, tempTableName, itemList[position].id, inputText.toString())
                    //性能有优化，但不多 如果获取了应该就能添加了吧
                    itemList = queryDatabase(db, tempTableName).reversed() //文字修改了就会修改数据——而不是按position加载
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            //CheckBox和EditText在滑动时应该都不能点击
            editText.isEnabled = false
            checkBox.isEnabled = false

            Handler(context.mainLooper).postDelayed({
                editText.isEnabled = true
                checkBox.isEnabled = true
            }, 100) //感觉差不多，GPT说正常人的反应时间大约为200ms
        }

    }

}
