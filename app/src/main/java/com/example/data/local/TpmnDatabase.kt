package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        AiPolicyEntity::class,
        AiChatMessageEntity::class,
        AdminAuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TpmnDatabase : RoomDatabase() {
    abstract fun tpmnDao(): TpmnDao

    companion object {
        @Volatile
        private var INSTANCE: TpmnDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN canAiTransfer INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE users ADD COLUMN canAiBillPay INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE users ADD COLUMN canAiVoice INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE users ADD COLUMN canAiVault INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context): TpmnDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TpmnDatabase::class.java,
                    "tpmn_bank.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
