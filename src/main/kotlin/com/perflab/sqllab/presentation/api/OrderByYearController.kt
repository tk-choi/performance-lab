package com.perflab.sqllab.presentation.api

import com.perflab.sqllab.application.scenario.indexinvalidation.IndexInvalidationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sql/orders/by-year")
class OrderByYearController(
    private val indexInvalidationService: IndexInvalidationService
) {
    @GetMapping("/before")
    fun searchBefore(@RequestParam year: Int) =
        indexInvalidationService.searchBefore(year)

    @GetMapping("/after")
    fun searchAfter(@RequestParam year: Int) =
        indexInvalidationService.searchAfter(year)
}
