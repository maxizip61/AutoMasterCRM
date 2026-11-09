package ru.automaster.app.data.local

import androidx.room.TypeConverter
import ru.automaster.app.domain.model.*
import java.math.BigDecimal
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = OrderStatus.valueOf(value)

    @TypeConverter
    fun fromStagePhase(value: StagePhase): String = value.name

    @TypeConverter
    fun toStagePhase(value: String): StagePhase = StagePhase.valueOf(value)

    @TypeConverter
    fun fromStageStatus(value: StageStatus): String = value.name

    @TypeConverter
    fun toStageStatus(value: String): StageStatus = StageStatus.valueOf(value)

    @TypeConverter
    fun fromPostName(value: PostName): String = value.name

    @TypeConverter
    fun toPostName(value: String): PostName = PostName.valueOf(value)

    @TypeConverter
    fun fromPostType(value: PostType): String = value.name

    @TypeConverter
    fun toPostType(value: String): PostType = PostType.valueOf(value)

    @TypeConverter
    fun fromPaymentType(value: PaymentType): String = value.name

    @TypeConverter
    fun toPaymentType(value: String): PaymentType = PaymentType.valueOf(value)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter
    fun fromPhotoType(value: PhotoType): String = value.name

    @TypeConverter
    fun toPhotoType(value: String): PhotoType = PhotoType.valueOf(value)

    @TypeConverter
    fun fromAccrualType(value: AccrualType): String = value.name

    @TypeConverter
    fun toAccrualType(value: String): AccrualType = AccrualType.valueOf(value)

    @TypeConverter
    fun fromTaxPaymentKind(value: TaxPaymentKind): String = value.name

    @TypeConverter
    fun toTaxPaymentKind(value: String): TaxPaymentKind = TaxPaymentKind.valueOf(value)

    @TypeConverter
    fun fromOrderUnit(value: OrderUnit): String = value.name

    @TypeConverter
    fun toOrderUnit(value: String): OrderUnit = OrderUnit.valueOf(value)
}