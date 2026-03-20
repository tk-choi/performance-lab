package com.perflab.sqllab.application.dto

data class PostWithCommentsResponse(
    val id: Long,
    val title: String,
    val commentCount: Int,
    val comments: List<CommentResponse>
)

data class CommentResponse(
    val id: Long,
    val content: String
)
