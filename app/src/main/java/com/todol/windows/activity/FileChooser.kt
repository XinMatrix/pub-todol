package com.todol.windows.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.todol.R
import com.todol.database.MyDatabaseHelper
import com.todol.mainPage.activity.THEME_INDEX
import com.todol.myTableName
import com.todol.myToastLong
import com.todol.saveFileToInternalStorage
import com.todol.startAnim
import com.todol.updateFilePath
import com.todol.windows.adapter.choosePosition
import com.todol.windows.service.InputWindows
import com.todol.windows.service.addAniCheck
import com.todol.windows.service.itemList

var FILE_CHOOSER_CHECK = false

class FileChooser : AppCompatActivity() {

    //数据库
    private lateinit var db: SQLiteDatabase

    private lateinit var imageTV: TextView
    private lateinit var audioTV: TextView
    private lateinit var videoTV: TextView

    private lateinit var ensureLL: LinearLayout

    private lateinit var chooserType: String

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_chooser)
        initViews()
        initListeners()

        run { //设置配色
            val tempThemeId = when (THEME_INDEX) {
                0 -> R.color.redFirstColor //这个刚刚好 greenSecondColor 淡一些
                1 -> R.color.orangeFirstColor
                2 -> R.color.yellowFirstColor
                3 -> R.color.greenFirstColor
                4 -> R.color.cyanFirstColor
                5 -> R.color.blueFirstColor
                6 -> R.color.purpleFirstColor
                else -> R.color.darkFirstColor
            }
            val color = ContextCompat.getColor(this, tempThemeId)
            ensureLL.setBackgroundColor(color)
        }

        db = MyDatabaseHelper(this).writableDatabase

        //检测长按更新
        when (intent.getStringExtra("Update") ?: "") {
            "Image" -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                chooserType = "image"
                intent.type = "${chooserType}/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                fileChooserLauncher.launch(intent)
            }

            "Audio" -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                chooserType = "audio"
                intent.type = "${chooserType}/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                fileChooserLauncher.launch(intent)
            }

            "Video" -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                chooserType = "video"
                intent.type = "${chooserType}/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                fileChooserLauncher.launch(intent)
            }

            else -> {

            }
        }
    }

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedFileUri: Uri? = result.data?.data
            if (selectedFileUri != null) {
                val documentFile = DocumentFile.fromSingleUri(this, selectedFileUri)
                val filePath = documentFile?.uri?.path
                var fileName = documentFile?.name // 获取文件名称
                if (chooserType == "audio") if ("." !in fileName.toString()) fileName = "$fileName.mp3" //音频默认仅支持mp3——如果没有后缀的话
                if (chooserType == "video") if ("." !in fileName.toString()) fileName = "${filePath!!.substringAfterLast("/")}.mp4"
                val savePath = run {
                    val downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    downloadDirectory.absolutePath + "/TODOL_SP_DATA/$chooserType/$fileName"
                }
                if (filePath != null)  //关闭程序，启动InputWindows
                    saveFileToInternalStorage(this@FileChooser, selectedFileUri, savePath) { isFinished, file ->
                        if (isFinished) { //保存至TODOL目录下 再重新启动InputWindows
                            updateFilePath(db, myTableName, itemList[choosePosition].id, file!!) //更新filePath
                            val intent = Intent(this, InputWindows::class.java)
                            startService(intent)
                            addAniCheck = false
                        } else myToastLong(this, "文件读取失败")
                    }
            }
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun initListeners() { //这个页面不能滑动
        //任何文件 */*
        //图像文件 image/*  //jpeg png gif bmp webp
        //音频文件 audio/*  //mpeg wav ogg
        //视频文件 video/*  //mp4 mpeg 3gpp
        //文本文件 text/*  //plain html xml csv
        //PDF文件 application/pdf
        //Word文档 application/msword
        //Excel电子表格 application/vnd.ms-excel
        //Excel电子表格 application/vnd.ms-powerpoint
        imageTV.setOnClickListener {
            startAnim(this, it)
            chooserType = "image"
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "${chooserType}/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            fileChooserLauncher.launch(intent)
        }
        audioTV.setOnClickListener {
            startAnim(this, it)
            chooserType = "audio"
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "${chooserType}/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            fileChooserLauncher.launch(intent)
        }
        videoTV.setOnClickListener {
            startAnim(this, it)
            chooserType = "video"
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "${chooserType}/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            fileChooserLauncher.launch(intent)
        }

        ensureLL.setOnClickListener { //用户主动取消选择
            finish()
            overridePendingTransition(0, 0) //这个要快一点
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() {
        imageTV = findViewById(R.id.fileChooserImageTV)
        audioTV = findViewById(R.id.fileChooserAudioTV)
        videoTV = findViewById(R.id.fileChooserVideoTV)

        ensureLL = findViewById(R.id.fileChooserEnsureLL)
    }

    override fun onDestroy() {
        super.onDestroy()
        //回到悬浮窗
        FILE_CHOOSER_CHECK = false
        val intent = Intent(this, InputWindows::class.java)
        startService(intent)
        addAniCheck = false
    }

}