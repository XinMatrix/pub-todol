package com.todol

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.todol.database.MyDatabaseHelper
import com.todol.mainPage.activity.ANI_AVAILABLE_CHECK
import com.todol.mainPage.activity.HINT_CHECK
import com.todol.mainPage.activity.Second
import com.todol.mainPage.activity.THEME_INDEX
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.ceil

var myTableName = "SHORT"

data class ShowInfo(
    val endTime: String,
    val number: Int,
    var content: String,
    val startTime: String,
    var differTime: Long,
    val place: String,
    val filePath: String,
    val uid: String
)

//id默认顺序增长 uid唯一标识符 data文字数据 state是否完成 remind提醒时间 child是否含有子集 startTime开始时间 endTime结束时间 place事件地点 filePath文件地址
data class User(
    val id: Int,
    val uid: String,
    val data: String,
    val state: Int,
    val remindTime: String,
    val child: Int,
    val startTime: String,
    val endTime: String,
    val place: String,
    val filePath: String,
    val dayCheck: String,
    val starCheck: String,
)

//动态创建数据库表格
fun createSheetIfNotExists(db: SQLiteDatabase, name: String) {
    db.execSQL(
        "create table if not exists $name(" +
                "id integer primary key autoincrement, " +
                "uid text not null," +
                "data text not null, " +
                "state integer not null, " +
                "remindTime text not null," +
                "child integer not null," +
                "startTime text not null," +
                "endTime text not null," +
                "place text not null," +
                "filePath text not null," +
                "dayCheck integer not null," +
                "starCheck integer not null)"

    )
}

@SuppressLint("Range")
fun queryDatabase(db: SQLiteDatabase, tableName: String): MutableList<User> {
    val columns = mutableListOf<String>()
    val tempMutableList = mutableListOf<User>()
    columns.add("id")
    columns.add("uid")
    columns.add("data")
    columns.add("state")
    columns.add("remindTime")
    columns.add("child")
    columns.add("startTime")
    columns.add("endTime")
    columns.add("place")
    columns.add("place")
    columns.add("filePath")
    columns.add("dayCheck")
    columns.add("starCheck")
    val cursor = db.query(tableName, columns.toTypedArray(), null, null, null, null, null)
    while (cursor.moveToNext()) {
        val id = cursor.getInt(cursor.getColumnIndex("id"))
        val uid = cursor.getString(cursor.getColumnIndex("uid"))
        val data = cursor.getString(cursor.getColumnIndex("data"))
        val state = cursor.getInt(cursor.getColumnIndex("state"))
        val remindTime = cursor.getString(cursor.getColumnIndex("remindTime"))
        val child = cursor.getInt(cursor.getColumnIndex("child"))
        val startTime = cursor.getString(cursor.getColumnIndex("startTime"))
        val endTime = cursor.getString(cursor.getColumnIndex("endTime"))
        val place = cursor.getString(cursor.getColumnIndex("place"))
        val filePath = cursor.getString(cursor.getColumnIndex("filePath"))
        val dayCheck = cursor.getString(cursor.getColumnIndex("dayCheck"))
        val starCheck = cursor.getString(cursor.getColumnIndex("starCheck"))
        tempMutableList.add(User(id, uid, data, state, remindTime, child, startTime, endTime, place, filePath, dayCheck, starCheck))
    }
    cursor.close()
    return tempMutableList
}

fun getAllNotes(context: Context): List<User> {
    val temp = mutableListOf<MutableList<User>>()
    val tableNames = getTableNames(MyDatabaseHelper(context))
    for (i in tableNames) if (i.isMyTable()) temp.add(queryDatabase(MyDatabaseHelper(context).writableDatabase, i))
    return temp.flatten()
}

@SuppressLint("Range")
fun queryUserStar(db: SQLiteDatabase): MutableList<ShowInfo> {
    val columns = mutableListOf<String>()
    val tempMutableList = mutableListOf<ShowInfo>()
    columns.add("id")
    columns.add("endTime")
    columns.add("number")
    columns.add("content")
    columns.add("startTime")
    columns.add("differTime")
    columns.add("place")
    columns.add("filePath")
    val cursor = db.query("USER_STAR", columns.toTypedArray(), null, null, null, null, null)
    while (cursor.moveToNext()) {
        val endTime = cursor.getString(cursor.getColumnIndex("endTime"))
        val number = cursor.getString(cursor.getColumnIndex("number"))
        val content = cursor.getString(cursor.getColumnIndex("content"))
        val startTime = cursor.getString(cursor.getColumnIndex("startTime"))
        val differTime = cursor.getString(cursor.getColumnIndex("differTime"))
        val place = cursor.getString(cursor.getColumnIndex("place"))
        val filePath = cursor.getString(cursor.getColumnIndex("filePath"))
        tempMutableList.add(ShowInfo(endTime, number.toInt(), content, startTime, differTime.toLong(), place, filePath, ""))
    }
    cursor.close()
    return tempMutableList
}

