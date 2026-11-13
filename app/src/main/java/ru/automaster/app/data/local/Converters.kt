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
    fun toBigDecimal(value: String?): BigDecimal? = try {
        value?.let { BigDecimal(it) }
    } catch (e: NumberFormatException) {
        BigDecimal.ZERO
    }

    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = try {
        OrderStatus.valueOf(value)
    } catch (e: IllegalArgumentException) {
        OrderStatus.ACCEPTED
    }

    @TypeConverter
    fun fromStagePhase(value: StagePhase): String = value.name

    @TypeConverter
    fun toStagePhase(value: String): StagePhase = try {
        StagePhase.valueOf(value)
    } catch (e: IllegalArgumentException) {
        StagePhase.BODY
    }

    @TypeConverter
    fun fromStageStatus(value: StageStatus): String = value.name

    @TypeConverter
    fun toStageStatus(value: String): StageStatus = try {
        StageStatus.valueOf(value)
    } catch (e: IllegalArgumentException) {
        StageStatus.PLANNED // ИСПРАВЛЕНО
    }

    @TypeConverter
    fun fromPostName(value: PostName): String = value.name

    @TypeConverter
    fun toPostName(value: String): PostName = try {
        PostName.valueOf(value)
    } catch (e: IllegalArgumentException) {
        PostName.PREP_1
    }

    @TypeConverter
    fun fromPostType(value: PostType): String = value.name

    @TypeConverter
    fun toPostType(value: String): PostType = try {
        PostType.valueOf(value)
    } catch (e: IllegalArgumentException) {
        PostType.PREP
    }

    @TypeConverter
    fun fromPaymentType(value: PaymentType): String = value.name

    @TypeConverter
    fun toPaymentType(value: String): PaymentType = try {
        PaymentType.valueOf(value)
    } catch (e: IllegalArgumentException) {
        PaymentType.PAYMENT
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (e: IllegalArgumentException) {
        PaymentMethod.CASH
    }

    @TypeConverter
    fun fromPhotoType(value: PhotoType): String = value.name

    @TypeConverter
    fun toPhotoType(value: String): PhotoType = try {
        PhotoType.valueOf(value)
    } catch (e: IllegalArgumentException) {
        PhotoType.BEFORE
    }

    @TypeConverter
    fun fromAccrualType(value: AccrualType): String = value.name

    @TypeConverter
    fun toAccrualType(value: String): AccrualType = try {
        AccrualType.valueOf(value)
    } catch (e: IllegalArgumentException) {
        AccrualType.PERCENT
    }

    @TypeConverter
    fun fromTaxPaymentKind(value: TaxPaymentKind): String = value.name

    @TypeConverter
    fun toTaxPaymentKind(value: String): TaxPaymentKind = try {
        TaxPaymentKind.valueOf(value)
    } catch (e: IllegalArgumentException) {
        TaxPaymentKind.USN_TAX // ИСПРАВЛЕНО
    }

    @TypeConverter
    fun fromOrderUnit(value: OrderUnit): String = value.name

    @TypeConverter
    fun toOrderUnit(value: String): OrderUnit = try {
        OrderUnit.valueOf(value)
    } catch (e: IllegalArgumentException) {
        OrderUnit.PCS // ИСПРАВЛЕНО
    }
}