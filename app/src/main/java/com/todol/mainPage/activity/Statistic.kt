package com.todol.mainPage.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.todol.BaseActivity
import com.todol.R
import com.todol.User
import com.todol.calculateDateDifference
import com.todol.calculateTimeDifference
import com.todol.database.MyDatabaseHelper
import com.todol.getGlobalData
import com.todol.getTableNames
import com.todol.isMyTable
import com.todol.queryDatabase
import com.todol.startAnim

class Statistic : BaseActivity() {

    private val myUserData = mutableListOf<MutableList<User>>()

    private lateinit var dbHelper: MyDatabaseHelper
    private lateinit var db: SQLiteDatabase

    private lateinit var emptyLL: LinearLayout

    private lateinit var recordDaysTV: TextView //记录天数
    private lateinit var recordAverageTV: TextView //平均每天
    private lateinit var recordNotesTV: TextView //记录笔记

    private lateinit var maxNotesDayTV: TextView //完成最多笔记的一天
    private lateinit var maxNotesDayCV: CardView //完成最多笔记的一天监听

    private lateinit var firstFinishNoteET: EditText //第一条完成的笔记
    private lateinit var maxLengthET: EditText //最长子串

    private lateinit var longestNoteET: EditText //最长的笔记
    private lateinit var oldestNoteET: EditText //最久的笔记

    private lateinit var installDayTV: TextView //安装时间
    private lateinit var continueDaysTV: TextView

