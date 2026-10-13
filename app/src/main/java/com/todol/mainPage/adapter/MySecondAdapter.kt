package com.todol.mainPage.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.todol.R
import com.todol.ShowInfo
import com.todol.User
import com.todol.database.MyDatabaseHelper
import com.todol.exportTextToPhoto
import com.todol.getAllNotes
import com.todol.isSon
import com.todol.mainPage.activity.Second
import com.todol.mainPage.activity.THEME_INDEX
import com.todol.myToast
import com.todol.queryDatabase
import com.todol.startAnim
import com.todol.startMyDialogButton
import java.time.LocalDate
import java.util.Calendar

interface CustomVariableListener {
    fun onCustomVariableChanged(newValue: IntArray)
}

class MySecondAdapter(
    private val context: Context,
    private val showInfoList: ArrayList<ShowInfo>,
    private val titleTableName: String,
    private val exportCheck: Boolean,
    private val listener: CustomVariableListener?,
    private val tempCardView: CardView?,
    private val starCheck: Boolean,
    private val childIndex: List<Int>
) :
    RecyclerView.Adapter<MySecondAdapter.MyViewHolder>() {

    private val db = MyDatabaseHelper(context).writableDatabase

    //防止数据重复加载——为recycleView添加控件奠定基础
    override fun getItemViewType(position: Int) = position.hashCode()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.second_show, parent, false)
        return MyViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val showInfo = showInfoList[position]
        holder.bind(showInfo, position)
    }

    override fun getItemCount() = showInfoList.size

    private val chooseIndexList = IntArray(showInfoList.size) { 0 }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardView: CardView = itemView.findViewById(R.id.secondShowCV)
        private val linearLayout: LinearLayout = itemView.findViewById(R.id.secondShowLL)
        private val contentTV: TextView = itemView.findViewById(R.id.secondShowContentTV)
        private val placeTV: TextView = itemView.findViewById(R.id.secondShowPlaceTV)
        private val startTimeTV: TextView = itemView.findViewById(R.id.secondShowStartTimeTV)
        private val spendTimeTV: TextView = itemView.findViewById(R.id.secondShowSpendTimeTV)
        private val fileIV: ImageView = itemView.findViewById(R.id.secondFileIV)

        private val starCV: CardView = itemView.findViewById(R.id.secondStarCV)

        private val childLL: LinearLayout = itemView.findViewById(R.id.secondShowChildLL)

        @RequiresApi(Build.VERSION_CODES.Q)
        @SuppressLint("SetTextI18n")
        fun bind(showInfo: ShowInfo, position: Int) {

            if (exportCheck) {
                val ratioColor = if (chooseIndexList[position] == 0) 0.5F else 0F
                val typedValue = TypedValue()
                context.theme.resolveAttribute(R.attr.myFirstColor, typedValue, true)
                val myFirstColor = typedValue.data
                val adjustedColor = ColorUtils.blendARGB(myFirstColor, Color.BLACK, ratioColor)
                cardView.setCardBackgroundColor(adjustedColor)
            }

            if (showInfo.filePath.isNotEmpty()) { //显示多媒体图标
                fileIV.isVisible = true
                when (THEME_INDEX) {
                    0 -> fileIV.setBackgroundResource(R.drawable.rounded_corner_red_big)
                    1 -> fileIV.setBackgroundResource(R.drawable.rounded_corner_orange_big)
                    2 -> fileIV.setBackgroundResource(R.drawable.rounded_corner_yellow_big)
                    3 -> fileIV.setBackgroundResource(R.drawable.rounded_corner_green_big)
                    4 -> fileIV.setBackgroundResource(R.drawable.rounded_corner_cyan_big)
                    5 -> fileIV.setBackgroundResource(R.drawable.rounded_corner_blue_big)
                    6 -> fileIV.setBackgroundResource(R.drawable.rounded_corner_purple_big)
                    else -> fileIV.setBackgroundResource(R.drawable.rounded_corner_dark_big)
                }
            }

            if (showInfo.place.isNotEmpty()) { //显示地点(如果有的话)
                placeTV.isVisible = true
                placeTV.text = showInfo.place
            }

            startAnim(context, linearLayout) //出现动画

            //--------------------------------Init Next--------------------------------

            spendTimeTV.text = run { //显示笔记持续时间
                val tempEndTime = showInfo.endTime.split("_").toTypedArray()
                val tempStartTime = showInfo.startTime.split("_").toTypedArray()
                val cal1 = Calendar.getInstance().apply { // 创建两个 Calendar 对象，分别表示两个日期
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
                // 将差异转换为年月日格式
                val diffInYears = diffInSeconds / (60 * 60 * 24 * 365) // 每年约有 365 天
                val diffInMonths = (diffInSeconds % (60 * 60 * 24 * 365)) / (60 * 60 * 24 * 30) // 每个月约有 30 天
                val diffInDays = (diffInSeconds % (60 * 60 * 24 * 30)) / (60 * 60 * 24)
                val diffInHours = (diffInSeconds % (60 * 60 * 24)) / (60 * 60)
                val diffInMinutes = (diffInSeconds % (60 * 60)) / (60)
                if (diffInYears != 0L) "$diffInYears 年" // //这里显示所用时间
                else if (diffInMonths != 0L) "$diffInMonths 个月"
                else if (diffInDays != 0L) "$diffInDays 天"
                else if (diffInHours != 0L) "$diffInHours 小时"
                else if (diffInMinutes != 0L) "$diffInMinutes 分钟"
                else "小于一分钟"
            }

            contentTV.text = showInfo.content //这里显示第一条内容 如果文字过多就省略号代替——好像

            startTimeTV.text = run {
                //因为EndTime是同一天，所以不写，只写StartTime 2023_12_26_18_42
                val formedText = showInfo.startTime
                    .replaceFirst("_", "年")
                    .replaceFirst("_", "月")
                    .replaceFirst("_", "日")
                    .replaceFirst("_", "时")
                if (formedText.length >= 11) formedText.replace("日", "日\n") //全长是11
                //获取今天的日期，逐个对比 我想如果是今年就不显示，和AdapterList一样
                val nowDate = LocalDate.now() //2023年12月26日18时42分
                formedText.replace("${nowDate.year}年", "")
                    .replace("${nowDate.monthValue}月", "")
                    .replace("${nowDate.dayOfMonth}日", "") + "分"
            }

            //--------------------------------Listener Next--------------------------------

            val tempListOld: List<User> = if (titleTableName.isEmpty()) {
                val tempUser = mutableListOf<User>()
                val allUser = getAllNotes(context)
                for (i in allUser) if (i.data == showInfo.content) tempUser.add(i)
                tempUser.reversed()
            } else {
                //标记收藏笔记
                val tableInfo = if (titleTableName.isSon()) titleTableName else ("_$titleTableName") //2024年1月13日
                    .replace('年', '_')
                    .replace('月', '_')
                    .replace("日", "")
                queryDatabase(db, tableInfo).reversed() //不反转会出问题
            }

            if (tempListOld[position].starCheck == "1") starCV.isVisible = true

            fun basicClick(view: View) {
                if (exportCheck) { //导出监听
                    val typedValue = TypedValue()
                    context.theme.resolveAttribute(R.attr.myFirstColor, typedValue, true)
                    val myFirstColor = typedValue.data
                    val tempData = if (chooseIndexList[position] == 0) {
                        chooseIndexList[position] = 1
                        0F
                    } else {
                        chooseIndexList[position] = 0
                        0.5F
                    }
                    listener?.onCustomVariableChanged(chooseIndexList)
                    val adjustedColor = ColorUtils.blendARGB(myFirstColor, Color.BLACK, tempData)
                    cardView.setCardBackgroundColor(adjustedColor)
                    if (chooseIndexList.contains(1)) {
                        cardView.isVisible = true
                    } else {
                        cardView.isVisible = true
                    }
                } else { //正常监听
                    startAnim(context, view) //默认点击 SecondAdapter
                    startMyDialogButton(context, db, showInfo, titleTableName, tempListOld[position], starCheck, starCV)
                }
            }

            if (childIndex.isNotEmpty()) if (childIndex[position] == 1) childLL.isVisible = true

            val showBasicClickListener = View.OnClickListener {
                if (childIndex.isEmpty()) {
                    basicClick(it)
                } else if (childIndex[position] == 1) {
                    startAnim(context, it)
                    val intent = Intent(context, Second::class.java)
                    intent.putExtra("title", titleTableName)
                    intent.putExtra("number", "共 ${showInfoList.size} 条")
                    intent.putExtra("tableName", showInfo.uid)
                    context.startActivity(intent)
                    (context as Activity).finish()
                    context.overridePendingTransition(0, 0)
                } else basicClick(it)
            }

            val showBasicLongClickListener = View.OnLongClickListener {
                basicClick(it)
                true
            }

            linearLayout.setOnClickListener(showBasicClickListener)
            linearLayout.setOnLongClickListener(showBasicLongClickListener)

            tempCardView?.setOnClickListener { view ->
                startAnim(context, view)
                val tempList = mutableListOf<ShowInfo>()
                for (i in showInfoList.indices) if (chooseIndexList[i] == 1) tempList.add(showInfoList[i])
                exportTextToPhoto(context, tempList.map { it.content }, titleTableName)
                myToast(context, "已成功保存到手机相册")
                val intent = Intent(context, Second::class.java)
                intent.putExtra("title", titleTableName)
                intent.putExtra("number", "共 ${showInfoList.size} 条")
                context.startActivity(intent)
                (context as Activity).finish()
                context.overridePendingTransition(0, 0)
            }

        }
    }
}