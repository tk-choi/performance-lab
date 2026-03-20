package com.perflab.sqllab.presentation.api

import com.perflab.sqllab.application.scenario.nplusone.NplusOneService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sql/posts/with-comments")
class PostWithCommentsController(
    private val nplusOneService: NplusOneService
) {
    @GetMapping("/before")
    fun searchBefore() =
        nplusOneService.searchBefore()

    @GetMapping("/after")
    fun searchAfter() =
        nplusOneService.searchAfter()
}