    //动画效果 暂未实现功能
    private lateinit var recordDaysCV: CardView
    private lateinit var recordNotesCV: CardView
    private lateinit var continueDaysCV: CardView
    private lateinit var recordAverageCV: CardView
    private lateinit var installDayCV: CardView

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistic)
        initViews()
        initListeners()

        //CardView 上面数字，下面文字（文字小一点，数字大一点，颜色区分）

        //已记录天数 Int
        //总笔记数 Int
        //平均每天记录笔记数 Int

        //完成最多笔记的一天 List<User> -> it.first().endTime

        //最长的文本 User -> data
        //持续最久的笔记 User
        //第一个完成的笔记 User

        //安装日期
        //已安装时间(按天算)
        //最长连续使用天数

        dbHelper = MyDatabaseHelper(this)
        db = dbHelper.writableDatabase

        //重新获取数据
        val myTables = getTableNames(dbHelper).filter { it.startsWith('_') } //仅记录已完成的笔记
        if (myTables.isEmpty()) {
            emptyLL.isVisible = true
            return
        }
        for (i in myTables) myUserData.add(queryDatabase(db, i))

        //已记录天数 Int
        val tableNumber = myTables.size //除去 SHORT 与 LONG 的表的数目(即历史表数目)
        recordDaysTV.text = "${tableNumber}天"

        //总笔记数 Int
        val noteNumber = myUserData.flatten().size
        recordNotesTV.text = "${noteNumber}条"

        //平均每天记录笔记数 Int
        val averageNumber = if (tableNumber == 0) 0 else noteNumber / tableNumber
        recordAverageTV.text = "${averageNumber}条"

        //一天中完成最多的笔记数——我想知道是那一天 List<User> -> it.first().endTime
        val maxNoteSize = myUserData.maxByOrNull { it.size }
        val tempData = maxNoteSize!!.first().endTime.split("_")
        maxNotesDayTV.text = "${tempData[0]}年${tempData[1]}月${tempData[2]}日"
        maxNotesDayCV.setOnClickListener {
            startAnim(this, it)
            val intent = Intent(this, Second::class.java)
            intent.putExtra("FINISH", true)
            intent.putExtra("title", maxNotesDayTV.text)
            intent.putExtra("number", "共 ${maxNoteSize.size} 条")
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        //最长的文本 User -> data
        val maxNoteLength = myUserData.flatten().maxByOrNull { it.data.length }
        longestNoteET.setText(maxNoteLength!!.data)
        longestNoteET.isFocusable = false
        longestNoteET.setOnClickListener {  //自动跳转到搜索页面并填入信息
            startAnim(this, it)
            val intent = Intent(this, Search::class.java)
            intent.putExtra("SearchData", maxNoteLength.data)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        //持续最久的笔记 User
        val longestDuringTime = run {
            val tempTimeList = mutableListOf<Long>()
            val longestNoteTime = myUserData.flatten()
            for (i in longestNoteTime) tempTimeList.add(calculateTimeDifference(i.startTime.split("_"), i.endTime.split("_")))
            longestNoteTime[tempTimeList.indexOf(tempTimeList.maxOrNull())]
        }
        oldestNoteET.setText(longestDuringTime.data)
        oldestNoteET.isFocusable = false
        oldestNoteET.setOnClickListener {
            startAnim(this, it)
            val intent = Intent(this, Search::class.java)
            intent.putExtra("SearchData", longestDuringTime.data)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        //最长连续使用天数
        var count = 0 //这个count就是连续登录日期
        val recordToday = getGlobalData(this, "RecordToday", String) as String
        val timeList = recordToday.split(" ")
        for (i in timeList.indices) {
            if (i + 2 > timeList.size || timeList[i + 1].isEmpty()) break
            val temp = calculateDateDifference(timeList[i + 1].split("_"), timeList[i].split("_")).toInt()
            if (temp == 1) count++ else break
        }
        continueDaysTV.text = "${count}天"

        //安装日期
        val installDate = getGlobalData(this, "FirstUse", String) as String //2023_12_25
        installDayTV.text = installDate.replaceFirst("_", "年").replaceFirst("_", "月") + "日"

        //已安装时间(按天算)
//        val installDays = calculateDateDifference(installDate.split("_"))

        //第一个完成的笔记 User
        val firstFinishedNote = myUserData.flatten().minByOrNull { it.endTime }
        firstFinishNoteET.setText(firstFinishedNote!!.data)
        firstFinishNoteET.isFocusable = false
        firstFinishNoteET.setOnClickListener {
            startAnim(this, it)
            val intent = Intent(this, Search::class.java)
            intent.putExtra("SearchData", firstFinishedNote.data)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        //出现评率最高的字
        maxLengthET.setText(run {
            val notes = mutableListOf<MutableList<User>>()
            for (i in getTableNames(dbHelper)) if (i.isMyTable()) notes.add(queryDatabase(db, i))
            val stringList = notes.flatten().map { it.data }
            fun findMostFrequentCharacter(list: List<String>): Char? {
                val combinedString = list.joinToString("")
                val characterCounts = combinedString.groupingBy { it }.eachCount()
                return characterCounts.maxByOrNull { it.value }?.key
            }
            findMostFrequentCharacter(stringList).toString()
        })
        maxLengthET.isFocusable = false
        maxLengthET.setOnClickListener {
            startAnim(this, it)
            val intent = Intent(this, Search::class.java)
            intent.putExtra("SearchData", firstFinishedNote.data)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun initListeners() {
        recordDaysCV.setOnClickListener {
            startAnim(this, it)
        }

        recordNotesCV.setOnClickListener {
            startAnim(this, it)
        }

        continueDaysCV.setOnClickListener {
            startAnim(this, it)
        }

        recordAverageCV.setOnClickListener {
            startAnim(this, it)
        }

        installDayCV.setOnClickListener {
            startAnim(this, it)
        }
    }

    private fun initViews() {
        recordDaysTV = findViewById(R.id.statisticRecordDaysTV)
        recordAverageTV = findViewById(R.id.statisticRecordAverageTV)
        recordNotesTV = findViewById(R.id.statisticRecordNotesTV)

        maxNotesDayTV = findViewById(R.id.statisticMaxNotesDayTV)
        maxNotesDayCV = findViewById(R.id.statisticMaxNotesDayCV)

        firstFinishNoteET = findViewById(R.id.statisticFirstFinishNoteET)
        maxLengthET = findViewById(R.id.statisticMaxLengthET)

        longestNoteET = findViewById(R.id.statisticLongestNoteET)
        oldestNoteET = findViewById(R.id.statisticOldestNoteET)

        installDayTV = findViewById(R.id.statisticInstallDayTV)

        continueDaysTV = findViewById(R.id.statisticContinueDaysTV)

        recordDaysCV = findViewById(R.id.statisticRecordDaysCV)
        recordNotesCV = findViewById(R.id.statisticRecordNotesCV)
        continueDaysCV = findViewById(R.id.statisticContinueDaysCV)
        recordAverageCV = findViewById(R.id.statisticRecordAverageCV)
        installDayCV = findViewById(R.id.statisticInstallDayCV)

        emptyLL = findViewById(R.id.statisticEmptyLL)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, 0)
    }

}
