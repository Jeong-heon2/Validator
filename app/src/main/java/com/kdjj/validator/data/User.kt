package com.kdjj.validator.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
    val luck: Float,

    @MinDouble(5.0)
    @MaxDouble(100.0)
    val power: Double,

    @MinLong(5L)
    @MaxLong(100L)
    val speed: Long,


    @MinLong(4L)
    val mutableLiveData: MutableLiveData<Long>,
    val liveData: LiveData<String>,
)