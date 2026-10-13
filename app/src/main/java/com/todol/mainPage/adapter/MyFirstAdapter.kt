package com.todol.mainPage.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.todol.R
import com.todol.ShowInfo
import com.todol.mainPage.activity.CHOOSE_POSITION
import com.todol.mainPage.activity.Second
import com.todol.myToast
import com.todol.startAnim

class MyFirstAdapter(private val context: Context, private val showInfoList: ArrayList<ShowInfo>, private val backAniCheck: Boolean) :
    RecyclerView.Adapter<MyFirstAdapter.MyViewHolder>() {

    //防止数据重复加载——为recycleView添加控件奠定基础
    override fun getItemViewType(position: Int) = position.hashCode()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.first_show, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val showInfo = showInfoList[position]
        holder.bind(showInfo, position)
    }

    override fun getItemCount() = showInfoList.size

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val linearLayout: LinearLayout = itemView.findViewById(R.id.firstShowLL)
        private val startEndLL: LinearLayout = itemView.findViewById(R.id.firstShowStartEndLL)

        private val timeTV: TextView = itemView.findViewById(R.id.firstShowTimeTV)
        private val numberTV: TextView = itemView.findViewById(R.id.firstShowNumberTV)
        private val contentTV: TextView = itemView.findViewById(R.id.firstShowContentTV)

        @SuppressLint("SetTextI18n")
        fun bind(showInfo: ShowInfo, position: Int) {

            startEndLL.isVisible = false

            if (position == showInfoList.size - 1) startEndLL.isVisible = true

            //出现动画
            if (backAniCheck) startAnim(context, linearLayout, 1) //加载列表

            //这里显示时间
            if (showInfo.endTime == "时间") timeTV.text = showInfo.endTime else timeTV.text = showInfo.endTime.substringBefore("日") + "日"
            if (showInfo.number == 0) { //这里显示条数
                numberTV.isVisible = false
                contentTV.text = "暂无内容"
            } else {
                numberTV.text = "共 ${showInfo.number} 条"
                contentTV.text = showInfo.content
            }
            linearLayout.setOnClickListener {
                startAnim(context, linearLayout) //默认点击动画
                if (showInfo.number == 0) {
                    myToast(context, "暂无内容，请添加笔记完成后重试")
                    return@setOnClickListener
                }
                CHOOSE_POSITION = position
                val intent = Intent(context, Second::class.java)
                intent.putExtra("title", showInfo.endTime.substringBefore("日") + "日")
                intent.putExtra("number", numberTV.text.toString())
                context.startActivity(intent)
                (context as Activity).finish()
            }
        }
    }
}