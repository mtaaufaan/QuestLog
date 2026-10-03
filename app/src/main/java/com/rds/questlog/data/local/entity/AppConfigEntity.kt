package com.rds.questlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Rows: is_premium ('0'|'1'), purchase_token, purchase_verified_at (epoch). */
@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val key: String,
    val value: String,
)
