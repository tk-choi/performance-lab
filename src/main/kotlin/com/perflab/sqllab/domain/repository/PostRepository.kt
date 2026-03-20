package com.perflab.sqllab.domain.repository

import com.perflab.sqllab.domain.model.Post
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PostRepository : JpaRepository<Post, Long> {
    @Query("SELECT DISTINCT p FROM Post p JOIN FETCH p.comments WHERE p.id IN (SELECT p2.id FROM Post p2)")
    fun findAllWithComments(pageable: Pageable): List<Post>
}
