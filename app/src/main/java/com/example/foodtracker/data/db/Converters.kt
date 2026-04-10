package com.example.foodtracker.data.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    @TypeConverter fun toDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun fromDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter fun toDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)
    @TypeConverter fun fromDateTime(dt: LocalDateTime?): String? = dt?.toString()
}