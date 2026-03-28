package com.zavscom.adonidirectory.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val subCategory: String?,
    val address: String,
    val area: String,
    val pincode: String,
    val city: String,
    val state: String,
    val phone: String?,
    val whatsapp: String?,
    val email: String?,
    val website: String?,
    val latitude: Double?,
    val longitude: Double?,
    val source: String,
    val lastSeenAt: String,
    @ColumnInfo(name = "extra_json") val extraJson: String?,
    @ColumnInfo(name = "is_favorite", defaultValue = "0") val isFavorite: Boolean = false,
)
