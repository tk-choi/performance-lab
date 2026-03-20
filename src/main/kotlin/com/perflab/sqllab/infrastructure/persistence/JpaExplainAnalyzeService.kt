package com.perflab.sqllab.infrastructure.persistence

import com.perflab.sqllab.domain.model.AccessType
import com.perflab.sqllab.domain.model.QueryDiagnosis
import com.perflab.sqllab.domain.service.ExplainAnalyzeService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class JpaExplainAnalyzeService(
    private val entityManager: EntityManager
) : ExplainAnalyzeService {

    override fun analyze(sql: String, vararg params: Any?): QueryDiagnosis {
        // EXPLAIN ANALYZE 실행
        val explainSql = "EXPLAIN ANALYZE $sql"
        val query = entityManager.createNativeQuery(explainSql)
        params.forEachIndexed { index, param ->
            query.setParameter(index + 1, param)
        }

        val startTime = System.nanoTime()
        val results = query.resultList
        val executionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0

        val explainOutput = results.joinToString("\n") { it.toString() }

        // EXPLAIN (non-ANALYZE) 실행하여 access type 추출
        val explainQuery = entityManager.createNativeQuery("EXPLAIN $sql")
        params.forEachIndexed { index, param ->
            explainQuery.setParameter(index + 1, param)
        }
        val explainResult = explainQuery.resultList

        var accessType = AccessType.ALL
        var estimatedRows = 0L

        if (explainResult.isNotEmpty()) {
            val row = explainResult[0] as Array<*>
            // EXPLAIN 결과: id, select_type, table, partitions, type, possible_keys, key, key_len, ref, rows, filtered, Extra
            if (row.size > 4) {
                accessType = AccessType.from(row[4]?.toString() ?: "ALL")
            }
            if (row.size > 9) {
                estimatedRows = row[9]?.toString()?.toLongOrNull() ?: 0L
            }
        }

        return QueryDiagnosis(
            explainAnalyze = explainOutput,
            accessType = accessType,
            estimatedRows = estimatedRows,
            executionTimeMs = executionTimeMs
        )
    }
}
