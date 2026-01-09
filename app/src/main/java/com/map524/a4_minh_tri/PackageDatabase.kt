package com.map524.a4_minh_tri

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Package::class], version = 1)
abstract class PackageDatabase: RoomDatabase() {
    abstract fun packageDao(): PackageDao
    companion object {
        private var INSTANCE: PackageDatabase? = null
        fun getInstance(context: Context): PackageDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    PackageDatabase::class.java,
                    "package_database"
                ).build().also { INSTANCE = it }
            }
        }
        fun getInstance(){
            return
        }
    }
}