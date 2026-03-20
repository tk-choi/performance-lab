package com.perflab.sqllab.application.dto

import com.perflab.sqllab.domain.model.QueryDiagnosis

data class DiagnosisResponse(
    val explainAnalyze: String,
    val accessType: String,
    val estimatedRows: Long,
    val executionTimeMs: Double
) {
    companion object {
        fun from(diagnosis: QueryDiagnosis) = DiagnosisResponse(
            explainAnalyze = diagnosis.explainAnalyze,
            accessType = diagnosis.accessType.name,
            estimatedRows = diagnosis.estimatedRows,
            executionTimeMs = diagnosis.executionTimeMs
        )
    }
}
