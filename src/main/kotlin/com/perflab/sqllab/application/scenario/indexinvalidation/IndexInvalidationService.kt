package com.perflab.sqllab.application.scenario.indexinvalidation

import com.perflab.sqllab.application.dto.DiagnosisResponse
import com.perflab.sqllab.application.dto.OrderByYearResponse
import com.perflab.sqllab.application.dto.QueryResultResponse
import com.perflab.sqllab.domain.repository.OrderRepository
import com.perflab.sqllab.domain.service.ExplainAnalyzeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class IndexInvalidationService(
    private val orderRepository: OrderRepository,
    private val explainAnalyzeService: ExplainAnalyzeService
) {
    fun searchBefore(year: Int): QueryResultResponse<List<OrderByYearResponse>> {
        val sql = "SELECT * FROM orders WHERE YEAR(created_at) = ?"
        val diagnosis = explainAnalyzeService.analyze(sql, year)
        val orders = orderRepository.findByYear(year)
            .map { it.toResponse() }

        return QueryResultResponse(
            scenario = "Index Invalidation",
            version = "before",
            data = orders,
            diagnosis = DiagnosisResponse.from(diagnosis),
            dataCount = orders.size
        )
    }

    fun searchAfter(year: Int): QueryResultResponse<List<OrderByYearResponse>> {
        val start = LocalDateTime.of(year, 1, 1, 0, 0, 0)
        val end = LocalDateTime.of(year, 12, 31, 23, 59, 59)
        val sql = "SELECT * FROM orders WHERE created_at BETWEEN ? AND ?"
        val diagnosis = explainAnalyzeService.analyze(sql, start, end)
        val orders = orderRepository.findByCreatedAtBetween(start, end)
            .map { it.toResponse() }

        return QueryResultResponse(
            scenario = "Index Invalidation",
            version = "after",
            data = orders,
            diagnosis = DiagnosisResponse.from(diagnosis),
            dataCount = orders.size
        )
    }

    private fun com.perflab.sqllab.domain.model.Order.toResponse() = OrderByYearResponse(
        id = id,
        userId = userId,
        productName = productName,
        amount = amount,
        createdAt = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}
