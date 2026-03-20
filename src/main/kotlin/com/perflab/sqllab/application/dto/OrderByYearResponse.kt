package com.perflab.sqllab.application.dto

data class OrderByYearResponse(
    val id: Long,
    val userId: Long,
    val productName: String,
    val amount: Int,
    val createdAt: String
)
