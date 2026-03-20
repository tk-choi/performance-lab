package com.perflab.sqllab.presentation.api

import com.perflab.sqllab.application.scenario.fulltablescan.FullTableScanService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sql/users/search")
class UserSearchController(
    private val fullTableScanService: FullTableScanService
) {
    @GetMapping("/before")
    fun searchBefore(@RequestParam name: String) =
        fullTableScanService.searchBefore(name)

    @GetMapping("/after")
    fun searchAfter(@RequestParam name: String) =
        fullTableScanService.searchAfter(name)
}
