package com.kdjj.validator.data

import com.kdjj.validation_annotation.annotation.*


@Validation
data class User (
    @MinLength(10)
    @MaxLength(40)
    @Regex("^*")
    val name: String,

    @MinInt(5)
    @MaxInt(10)
    val age: Int,

    @MinFloat(5f)
    @MaxFloat(10f)
    val score: Float,

    @MinDouble(5.0)
    @MaxDouble(100.0)
    val percentage: Double
)