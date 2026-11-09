package ru.automaster.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cars",
    foreignKeys = [
        ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("clientId"), Index(value = ["vin"]), Index(value = ["licensePlateSearchIndex"])]
)
data class CarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val licensePlate: String,
    val licensePlateSearchIndex: String,
    val vin: String,
    val make: String,
    val model: String,
    val year: Int? = null,
    val color: String? = null,
    val mileage: Int? = null,
    val fuelLevel: Int? = null,
    val notes: String? = null
)