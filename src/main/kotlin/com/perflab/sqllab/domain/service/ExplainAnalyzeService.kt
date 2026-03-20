package com.perflab.sqllab.domain.service

import com.perflab.sqllab.domain.model.QueryDiagnosis

interface ExplainAnalyzeService {
    fun analyze(sql: String, vararg params: Any?): QueryDiagnosis
}
