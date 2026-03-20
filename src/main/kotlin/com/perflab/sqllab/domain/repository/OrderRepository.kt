package com.perflab.sqllab.domain.repository

import com.perflab.sqllab.domain.model.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface OrderRepository : JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE YEAR(o.createdAt) = :year")
    fun findByYear(@Param("year") year: Int): List<Order>

    fun findByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<Order>
}
