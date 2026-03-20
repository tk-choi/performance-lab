package com.perflab.sqllab.domain.repository

import com.perflab.sqllab.domain.model.Log
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface LogRepository : JpaRepository<Log, Long> {
    fun findByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<Log>
    fun findByStatusAndCreatedAtBetween(status: String, start: LocalDateTime, end: LocalDateTime): List<Log>
}
