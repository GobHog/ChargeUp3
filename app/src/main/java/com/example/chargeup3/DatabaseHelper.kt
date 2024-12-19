package com.example.chargeup3

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // Этот метод вызывается для создания базы данных
    override fun onCreate(db: SQLiteDatabase) {
        val createWorkoutTable = """
            CREATE TABLE workout (
                id INTEGER PRIMARY KEY AUTOINCREMENT, 
                name TEXT NOT NULL,
                image_path TEXT,
                time_work INTEGER NOT NULL,
                time_relax INTEGER NOT NULL
            )
        """

        val createExerciseTable = """
            CREATE TABLE exercise (
                id INTEGER PRIMARY KEY AUTOINCREMENT, 
                name TEXT NOT NULL,
                image_path TEXT,
                workout_id INTEGER,
                FOREIGN KEY (workout_id) REFERENCES workout(id)
            )
        """

        db.execSQL(createWorkoutTable)
        db.execSQL(createExerciseTable)
    }

    // Этот метод вызывается, если версия базы данных изменилась
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 3) {
        }
    }

    companion object {
        private const val DATABASE_NAME = "workout_db" // Имя базы данных
        private const val DATABASE_VERSION = 3        // Версия базы данных увеличена до 2
    }
}
