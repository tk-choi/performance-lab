package com.perflab.sqllab.domain.model

data class QueryDiagnosis(
    val explainAnalyze: String,
    val accessType: AccessType,
    val estimatedRows: Long,
    val executionTimeMs: Double
)
