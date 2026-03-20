package com.perflab.sqllab.application.dto

data class LogSearchResponse(
    val id: Long,
    val userId: Long,
    val action: String,
    val status: String,
    val createdAt: String
)
