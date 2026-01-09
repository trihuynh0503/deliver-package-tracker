package com.map524.a4_minh_tri

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PackageDao {
    @Insert
    suspend fun insert(newPackage: Package)
    @Update
    suspend fun update(selectedPackage: Package)

    @Delete
    suspend fun delete(selectedPackage: Package)

    @Query("SELECT * FROM package")
    suspend fun getAllPackage(): List<Package>

    @Query("SELECT * FROM package WHERE deliveryStatus LIKE :searchQuery")
    suspend fun searchPackagesByStatus(searchQuery: DeliveryStatus): List<Package>
    @Query("SELECT * FROM package WHERE id = :id")
    suspend fun getPackageById(id: Int): Package
}