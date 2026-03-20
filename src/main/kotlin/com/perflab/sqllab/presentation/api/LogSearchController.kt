package com.perflab.sqllab.presentation.api

import com.perflab.sqllab.application.scenario.compositeindex.CompositeIndexService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/sql/logs/search")
class LogSearchController(
    private val compositeIndexService: CompositeIndexService
) {
    @GetMapping("/before")
    fun searchBefore(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ) = compositeIndexService.searchBefore(from, to)

    @GetMapping("/after")
    fun searchAfter(
        @RequestParam status: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ) = compositeIndexService.searchAfter(status, from, to)
}