fun insertUser(
    db: SQLiteDatabase,
    tableName: String,
    uid: String,
    data: String,
    state: Int,
    remindTime: String,
    child: Int,
    startTime: String,
    endTime: String,
    place: String,
    filePath: String,
    dayCheck: String,
    starCheck: String,
) {
    val values = ContentValues().apply {
        put("uid", uid)
        put("data", data)
        put("state", state)
        put("remindTime", remindTime)
        put("child", child)
        put("startTime", startTime)
        put("endTime", endTime)
        put("place", place)
        put("filePath", filePath)
        put("dayCheck", dayCheck)
        put("starCheck", starCheck)
    }
    db.insert(tableName, null, values)
}

fun insertStar(db: SQLiteDatabase, showInfo: ShowInfo) {
    val values = ContentValues().apply {
        put("endTime", showInfo.endTime)
        put("number", showInfo.number)
        put("content", showInfo.content)
        put("startTime", showInfo.startTime)
        put("differTime", showInfo.differTime)
        put("place", showInfo.place)
        put("filePath", showInfo.filePath)
    }
    db.insert("USER_STAR", null, values)
}

//交换用函数
private fun updateUser(
    db: SQLiteDatabase,
    tableName: String,
    id: Int,
    uid: String,
    data: String,
    state: Int,
    remindTime: String,
    child: Int,
    startTime: String,
    endTime: String,
    place: String,
    filePath: String,
    dayCheck: String,
) {
    val values = ContentValues().apply {
        put("uid", uid)
        put("data", data)
        put("state", state)
        put("remindTime", remindTime)
        put("child", child)
        put("startTime", startTime)
        put("endTime", endTime)
        put("place", place)
        put("filePath", filePath)
        put("dayCheck", dayCheck)
    }
    db.update(tableName, values, "id = ?", arrayOf(id.toString()))
}

//只更新state
fun updateState(db: SQLiteDatabase, tableName: String, id: Int, state: Int) {
    val values = ContentValues().apply {
        put("state", state)
    }
    db.update(tableName, values, "id = ?", arrayOf(id.toString()))
}

//只更新data
fun updateInputData(db: SQLiteDatabase, tableName: String, id: Int, data: String) {
    val values = ContentValues().apply {
        put("data", data)
    }
    db.update(tableName, values, "id = ?", arrayOf(id.toString()))
}

//只更新time
fun updateRemind(db: SQLiteDatabase, tableName: String, id: Int, remindTime: String) {
    val values = ContentValues().apply {
        put("remindTime", remindTime)
    }
    db.update(tableName, values, "id = ?", arrayOf(id.toString()))
}

//只更新子集判定
fun updateChild(db: SQLiteDatabase, tableName: String, id: Int, child: Int) {
    val values = ContentValues().apply {
        put("child", child)
    }
    db.update(tableName, values, "id = ?", arrayOf(id.toString()))
}

//只更新结束时间
fun updateEndTime(db: SQLiteDatabase, tableName: String, id: Int, endTime: String) {
    val values = ContentValues().apply {
        put("endTime", endTime)
    }
    db.update(tableName, values, "id = ?", arrayOf(id.toString()))
}

//只更新Place
fun updatePlace(db: SQLiteDatabase, tableName: String, id: Int, place: String) {
    val values = ContentValues().apply {
        put("place", place)
    }
    db.update(tableName, values, "id = ?", arrayOf(id.toString()))
}

//只更新FilePath
fun updateFilePath(db: SQLiteDatabase, tableName: String, id: Int, filePath: String) {
    val values = ContentValues().apply {
        put("filePath", filePath)
    }
    db.update(tableName, values, "id = ?", arrayOf(id.toString()))
}

//只更新DayCheck
fun updateDayCheck(db: SQLiteDatabase, tableName: String, id: Int, dayCheck: String) {
    val values = ContentValues().apply {
        put("dayCheck", dayCheck)
    }
    db.update(tableName, values, "id = ?", arrayOf(id.toString()))
}

fun updateStarCheck(db: SQLiteDatabase, tableName: String, content: String, starCheck: Int) {
    val values = ContentValues().apply {
        put("starCheck", starCheck)
    }
    db.update(tableName, values, "data = ?", arrayOf(content))
}

fun resetFilePathAndDelete(context: Context, db: SQLiteDatabase, tableName: String, filePath: String) {
    val values = ContentValues().apply {
        put("filePath", "")
    }
    db.update(tableName, values, "filePath = ?", arrayOf(filePath))
    val regex = "/multiMedia/image/.*"
    val pattern: Pattern = Pattern.compile(regex)
    val matcher: Matcher = pattern.matcher(filePath)
    var extractedPath = ""
    if (matcher.find()) extractedPath = matcher.group().substring(1)
    if (extractedPath.isNotEmpty()) {
        val file = File(context.filesDir, extractedPath)
        if (file.delete()) myToastLong(context, "附件删除成功")
        else myToastLong(context, "附件删除失败")
    } else myToastLong(context, "异常-找不到文件")
}

