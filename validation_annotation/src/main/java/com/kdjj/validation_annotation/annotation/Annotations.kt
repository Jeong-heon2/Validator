package com.kdjj.validation_annotation.annotation


@Target(AnnotationTarget.CLASS)
annotation class Validation

// String field
@Target(AnnotationTarget.FIELD)
annotation class MinLength(
    val length: Int,
)


@Target(AnnotationTarget.FIELD)
annotation class MaxLength(
    val length: Int,
)


@Target(AnnotationTarget.FIELD)
annotation class Regex(
    val regex: String,
)

// Int field
@Target(AnnotationTarget.FIELD)
annotation class MaxInt(
    val value: Int,
)


@Target(AnnotationTarget.FIELD)
annotation class MinInt(
    val value: Int,
)

// float field

