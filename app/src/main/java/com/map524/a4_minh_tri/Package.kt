package com.map524.a4_minh_tri

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "package")
data class Package (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val deliveryStatus: DeliveryStatus,
    val deliveryDate: String
): Serializable
