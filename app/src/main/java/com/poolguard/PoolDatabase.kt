package com.poolguard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PoolDatabase(context: Context) : SQLiteOpenHelper(context, "poolguard.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE pools (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, client TEXT, phone TEXT, days TEXT, completed INTEGER DEFAULT 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
        db.execSQL("DROP TABLE IF EXISTS pools")
        onCreate(db)
    }

    fun save(p: PoolData) {
        val cv = ContentValues()
        cv.put("name", p.name)
        cv.put("client", p.client)
        cv.put("phone", p.phone)
        cv.put("days", p.days)
        cv.put("completed", if (p.completed) 1 else 0)
        writableDatabase.insert("pools", null, cv)
    }

    fun getAll(): List<PoolData> {
        val list = mutableListOf<PoolData>()
        val c = readableDatabase.rawQuery("SELECT * FROM pools", null)
        while (c.moveToNext()) {
            list.add(PoolData(c.getInt(0), c.getString(1)?:"", c.getString(2)?:"", c.getString(3)?:"", c.getString(4)?:"", c.getInt(5) == 1))
        }
        c.close()
        return list
    }

    fun updateCompleted(id: Int, completed: Boolean) {
        val cv = ContentValues()
        cv.put("completed", if (completed) 1 else 0)
        writableDatabase.update("pools", cv, "id = ?", arrayOf(id.toString()))
    }
}

data class PoolData(
    val id: Int = 0,
    val name: String,
    val client: String,
    val phone: String,
    val days: String,
    var completed: Boolean = false
)
