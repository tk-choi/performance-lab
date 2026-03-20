package com.perflab.sqllab.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val userId: Long,

    val title: String,

    val content: String,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    val comments: MutableList<Comment> = mutableListOf()
)
