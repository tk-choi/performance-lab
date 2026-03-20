package com.perflab.sqllab.domain.repository

import com.perflab.sqllab.domain.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByNameContaining(name: String): List<User>
    fun findByName(name: String): List<User>
}