fun deleteByData(db: SQLiteDatabase, tableName: String, data: String) {
    db.delete(tableName, "data = ?", arrayOf(data))
    // 重新排序id
    val users = queryDatabase(db, tableName)
    dropTableIfExists(db, tableName)
    createSheetIfNotExists(db, tableName)
    for (i in users) insertUser(
        db,
        tableName,
        i.uid,
        i.data,
        i.state,
        i.remindTime,
        i.child,
        i.startTime,
        i.endTime,
        i.place,
        i.filePath,
        i.dayCheck,
        i.starCheck,
    )
}

fun deleteStar(db: SQLiteDatabase, data: String) {
    db.delete("USER_STAR", "content = ?", arrayOf(data))
}

// 定义交换函数 只是互换数据 id还是顺序
fun swapRows(db: SQLiteDatabase, tableName: String, id1: Int, id2: Int) {
    if (id1 == id2) return
    val itemList = queryDatabase(db, tableName)
    val temp = mutableListOf<User>()
    //遍历 新建 交换 删除
    for (i in 0 until itemList.size) if (itemList[i].id == id1) { //获取name1的数据给temp
        temp.add(
            User(
                itemList[i].id,
                itemList[i].uid,
                itemList[i].data,
                itemList[i].state,
                itemList[i].remindTime,
                itemList[i].child,
                itemList[i].startTime,
                itemList[i].endTime,
                itemList[i].place,
                itemList[i].filePath,
                itemList[i].dayCheck,
                itemList[i].starCheck,
            )
        )
        break
    }
    for (i in 0 until itemList.size) if (itemList[i].id == id2) { //name2数据给name1
        updateUser(
            db,
            tableName,
            id1,
            itemList[i].uid,
            itemList[i].data,
            itemList[i].state,
            itemList[i].remindTime,
            itemList[i].child,
            itemList[i].startTime,
            itemList[i].endTime,
            itemList[i].place,
            itemList[i].filePath,
            itemList[i].dayCheck,
        )
        break
    }
    updateUser(
        //temp数据给name2
        db,
        tableName,
        id2,
        temp.first().uid,
        temp.first().data,
        temp.first().state,
        temp.first().remindTime,
        temp.first().child,
        temp.first().startTime,
        temp.first().endTime,
        temp.first().place, //这里写错了。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。
        temp.first().filePath,
        temp.first().dayCheck,
    )
}

//这两个函数是TODOL的核心函数
fun swapToTop(db: SQLiteDatabase, tableName: String, id: Int) { //元素置底功能  从上到下交换
    val itemList = queryDatabase(db, tableName)
    for (i in id until itemList.size) swapRows(db, tableName, i, i + 1)
}

//从id交换到第一位
fun swapToBottom(db: SQLiteDatabase, tableName: String, id: Int) { //难道就这样解决了？
    for (i in id - 1 downTo 1) swapRows(db, tableName, i, i + 1)
}

