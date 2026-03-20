package com.perflab.sqllab.application.scenario.fulltablescan

import com.perflab.sqllab.application.dto.DiagnosisResponse
import com.perflab.sqllab.application.dto.QueryResultResponse
import com.perflab.sqllab.application.dto.UserSearchResponse
import com.perflab.sqllab.domain.model.User
import com.perflab.sqllab.domain.repository.UserRepository
import com.perflab.sqllab.domain.service.ExplainAnalyzeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FullTableScanService(
    private val userRepository: UserRepository,
    private val explainAnalyzeService: ExplainAnalyzeService
) {
    fun searchBefore(name: String): QueryResultResponse<List<UserSearchResponse>> {
        val sql = "SELECT * FROM users WHERE name LIKE CONCAT('%', ?, '%')"
        val diagnosis = explainAnalyzeService.analyze(sql, "%$name%")
        val users = userRepository.findByNameContaining(name)
            .map { it.toResponse() }

        return QueryResultResponse(
            scenario = "Full Table Scan",
            version = "before",
            data = users,
            diagnosis = DiagnosisResponse.from(diagnosis),
            dataCount = users.size
        )
    }

    fun searchAfter(name: String): QueryResultResponse<List<UserSearchResponse>> {
        val sql = "SELECT * FROM users WHERE name = ?"
        val diagnosis = explainAnalyzeService.analyze(sql, name)
        val users = userRepository.findByName(name)
            .map { it.toResponse() }

        return QueryResultResponse(
            scenario = "Full Table Scan",
            version = "after",
            data = users,
            diagnosis = DiagnosisResponse.from(diagnosis),
            dataCount = users.size
        )
    }

    private fun User.toResponse() = UserSearchResponse(
        id = id,
        name = name,
        email = email
    )
}
