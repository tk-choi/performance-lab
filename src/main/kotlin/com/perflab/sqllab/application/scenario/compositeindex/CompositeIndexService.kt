package com.perflab.sqllab.application.scenario.compositeindex

import com.perflab.sqllab.application.dto.DiagnosisResponse
import com.perflab.sqllab.application.dto.LogSearchResponse
import com.perflab.sqllab.application.dto.QueryResultResponse
import com.perflab.sqllab.domain.repository.LogRepository
import com.perflab.sqllab.domain.service.ExplainAnalyzeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class CompositeIndexService(
    private val logRepository: LogRepository,
    private val explainAnalyzeService: ExplainAnalyzeService
) {
    fun searchBefore(from: LocalDate, to: LocalDate): QueryResultResponse<List<LogSearchResponse>> {
        val start = from.atStartOfDay()
        val end = to.atTime(LocalTime.MAX)
        val sql = "SELECT * FROM logs WHERE created_at BETWEEN ? AND ?"
        val diagnosis = explainAnalyzeService.analyze(sql, start, end)
        val logs = logRepository.findByCreatedAtBetween(start, end)
            .map { it.toResponse() }

        return QueryResultResponse(
            scenario = "Composite Index",
            version = "before",
            data = logs,
            diagnosis = DiagnosisResponse.from(diagnosis),
            dataCount = logs.size
        )
    }

    fun searchAfter(status: String, from: LocalDate, to: LocalDate): QueryResultResponse<List<LogSearchResponse>> {
        val start = from.atStartOfDay()
        val end = to.atTime(LocalTime.MAX)
        val sql = "SELECT * FROM logs WHERE status = ? AND created_at BETWEEN ? AND ?"
        val diagnosis = explainAnalyzeService.analyze(sql, status, start, end)
        val logs = logRepository.findByStatusAndCreatedAtBetween(status, start, end)
            .map { it.toResponse() }

        return QueryResultResponse(
            scenario = "Composite Index",
            version = "after",
            data = logs,
            diagnosis = DiagnosisResponse.from(diagnosis),
            dataCount = logs.size
        )
    }

    private fun com.perflab.sqllab.domain.model.Log.toResponse() = LogSearchResponse(
        id = id,
        userId = userId,
        action = action,
        status = status,
        createdAt = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}
