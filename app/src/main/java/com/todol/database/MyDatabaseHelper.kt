package com.todol.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MyDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "TODOL_DATABASE.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        // 数据库第一次创建时调用，通常在此处创建表格等结构
        db?.execSQL(
            "create table SHORT(" +
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

        db?.execSQL(
            "create table LONG(" +
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

        //这个是SHOWINFO类型的
        db?.execSQL(
            "create table USER_STAR(" +
                    "id integer primary key autoincrement, " +
                    "endTime text not null, " +
                    "number text not null, " +
                    "content text not null," +
                    "startTime text not null," +
                    "differTime text not null," +
                    "place text not null," +
                    "filePath text not null)"
        )

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}

}
