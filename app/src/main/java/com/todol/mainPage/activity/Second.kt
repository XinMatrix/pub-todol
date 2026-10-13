package com.todol.mainPage.activity

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.todol.*
import com.todol.database.MyDatabaseHelper
import com.todol.mainPage.adapter.CustomVariableListener
import com.todol.mainPage.adapter.MySecondAdapter
import java.util.*

private var oldTableName = mutableListOf<String>()

class Second : BaseActivity(), CustomVariableListener {

    //数据库
    private lateinit var dbHelper: MyDatabaseHelper
    private lateinit var db: SQLiteDatabase

    private lateinit var timeTV: TextView
    private lateinit var numberTV: TextView

    private lateinit var linearLayout: LinearLayout

    private lateinit var recyclerView: RecyclerView

    private lateinit var cardView: CardView

    private lateinit var exportTV: TextView

    private var myData = ArrayList<ShowInfo>()

    private var tempTableName = ""

    private var title = ""

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        dbHelper = MyDatabaseHelper(this)
        db = dbHelper.writableDatabase

        timeTV = findViewById(R.id.secondTextView)
        numberTV = findViewById(R.id.secondNumberTV)

        linearLayout = findViewById(R.id.secondLinearLayout)
        recyclerView = findViewById(R.id.secondRV)
        exportTV = findViewById(R.id.secondExportTV)

        cardView = findViewById(R.id.secondCV)

        overridePendingTransition(0, 0)

        numberTV.text = intent.getStringExtra("number")

        title = intent.getStringExtra("title") ?: ""
        timeTV.text = title

        val starCheck = intent.getBooleanExtra("starCheck", false) //默认false

        var childIndex = listOf<Int>()  //默认为空

        //Star仅记录单值？感觉也可以记录全值
        //现在仅处理单值
        if (starCheck) for (i in queryUserStar(db)) myData.add(i)
        else {
            tempTableName = intent.getStringExtra("tableName") ?: ("_" + title.replace("年", "_")
                .replace("月", "_")
                .replace("日", ""))
            oldTableName.add(tempTableName)
            val temp = queryDatabase(db, tempTableName)
            childIndex = temp.map { it.child }.reversed()
            for (j in temp.indices) myData.add(
                ShowInfo(
                    temp[j].endTime,
                    0,
                    temp[j].data,
                    temp[j].startTime,
                    0L,
                    temp[j].place,
                    temp[j].filePath,
                    temp[j].uid
                )
            )
        }
        for (i in 0 until myData.size) {
            val tempEndTime = myData[i].endTime.split("_").toTypedArray()
            val tempStartTime = myData[i].startTime.split("_").toTypedArray()
            // 创建两个 Calendar 对象，分别表示两个日期
            val cal1 = Calendar.getInstance().apply {
                set(
                    tempEndTime[0].toInt(),
                    tempEndTime[1].toInt() - 1,
                    tempEndTime[2].toInt(),
                    tempEndTime[3].toInt(),
                    tempEndTime[4].toInt()
                ) // 注意月份是从0开始计数的，所以5月要写成4
            }
            val cal2 = Calendar.getInstance().apply {
                set(
                    tempStartTime[0].toInt(),
                    tempStartTime[1].toInt() - 1,
                    tempStartTime[2].toInt(),
                    tempStartTime[3].toInt(),
                    tempStartTime[4].toInt()
                )
            }
            // 计算两个日期之间的差值（以毫秒为单位）
            val diffInSeconds = (cal1.timeInMillis - cal2.timeInMillis) / 1000
            myData[i].differTime = diffInSeconds
        }

        //按持续时间有小到大排序 妙啊
        myData = myData.sortedByDescending { it.differTime }.reversed().toCollection(ArrayList())

        val tempTitle = if (tempTableName.isSon()) tempTableName else title
        var adapter = MySecondAdapter(this, myData, tempTitle, false, this, cardView, starCheck, childIndex)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        exportTV.setOnClickListener { //点击自由选择部分笔记
            val tempCheck = if (exportTV.text == "取消") {
                exportTV.text = "导出"
                Handler(mainLooper).postDelayed({ //延迟显示防止顺序错误
                    cardView.isVisible = false
                }, 10)
                false
            } else {
                exportTV.text = "取消"
                true
            }
            adapter = MySecondAdapter(this, myData, tempTitle, tempCheck, this, cardView, starCheck, childIndex)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(this)
        }

        exportTV.setOnLongClickListener { //长按导出全部笔记
            val rawTextList = myData.map { it.content }
            exportTextToPhoto(this, rawTextList, title)
            myToast(this, "已成功保存到手机相册")
            true
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        if (tempTableName.isSon()) { //Nice
            val intent = Intent(this, Second::class.java)
            intent.putExtra("title", title)
            intent.putExtra("tableName", oldTableName.lastTwo())
            intent.putExtra("number", numberTV.text.toString())
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
            return
        }
        if (intent.getBooleanExtra("FINISH", false)) {
            finish()
            overridePendingTransition(0, 0)
            return
        }
        val intent = Intent(this, Main::class.java)
        intent.putExtra("AniCheck", false)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }

    override fun onCustomVariableChanged(newValue: IntArray) { //Nice 回调接口
        cardView.isVisible = !newValue.all { it == 0 }
    }

}