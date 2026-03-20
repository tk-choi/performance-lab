package com.perflab.sqllab.application.scenario.nplusone

import com.perflab.sqllab.application.dto.CommentResponse
import com.perflab.sqllab.application.dto.DiagnosisResponse
import com.perflab.sqllab.application.dto.PostWithCommentsResponse
import com.perflab.sqllab.application.dto.QueryResultResponse
import com.perflab.sqllab.domain.repository.PostRepository
import com.perflab.sqllab.domain.service.ExplainAnalyzeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NplusOneService(
    private val postRepository: PostRepository,
    private val explainAnalyzeService: ExplainAnalyzeService
) {
    fun searchBefore(): QueryResultResponse<List<PostWithCommentsResponse>> {
        val sql = "SELECT * FROM posts"
        val diagnosis = explainAnalyzeService.analyze(sql)
        val posts = postRepository.findAll()
            .map { post ->
                PostWithCommentsResponse(
                    id = post.id,
                    title = post.title,
                    commentCount = post.comments.size,
                    comments = post.comments.map { CommentResponse(it.id, it.content) }
                )
            }

        return QueryResultResponse(
            scenario = "N+1 Problem",
            version = "before",
            data = posts,
            diagnosis = DiagnosisResponse.from(diagnosis),
            dataCount = posts.size
        )
    }

    fun searchAfter(): QueryResultResponse<List<PostWithCommentsResponse>> {
        val sql = "SELECT DISTINCT p.* FROM posts p JOIN comments c ON p.id = c.post_id"
        val diagnosis = explainAnalyzeService.analyze(sql)
        val posts = postRepository.findAllWithComments()
            .map { post ->
                PostWithCommentsResponse(
                    id = post.id,
                    title = post.title,
                    commentCount = post.comments.size,
                    comments = post.comments.map { CommentResponse(it.id, it.content) }
                )
            }

        return QueryResultResponse(
            scenario = "N+1 Problem",
            version = "after",
            data = posts,
            diagnosis = DiagnosisResponse.from(diagnosis),
            dataCount = posts.size
        )
    }
}
