package com.perflab.sqllab.application.dto

data class QueryResultResponse<T>(
    val scenario: String,
    val version: String,
    val data: T,
    val diagnosis: DiagnosisResponse,
    val dataCount: Int
)
