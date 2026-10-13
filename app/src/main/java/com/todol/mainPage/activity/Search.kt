package com.todol.mainPage.activity

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.todol.BaseActivity
import com.todol.R
import com.todol.ShowInfo
import com.todol.User
import com.todol.database.MyDatabaseHelper
import com.todol.dropKeyBoard
import com.todol.getTableNames
import com.todol.mainPage.adapter.MyFirstAdapter
import com.todol.mainPage.adapter.MySecondAdapter
import com.todol.queryDatabase
import com.todol.showKeyBoard
import com.todol.startAnim
import java.util.Calendar

class Search : BaseActivity() {

    private lateinit var searchET: EditText
    private lateinit var recyclerView: RecyclerView

    private lateinit var typeTV: TextView //选择搜索类型
    private var searchType = true //默认是搜索笔记
    private lateinit var clearTV: TextView //清空内容
    private lateinit var randomTV: TextView

    private lateinit var dbHelper: MyDatabaseHelper
    private lateinit var db: SQLiteDatabase

    private lateinit var timeList: List<String>

    private val showInfoListList = mutableListOf<MutableList<User>>() //根据日期查询的所有详细数据

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        initViews()
        initListeners()

        overridePendingTransition(0, 0)

        dbHelper = MyDatabaseHelper(this)
        db = dbHelper.writableDatabase

        //查询所有日期列表 2023年12月1日
        timeList = getTableNames(dbHelper)
            .filter { it.startsWith("_") }.toTypedArray()
            .map {
                it.replaceFirst("_", "")
                    .replaceFirst("_", "年")
                    .replaceFirst("_", "月") + "日"
            }

        for (i in timeList) showInfoListList.add( //查询所有详细数据
            queryDatabase(
                db,
                "_" + i
                    .replace("年", "_")
                    .replace("月", "_")
                    .replace("日", "")
            )
        )

        val searchData = intent.getStringExtra("SearchData") ?: ""
        if (searchData.isNotEmpty()) { //Nice
            searchET.setText(searchData)
            searchET.clearFocus()
        }

        searchET.hint = if (showInfoListList.isEmpty()) "搜索笔记"
        else if (searchType) showInfoListList.flatten().random().data.standerShow() else timeList.random()

    }

    private fun initViews() {
        searchET = findViewById(R.id.searchET)
        searchET.requestFocus()
        recyclerView = findViewById(R.id.searchRV)
        typeTV = findViewById(R.id.searchTypeTV)
        clearTV = findViewById(R.id.searchClearTV)
        randomTV = findViewById(R.id.searchRandomTV)

        initTextWatcher()
    }

    @SuppressLint("SetTextI18n")
    private fun initListeners() {
        typeTV.setOnClickListener { //随机显示一个已有的表，如果用户没搜索而直接回车那么就搜索这个吧
            startAnim(this, it)
            if (searchType) {
                typeTV.text = "搜索\n日期"
                searchET.hint = if (timeList.isEmpty()) "搜索日期" else timeList.random()
            } else {
                typeTV.text = "搜索\n笔记"
                searchET.hint = if (timeList.isEmpty()) "搜索笔记" else showInfoListList.flatten().random().data.standerShow()
            }
            searchType = !searchType
        }
        searchET.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                searchET.setText(searchET.hint)
            }
            false
        }
        clearTV.setOnClickListener { //清空内容
            startAnim(this, it)
            if (searchET.hint == "搜索日期" || searchET.hint == "搜索笔记" || searchET.hint == "已经很干净了:)" && searchET.text.isEmpty())
                searchET.hint = "已经很干净了:)" else searchET.text.clear().also { showKeyBoard(this, searchET) }
        }
        randomTV.setOnClickListener { //随机内容
            startAnim(this, it)
            searchET.setText(if (searchType) showInfoListList.flatten().random().data.standerShow() else timeList.random())
            dropKeyBoard(this, searchET)
        }
    }

    private fun initTextWatcher() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                if (inputText.isEmpty()) { //防止空数据加载
                    recyclerView.adapter = null
                    searchET.hint = if (showInfoListList.isEmpty()) if (searchType) "搜索笔记" else "搜索日期"
                    else if (searchType) showInfoListList.flatten().random().data.standerShow() else timeList.random()
                    return
                }
                //查询数据库 实时搜索刷新
                val showInfoList = mutableListOf<ShowInfo>()
                run {
                    for (j in showInfoListList) {
                        val tempList = mutableListOf<String>() //防止重复日期
                        for (i in j.indices) {
                            val temp = j[i].endTime.split("_").toTypedArray()
                            if ("${temp[0]}年${temp[1]}月${temp[2]}日" !in tempList) { //防止日期相同
                                showInfoList.add(
                                    ShowInfo(
                                        "${temp[0]}年${temp[1]}月${temp[2]}日${temp[3]}时${temp[4]}分",
                                        j.size,
                                        j[j.size - 1].data,
                                        j[j.size - 1].startTime,
                                        0L,
                                        "",
                                        j[j.size - 1].filePath,
                                        j[j.size - 1].uid,
                                    )
                                )
                                tempList.add("${temp[0]}年${temp[1]}月${temp[2]}日")
                            }
                        }
                    }
                }
                val adapter = if (searchType) { //默认搜索笔记
                    val myData = mutableListOf<ShowInfo>()
                    val queryList = mutableListOf<MutableList<User>>()
                    for (i in showInfoList) {
                        val formedTime = i.endTime.substringBefore('日').replace('年', '_').replace('月', '_')
                        queryList.add(
                            queryDatabase(
                                db, "_" + formedTime
                                    .replace("年", "_")
                                    .replace("月", "_")
                                    .replace("日", "")
                            )
                        )
                    }
                    val childIndex = queryList.flatten().map { it.child }.reversed()
                    for (j in queryList.flatten().indices)
                        myData.add(
                            ShowInfo(
                                queryList.flatten()[j].endTime,
                                0,
                                queryList.flatten()[j].data,
                                queryList.flatten()[j].startTime,
                                0L,
                                queryList.flatten()[j].place,
                                queryList.flatten()[j].filePath,
                                queryList.flatten()[j].uid,
                            )
                        )
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
                    val tempShowInfoList = mutableListOf<ShowInfo>()
                    for (i in myData) {
                        if (!i.content.contains(inputText)) continue
                        tempShowInfoList.add(
                            ShowInfo(
                                i.endTime.replace('年', '_')
                                    .replace('月', '_')
                                    .replace('日', '_')
                                    .replace('时', '_')
                                    .replace('分', '_'),
                                i.number,
                                i.content,
                                i.startTime,
                                i.differTime,
                                i.place,
                                i.filePath,
                                i.uid
                            )
                        )
                    }
                    MySecondAdapter(this@Search, tempShowInfoList as ArrayList<ShowInfo>, "", false, null, null, false, childIndex)
                } else { //直接用MyFirstAdapter
                    MyFirstAdapter(this@Search, showInfoList.filter { it.endTime.contains(inputText) } as ArrayList<ShowInfo>, true)
                }
                recyclerView.adapter = adapter
                recyclerView.layoutManager = LinearLayoutManager(this@Search)

            }
        }
        searchET.addTextChangedListener(textWatcher)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }

}

private fun String.standerShow() = this.replace("\n", " ")
