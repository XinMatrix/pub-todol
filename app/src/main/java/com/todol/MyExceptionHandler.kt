package com.todol

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter

//捕捉全局错误信息，复制到剪切板
class MyExceptionHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val tempHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 获取崩溃信息
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val crashLog = sw.toString()

        // 将崩溃信息保存到剪贴板
        context.copyToClipboard(crashLog, "TODOL崩溃信息")

        // 调用默认的异常处理程序处理崩溃
        tempHandler?.uncaughtException(thread, throwable)
    }
}