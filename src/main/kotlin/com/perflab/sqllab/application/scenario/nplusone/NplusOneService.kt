package com.perflab.sqllab.application.scenario.nplusone

import com.perflab.sqllab.application.dto.CommentResponse
import com.perflab.sqllab.application.dto.DiagnosisResponse
import com.perflab.sqllab.application.dto.PostWithCommentsResponse
import com.perflab.sqllab.application.dto.QueryResultResponse
import com.perflab.sqllab.domain.model.Post
import com.perflab.sqllab.domain.repository.PostRepository
import com.perflab.sqllab.domain.service.ExplainAnalyzeService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NplusOneService(
    private val postRepository: PostRepository,
    private val explainAnalyzeService: ExplainAnalyzeService
) {
    fun searchBefore(): QueryResultResponse<List<PostWithCommentsResponse>> {
        val sql = "SELECT * FROM posts LIMIT 100"
        val diagnosis = explainAnalyzeService.analyze(sql)
        val posts = postRepository.findAll(PageRequest.of(0, 100))
            .content
            .map { it.toResponse() }

        return QueryResultResponse(
            scenario = "N+1 Problem",
            version = "before",
            data = posts,
            diagnosis = DiagnosisResponse.from(diagnosis),
            dataCount = posts.size
        )
    }

    fun searchAfter(): QueryResultResponse<List<PostWithCommentsResponse>> {
        val sql = "SELECT DISTINCT p.* FROM posts p JOIN comments c ON p.id = c.post_id LIMIT 100"
        val diagnosis = explainAnalyzeService.analyze(sql)
        val posts = postRepository.findAllWithComments(PageRequest.of(0, 100))
            .map { it.toResponse() }

        return QueryResultResponse(
            scenario = "N+1 Problem",
            version = "after",
            data = posts,
            diagnosis = DiagnosisResponse.from(diagnosis),
            dataCount = posts.size
        )
    }

    private fun Post.toResponse() = PostWithCommentsResponse(
        id = id,
        title = title,
        commentCount = comments.size,
        comments = comments.map { CommentResponse(it.id, it.content) }
    )
}