fun myVibrate(context: Context, timeMs: Long) {
    val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java) as Vibrator
    if (vibrator.hasVibrator()) { // 检查设备是否支持振动
        val vibrationDuration: Long = timeMs // 单位为毫秒
        vibrator.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

fun dropTableIfExists(db: SQLiteDatabase, tableName: String) = db.execSQL("DROP TABLE IF EXISTS $tableName")

//获取所以表的名称
fun getTableNames(dbHelper: MyDatabaseHelper): List<String> {
    val tableNames = mutableListOf<String>()
    val cursor = dbHelper.readableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
    if (cursor.moveToFirst()) while (!cursor.isAfterLast) {
        tableNames.add(cursor.getString(0))
        cursor.moveToNext()
    }
    cursor.close()
    return tableNames
}

//每天自动创建  今天启动给昨天归档  也不一定是昨天 基本思想是存
//获取今天的时间 查找以前完成的项目 创建or not 新表格 插入数据
fun filePerDay(context: Context, db: SQLiteDatabase, dbHelper: MyDatabaseHelper) {
    //获取今天的时间
    val nowDate = LocalDate.now()
    val dayCheck = getGlobalData(context, "dayCheck", String) as String
    val today = "${nowDate.year}_${nowDate.monthValue}_${nowDate.dayOfMonth}"
    if (dayCheck == today) return
    putGlobalData(context, "dayCheck", today)
    val allTableNames = getTableNames(dbHelper).toTypedArray() //获取所有表名
    for (j in allTableNames.indices) {
        //如果是非手动创建表就返回  只拿今天的去算 之前的不用算
        if (!allTableNames[j].startsWith('S') && !allTableNames[j].startsWith('L')) continue //结束本次循环
        if (allTableNames[j].isSon()) continue //不记录子集
        val recordList = mutableListOf<Int>()
        val tempList = queryDatabase(db, allTableNames[j])
        //遍历所有元素 获取这个表中已完成笔记的下标
        //遍历已完成元素存入名为 _2023_5_28 的新表中
        for (i in 0 until tempList.size) {
            if (tempList[i].endTime.isNotEmpty()) {
                //标记这条数据已完成
                recordList.add(i)
                //获取它的完成时间  [2023, 5, 29, 13, 31]
                val arr = tempList[i].endTime.split("_").toTypedArray()
                //等于今天就返回
                if (arr[2] == nowDate.dayOfMonth.toString() &&
                    arr[1] == nowDate.monthValue.toString() &&
                    arr[0] == nowDate.year.toString()
                ) return
                //存入到 _2023_5_29 里
                val temp = "_${arr[0]}_${arr[1]}_${arr[2]}"
                createSheetIfNotExists(db, temp)
                insertUser(
                    db,
                    temp,
                    tempList[i].uid,
                    tempList[i].data,
                    tempList[i].state,
                    tempList[i].remindTime,
                    tempList[i].child,
                    tempList[i].startTime,
                    tempList[i].endTime,
                    tempList[i].place,
                    tempList[i].filePath,
                    tempList[i].dayCheck,
                    tempList[i].starCheck,
                )
            }
        }
        //创建临时容器存储所有数据 删除已完成现表元素并重新导入未完成表元素
        val totalData = mutableListOf<User>()
        for (i in 0 until tempList.size)
            if (i !in recordList)
                totalData.add(
                    User(
                        tempList[i].id,
                        tempList[i].uid,
                        tempList[i].data,
                        tempList[i].state,
                        tempList[i].remindTime,
                        tempList[i].child,
                        tempList[i].startTime,
                        tempList[i].endTime,
                        tempList[i].place,
                        tempList[i].filePath,
                        tempList[i].dayCheck,
                        tempList[i].starCheck,
                    )
                )
        //清空数据库并重新导入数据 可以保证id顺序增长 数据库不出错
        dropTableIfExists(db, allTableNames[j])
        createSheetIfNotExists(db, allTableNames[j])
        for (i in 0 until totalData.size)
            insertUser(
                db,
                allTableNames[j],
                totalData[i].uid,
                totalData[i].data,
                totalData[i].state,
                totalData[i].remindTime,
                totalData[i].child,
                totalData[i].startTime,
                totalData[i].endTime,
                totalData[i].place,
                totalData[i].filePath, //原来是这个地方写错了
                totalData[i].dayCheck,
                totalData[i].starCheck,
            )
        //dayCheck数据置一
        resetDayCheck(db) //对所有SHORT表内容置一
    }
}

private fun resetDayCheck(db: SQLiteDatabase) {
    if (!HINT_CHECK) return //因为默认是true 所以不是true就会返回
    val tableName = "SHORT"
    val oldData = queryDatabase(db, tableName)
    dropTableIfExists(db, tableName)
    createSheetIfNotExists(db, tableName) //不能是完成的笔记
    for (i in oldData) insertUser(
        db,
        tableName,
        i.uid,
        i.data,
        i.state,
        i.remindTime,
        i.child,
        i.startTime,
        i.endTime,
        i.place,
        i.filePath,
        if (i.state == 1) "0" else "1", //对于完成的不用标记
        i.starCheck,
    )
}

fun startAnim(context: Context, view: View, check: Int = 0) {
    if (ANI_AVAILABLE_CHECK) return //默认false，也就是加载动画——全局设置动画
    val animScale: Animation = when (check) {
        //全局 用于加载列表
        1 -> AnimationUtils.loadAnimation(context, R.anim.s_951_a_01_t_200)
        //底部菜单栏
//        2 -> AnimationUtils.loadAnimation(context, R.anim.s_09_200)
        //listview item展示
//        3 -> AnimationUtils.loadAnimation(context, R.anim.a_00_400_t_600_400)
        //消失
//        4 -> AnimationUtils.loadAnimation(context, R.anim.a_10_400)
        //出现 用于加载出现
        5 -> AnimationUtils.loadAnimation(context, R.anim.a_01_200)
        //点击
        else -> AnimationUtils.loadAnimation(context, R.anim.s_098_a_051_300)
    }
    view.startAnimation(animScale)
}

//获取剪切板数据
fun getClipBoardContent(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip?.getItemAt(0)?.text.toString()
}

//存储至剪切板
fun Context.copyToClipboard(text: String, label: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

//获取全局变量
fun getGlobalData(context: Context, name: String, exception: Any = Any()): Any {
    val sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
    try { //sharedPreferences.all[name]存有类型信息
        return when (val data = sharedPreferences.all[name]) {
            is String -> data
            is Int -> data
            is Float -> data
            is Boolean -> data
            else -> throw IllegalArgumentException("$name is of unsupported type")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return when (exception) {
        Boolean -> false
        Int -> -1
        Float -> 0F
        String -> ""
        else -> throw IllegalArgumentException("$name is of unsupported type")
    }
}

//存储全局变量——尽可能只存储长期配置数据
fun putGlobalData(context: Context, name: String, any: Any) {
    val sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    when (any) {
        is String -> editor.putString(name, any.toString())
        is Int -> editor.putInt(name, any)
        is Float -> editor.putFloat(name, any)
        is Boolean -> editor.putBoolean(name, any)
        else -> throw IllegalArgumentException("Unsupported data type")
    }
    editor.apply()
}

fun setViewVisible(viewList: List<View>, boolean: Boolean) {
    for (view in viewList) view.isVisible = boolean
}

fun myToast(context: Context, text: String) {
    val toast = Toast.makeText(context, text, Toast.LENGTH_SHORT)
    toast.setGravity(Gravity.CENTER, 0, 300) //300
    toast.show()
}

fun myToastLong(context: Context, text: String) {
    val toast = Toast.makeText(context, text, Toast.LENGTH_LONG)
    toast.setGravity(Gravity.CENTER, 0, 300) //300
    toast.show()
}

fun calculateTimeDifference(timeList: List<String>): Long { //大于零说明在未来
    val tempNowDate = LocalDate.now()
    val tempNowTime = LocalTime.now()
    val nowTime = listOf(tempNowDate.year, tempNowDate.monthValue, tempNowDate.dayOfMonth, tempNowTime.hour, tempNowTime.minute)
    //[2023, 11, 24, 8, 12]
    val calTime = timeList.map { it.toInt() }
    // 构建当前时间的Calendar对象
    val currentTime = Calendar.getInstance()
    currentTime.set(nowTime[0], nowTime[1], nowTime[2], nowTime[3], nowTime[4])
    // 构建给定时间的Calendar对象
    val givenTime = Calendar.getInstance()
    givenTime.set(calTime[0], calTime[1], calTime[2], calTime[3], calTime[4])
    // 计算时间差（单位：秒）
    val differenceInMillis = givenTime.timeInMillis - currentTime.timeInMillis
    return TimeUnit.MILLISECONDS.toMinutes(differenceInMillis)
}

//重载函数，计算两个时间差，返回值应该为正值
fun calculateTimeDifference(smallTime: List<String>, bigTime: List<String>): Long { //[2023, 11, 24, 8, 12]
    // 构建当前时间的Calendar对象
    val tempBigTime = bigTime.map { it.toInt() }
    val bigTimeInstance = Calendar.getInstance()
    bigTimeInstance.set(tempBigTime[0], tempBigTime[1], tempBigTime[2], tempBigTime[3], tempBigTime[4])
    // 构建给定时间的Calendar对象
    val tempSmallTime = smallTime.map { it.toInt() }
    val smallTimeInstance = Calendar.getInstance()
    smallTimeInstance.set(tempSmallTime[0], tempSmallTime[1], tempSmallTime[2], tempSmallTime[3], tempSmallTime[4])
    val differenceInMillis = bigTimeInstance.timeInMillis - smallTimeInstance.timeInMillis
    return TimeUnit.MILLISECONDS.toMinutes(differenceInMillis)
}

fun calculateDateDifference(smallTime: List<String>, bigTime: List<String>): Long { //[2023, 11, 24, 8, 12]
    // 构建当前时间的Calendar对象
    val tempBigTime = bigTime.map { it.toInt() }
    val bigTimeInstance = Calendar.getInstance()
    bigTimeInstance.set(tempBigTime[0], tempBigTime[1], tempBigTime[2])
    // 构建给定时间的Calendar对象
    val tempSmallTime = smallTime.map { it.toInt() }
    val smallTimeInstance = Calendar.getInstance()
    smallTimeInstance.set(tempSmallTime[0], tempSmallTime[1], tempSmallTime[2])
    val differenceInMillis = bigTimeInstance.timeInMillis - smallTimeInstance.timeInMillis
    return TimeUnit.MILLISECONDS.toDays(differenceInMillis)
}

fun Long.isNotFuture() = this < 0

fun initImage(context: Context, filePath: String, imageView: ImageView) {
    imageView.clipToOutline = true
    imageView.isVisible = true
    val glideOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL) // 添加其他Glide选项，如占位图、错误图等
    Glide.with(context).load(filePath).apply(glideOptions).into(imageView)
}

fun initTVFile(filePath: String, textView: TextView) {
    textView.isVisible = true
    textView.text = filePath.substringAfterLast("/") //如果超出就用省略号
}

fun saveFileToInternalStorage(context: Context, selectedFileUri: Uri, filePath: String, callback: (Boolean, String?) -> Unit) {
    val file = File(filePath)
    val outputStream: FileOutputStream
    try {
        outputStream = FileOutputStream(file)
        val inputStream = context.contentResolver.openInputStream(selectedFileUri)
        // 将输入流的内容复制到输出流中
        val buffer = ByteArray(4 * 1024) // 缓冲区大小
        var bytesRead: Int
        while (inputStream?.read(buffer).also { bytesRead = it!! } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.close()
        inputStream?.close()
        callback(true, file.absolutePath) // 文件保存成功，返回文件地址
    } catch (e: Exception) {
        e.printStackTrace()
        callback(false, null) // 文件保存失败，返回空文件地址
    }
}

fun String.isImageFile(): Boolean {
    val imageExtensions = arrayOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp")
    val lowercaseFilePath = this.lowercase(Locale.ROOT)
    for (extension in imageExtensions) if (lowercaseFilePath.endsWith(extension)) return true
    return false
}

fun String.isAudioFile(): Boolean {
    val musicExtensions = arrayOf(".mp3", ".wav", ".ogg", ".aac", ".flac")
    val lowercaseFilePath = this.lowercase(Locale.ROOT)
    for (extension in musicExtensions) if (lowercaseFilePath.endsWith(extension)) return true
    return false
}

fun String.isVideoFile(): Boolean {
    val videoExtensions = arrayOf(".mp4", ".mkv", ".avi", ".mov", ".wmv")
    val lowercaseFilePath = this.lowercase(Locale.ROOT)
    for (extension in videoExtensions) if (lowercaseFilePath.endsWith(extension)) return true
    return false
}

//仅写入文本——一切正常
@RequiresApi(Build.VERSION_CODES.Q)
fun createFileInDownloadsOnText(
    context: Context,
    userDataMap: MutableMap<String, MutableList<User>>,
    callback: (Boolean, Uri?) -> Unit
) {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, "EXPORT.txt")
        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TODOL_SP_DATA")
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
    uri?.let { fileUri ->
        try {
            val gson = Gson()
            val json = gson.toJson(userDataMap) // 将 userDataMap 转换为 JSON 字符串
            resolver.openOutputStream(fileUri)?.use { outputStream ->
                outputStream.write(json.toByteArray()) // 将 JSON 字符串写入文件
                callback(true, fileUri) // 写入成功后的回调
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false, null)
        }
    } ?: run {
        callback(false, null)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun importDataFromFile(filePath: String): MutableMap<String, MutableList<User>>? {
    try {
        val file = File(filePath)
        val fileReader = FileReader(file)
        val gson = Gson()
        val userDataJson = fileReader.readText()
        fileReader.close()
        return gson.fromJson(userDataJson, object : TypeToken<MutableMap<String, MutableList<User>>>() {}.type)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

//对错弹窗
@SuppressLint("SetTextI18n")
fun startMyDialog(context: Context, title: String, content: String, function: () -> Unit) {
    val dialog = Dialog(context)
    dialog.setContentView(R.layout.my_dialog)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // 设置对话框的背景为透明 Nice
    val titleTV: TextView = dialog.findViewById(R.id.myDialogTitleTV)
    val contentTV: TextView = dialog.findViewById(R.id.myDialogContentTV)
    val noTV: TextView = dialog.findViewById(R.id.myDialogNoTV)
    val yesTV: TextView = dialog.findViewById(R.id.myDialogYesTV)

    titleTV.text = title
    contentTV.text = content

    noTV.setOnClickListener {
        dialog.dismiss() // 关闭对话框
    }
    yesTV.setOnClickListener {
        dialog.dismiss() // 关闭对话框
        function()
    }
    dialog.show()
}

//按钮弹窗
fun startMyDialogButton(
    context: Context,
    db: SQLiteDatabase,
    showInfo: ShowInfo,
    titleTableName: String,
    user: User,
    startCheck: Boolean,
    starCV: CardView
) {
    val dialog = Dialog(context)
    dialog.setContentView(R.layout.my_dialog_button)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // 设置对话框的背景为透明 Nice

    val checkFileTV: TextView = dialog.findViewById(R.id.myDialogCheckFileTV)
    val checkFileLL: LinearLayout = dialog.findViewById(R.id.myDialogCheckFileLL)

    val deleteFileTV: TextView = dialog.findViewById(R.id.myDialogDeleteFileTV)
    val deleteFileLL: LinearLayout = dialog.findViewById(R.id.myDialogDeleteFileLL)

    val starTV: TextView = dialog.findViewById(R.id.myDialogButtonStarTV)

    val saveToClipBoardTV: TextView = dialog.findViewById(R.id.myDialogButtonSaveToClipBoardTV)
    val deleteTV: TextView = dialog.findViewById(R.id.myDialogButtonDeleteTV)

    if (showInfo.filePath.isNotEmpty()) setViewVisible(listOf(checkFileLL, checkFileTV, deleteFileLL, deleteFileTV), true)

    fun openFile() { //一码多用
        val file = File(showInfo.filePath)
        val uri = FileProvider.getUriForFile(context, "com.todol", file)
        val mimeType = context.contentResolver.getType(uri)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    checkFileTV.setOnClickListener { //查看文件
        if (showInfo.filePath.isImageFile()) {  //打开本地相册并显示文件
            openFile()
            dialog.dismiss()
        } else if (showInfo.filePath.isAudioFile()) {
            openFile()
            dialog.dismiss()
        } else if (showInfo.filePath.isVideoFile()) {
            openFile()
            dialog.dismiss()
        }
    }

    //这里有点乱，先凑合用
    if (user.starCheck == "1" || startCheck || starCV.isVisible) starTV.text = "取消收藏"

    //坑真多
    starTV.setOnClickListener {
        startAnim(context, it)
        //2024_1_13_9_4
        val tableName = run {
            val temp = showInfo.endTime.split("_").toTypedArray()
            "_${temp[0]}_${temp[1]}_${temp[2]}"
        }
        if (starCV.isVisible) {
            starCV.isVisible = false
            deleteStar(db, showInfo.content)
            updateStarCheck(db, tableName, showInfo.content, 0) //标记为已取消收藏
            myToast(context, "已移除")
        } else {
            starCV.isVisible = true
            insertStar(db, showInfo)
            updateStarCheck(db, tableName, showInfo.content, 1) //标记为已添加收藏
            myToast(context, "已添加至收藏列表")
        }
        dialog.dismiss()
    }

    deleteFileTV.setOnClickListener { //删除附件
        startMyDialog(context, "删除附件 ?", "删除TODOL复制文件\n该操作不可恢复") { //删除笔记
            val tableName = "_" + titleTableName //这里之所以要加下划线是为了数据库使用
                .replace("年", "_")
                .replace("月", "_")
                .replace("日", "")
            resetFilePathAndDelete(context, db, tableName, showInfo.filePath)
            val intent = Intent(context, Second::class.java)
            intent.putExtra("title", titleTableName)
            intent.putExtra("number", "共 ${showInfo.number} 条")
            context.startActivity(intent)
            (context as Activity).finish()
            context.overridePendingTransition(0, 0)
        }
        dialog.dismiss()
    }

    saveToClipBoardTV.setOnClickListener {
        context.copyToClipboard(showInfo.content, "笔记内容")
        myToast(context, "已复制")
        dialog.dismiss()
    }

    deleteTV.setOnClickListener {
        startMyDialog(context, "删除笔记 ?", "该操作不可恢复") { //删除笔记
            val tableName = "_" + titleTableName //这里之所以要加下划线是为了数据库使用
                .replace("年", "_")
                .replace("月", "_")
                .replace("日", "")
            deleteByData(db, tableName, showInfo.content)
            myToast(context, "笔记删除成功!")
            //Start Again
            val intent = Intent(context, Second::class.java)
            intent.putExtra("title", titleTableName)
            intent.putExtra("number", if (showInfo.number == 0) "" else "共 ${showInfo.number} 条") //减一吧直接
            context.startActivity(intent)
            (context as Activity).finish()
            context.overridePendingTransition(0, 0)
        }
        dialog.dismiss()
    }
    dialog.show()
}

//动态设置LinearLayout高度
fun setLLHeight(context: Context, heightInDp: Int, linearLayout: LinearLayout) {
    val density = context.resources.displayMetrics.density
    val heightInPixels = (heightInDp * density).toInt()
    val layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        heightInPixels
    )
    linearLayout.layoutParams = layoutParams
}

fun dropKeyBoard(context: Context, editText: EditText) {
    editText.clearFocus()
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(editText.windowToken, 0)
}

fun showKeyBoard(context: Context, editText: EditText) {
    editText.requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
}

//笔记导出为图片
//假设默认最多30行，分批导出
fun exportTextToPhoto(context: Context, rawTextList: List<String>, time: String) {

    val tempColorList = when (THEME_INDEX) {
        0 -> listOf(
            context.resources.getColor(R.color.redFirstColor, context.theme),
            context.resources.getColor(R.color.redSecondColor, context.theme)
        )

        1 -> listOf(
            context.resources.getColor(R.color.orangeFirstColor, context.theme),
            context.resources.getColor(R.color.orangeSecondColor, context.theme)
        )

        2 -> listOf(
            context.resources.getColor(R.color.yellowFirstColor, context.theme),
            context.resources.getColor(R.color.yellowSecondColor, context.theme)
        )

        3 -> listOf(
            context.resources.getColor(R.color.greenFirstColor, context.theme),
            context.resources.getColor(R.color.greenSecondColor, context.theme)
        )

        4 -> listOf(
            context.resources.getColor(R.color.cyanFirstColor, context.theme),
            context.resources.getColor(R.color.cyanSecondColor, context.theme)
        )

        5 -> listOf(
            context.resources.getColor(R.color.blueFirstColor, context.theme),
            context.resources.getColor(R.color.blueSecondColor, context.theme)
        )

        6 -> listOf(
            context.resources.getColor(R.color.purpleFirstColor, context.theme),
            context.resources.getColor(R.color.purpleSecondColor, context.theme)
        )

        7 -> listOf(
            context.resources.getColor(R.color.darkFirstColor, context.theme),
            context.resources.getColor(R.color.darkSecondColor, context.theme)
        )

        else -> listOf(
            context.resources.getColor(R.color.greenFirstColor, context.theme),
            context.resources.getColor(R.color.greenSecondColor, context.theme)
        )
    }

    val fullScreenWidth = context.resources.displayMetrics.widthPixels
    val fullScreenHeight = context.resources.displayMetrics.heightPixels
    val tempHeightList = mutableListOf<Float>()
    for (text in rawTextList) {
        val width = fullScreenWidth - 200
        val textPaint = TextPaint().apply {
            textSize = 45F
            color = Color.WHITE
            isFakeBoldText = true
        }
        val textWidth = textPaint.measureText(text)
        val lineHeight = textPaint.fontMetrics.bottom - textPaint.fontMetrics.top
        val lineCount = ceil(textWidth / width.toDouble()).toInt()
        val textHeight = lineCount * lineHeight
        tempHeightList.add(textHeight)
    }
    val totalHeight = tempHeightList.sum() + (rawTextList.size + 1) * 90
    //绘制开发标准背景
    val bitmap = Bitmap.createBitmap(
        fullScreenWidth,
        fullScreenHeight,
        Bitmap.Config.RGBA_F16
    ) //RGBA_F16精度更高 ARGB_8888的平衡更好
    val canvas = Canvas(bitmap)
    //绘制基本元素
    run { //绘制背景
        val margin = 100F //左右margin 网易云歌词图片最小1080
        val radius = 50F //圆角
        val height = maxOf(totalHeight, 1080F)
        val foregroundPaint = Paint().apply {
            color = context.resources.getColor(tempColorList.first(), context.theme)
            style = Paint.Style.FILL
        }
        val rectHeight = height - margin * 2
        val rectTop = (fullScreenHeight.toFloat() - rectHeight) / 2
        val rectF2 = RectF(
            margin,
            rectTop,
            fullScreenWidth - margin,
            rectTop + rectHeight
        )
        canvas.drawRoundRect(rectF2, radius, radius, foregroundPaint)
        //多文本自动换行
        run {
            val textPaint = TextPaint().apply {
                textSize = 45F
                color = Color.WHITE
                isFakeBoldText = true
            }
            val maxWidth = fullScreenWidth - 2 * margin - 100 // 绘制区域的最大宽度
            val text = TextUtils.join("\n\n", rawTextList)
            val textLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, maxWidth.toInt())
                .build()
            canvas.save()
            canvas.translate(150F, rectTop + 50F) // 设置绘制的起始位置
            textLayout.draw(canvas)
            canvas.restore()
        }
        //绘制底板
        run {
            val temp = rectTop + rectHeight
            val foregroundPaint2 = Paint().apply {
                color = context.resources.getColor(tempColorList.last(), context.theme)
                style = Paint.Style.FILL
            }
            val rectF3 = RectF(
                100F,
                temp - 140F,
                fullScreenWidth - 100F,
                temp
            )
            canvas.drawRoundRect(rectF3, 50F, 50F, foregroundPaint2)
        }
        // 绘制Logo
        val imageRect = run {
            val bitmapImage = BitmapFactory.decodeResource(context.resources, R.drawable.todo)
            val temp = (rectTop + rectHeight).toInt()
            val imageRect = Rect(120, temp - 120, 220, temp - 20) //左下角
            canvas.drawBitmap(bitmapImage, null, imageRect, null)
            imageRect
        }
        //绘制TODOL
        run {
            val text = "TODOL"
            val textPaint = Paint().apply {
                textSize = 50F
                color = Color.WHITE
                isFakeBoldText = true
            }
            val textRect = Rect()
            textPaint.getTextBounds(text, 0, text.length, textRect)
            val textX = imageRect.left.toFloat() + 110
            val textY = imageRect.bottom.toFloat() - 30 // 文字的垂直位置，可以根据需要调整
            canvas.drawText(text, textX, textY, textPaint)
        }
        //绘制日期
        run {
            val textPaint = Paint().apply {
                textSize = 50F
                color = Color.WHITE
                isFakeBoldText = true
            }
            val textRect = Rect()
            textPaint.getTextBounds(time, 0, time.length, textRect)
            val temp = when (time.length) {
                9 -> 510 //2024年1月1日
                10 -> 495 //2024年1月15日
                11 -> 470 //2024年11月15日
                else -> 470
            }
            val textX = imageRect.left.toFloat() + temp //2024年11月15日
            val textY = imageRect.bottom.toFloat() - 30 // 文字的垂直位置，可以根据需要调整
            canvas.drawText(time, textX, textY, textPaint)
        }
    }
    //导出到相册
    run {
        val imageUri =
            MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "TODOL_$time", null)
        MediaScannerConnection.scanFile(
            context.applicationContext,
            arrayOf(imageUri),
            null,
            null
        )
    }
}

//生成UUID 使用纳秒保证了时间的统一性
fun getUUID(): String {
    val tempLocalDate = LocalDate.now()
    val tempLocalTime = LocalTime.now()
    return "SON_${tempLocalDate.year}_${tempLocalDate.monthValue}_${tempLocalDate.dayOfMonth}_${tempLocalTime.hour}_${tempLocalTime.minute}_${tempLocalTime.second}_${
        tempLocalTime.nano.toString().replace("0", "")
    }"
}

//保守估计21
fun String.isSon() = this.length > 20

fun <E> List<E>.lastTwo() = if (this.size >= 2) this[this.size - 2].toString() else this.toString()

//这种拓展函数真的很好用
fun String.isMyTable() = this.startsWith("S") || this.startsWith("L") || this.startsWith("_")
