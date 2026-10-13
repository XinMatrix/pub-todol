package com.todol

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.todol.mainPage.activity.THEME_INDEX

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler(MyExceptionHandler(this))

        //设置主题配色
        val getTheme = getGlobalData(this, "THEME_INDEX", Int) as Int
        THEME_INDEX = if (getTheme < 0) 3 else getTheme //如果有这行代码，说不定我的数据就不会丢了
        setTheme(
            when (THEME_INDEX) { //红橙黄绿青蓝紫
                0 -> R.style.Theme_TODOL_RED
                1 -> R.style.Theme_TODOL_ORANGE
                2 -> R.style.Theme_TODOL_YELLOW
                3 -> R.style.Theme_TODOL_GREEN
                4 -> R.style.Theme_TODOL_CYAN
                5 -> R.style.Theme_TODOL_BLUE
                6 -> R.style.Theme_TODOL_PURPLE
                else -> R.style.Theme_TODOL_DARK
            }
        )
    }
}
