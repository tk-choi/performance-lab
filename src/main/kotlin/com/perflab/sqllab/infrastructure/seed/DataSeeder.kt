package com.perflab.sqllab.infrastructure.seed

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.random.Random

@Component
@Profile("seed")
class DataSeeder(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(DataSeeder::class.java)

    private val BATCH_SIZE = 5_000
    private val USER_COUNT = 100_000
    private val ORDER_COUNT = 300_000
    private val POST_COUNT = 100_000
    private val COMMENT_COUNT = 500_000
    private val LOG_COUNT = 1_000_000

    private val ACTIONS = listOf("LOGIN", "LOGOUT", "VIEW", "SEARCH", "UPDATE")
    private val STATUSES = listOf("SUCCESS", "FAIL", "PENDING")
    private val PRODUCTS = listOf(
        "노트북", "스마트폰", "태블릿", "모니터", "키보드",
        "마우스", "헤드폰", "스피커", "카메라", "프린터"
    )

    override fun run(args: ApplicationArguments) {
        val userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long::class.java) ?: 0L
        if (userCount > 0) {
            log.info("데이터가 이미 존재합니다 (users: {}건). 시딩을 건너뜁니다.", userCount)
            return
        }

        log.info("데이터 시딩을 시작합니다...")
        val startTime = System.currentTimeMillis()

        seedUsers()
        seedOrders()
        seedPosts()
        seedComments()
        seedLogs()

        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
        log.info("데이터 시딩 완료! 총 소요 시간: {}초", elapsed)
    }

    private fun seedUsers() {
        log.info("users 시딩 시작: {}건", USER_COUNT)
        val sql = "INSERT INTO users (name, email, created_at) VALUES (?, ?, ?)"

        var inserted = 0
        while (inserted < USER_COUNT) {
            val batchEnd = minOf(inserted + BATCH_SIZE, USER_COUNT)
            val batchArgs = (inserted + 1..batchEnd).map { i ->
                arrayOf<Any?>(
                    "user_$i",
                    "user_$i@test.com",
                    randomDateTime(2019, 2024)
                )
            }
            jdbcTemplate.batchUpdate(sql, batchArgs)
            inserted = batchEnd
            logProgress("users", inserted, USER_COUNT)
        }
        log.info("users 시딩 완료: {}건", inserted)
    }

    private fun seedOrders() {
        log.info("orders 시딩 시작: {}건", ORDER_COUNT)
        val sql = "INSERT INTO orders (user_id, product_name, amount, created_at) VALUES (?, ?, ?, ?)"

        var inserted = 0
        while (inserted < ORDER_COUNT) {
            val batchEnd = minOf(inserted + BATCH_SIZE, ORDER_COUNT)
            val batchArgs = (inserted + 1..batchEnd).map {
                arrayOf<Any?>(
                    Random.nextLong(1, USER_COUNT + 1L),
                    PRODUCTS[Random.nextInt(PRODUCTS.size)],
                    Random.nextInt(1_000, 500_001),
                    randomDateTime(2019, 2024)
                )
            }
            jdbcTemplate.batchUpdate(sql, batchArgs)
            inserted = batchEnd
            logProgress("orders", inserted, ORDER_COUNT)
        }
        log.info("orders 시딩 완료: {}건", inserted)
    }

    private fun seedPosts() {
        log.info("posts 시딩 시작: {}건", POST_COUNT)
        val sql = "INSERT INTO posts (user_id, title, content, created_at) VALUES (?, ?, ?, ?)"

        var inserted = 0
        while (inserted < POST_COUNT) {
            val batchEnd = minOf(inserted + BATCH_SIZE, POST_COUNT)
            val batchArgs = (inserted + 1..batchEnd).map { i ->
                val userId = Random.nextLong(1, USER_COUNT + 1L)
                arrayOf<Any?>(
                    userId,
                    "포스트 제목 $i",
                    "포스트 내용입니다. user_$userId 가 작성한 게시글 $i 입니다.",
                    randomDateTime(2019, 2024)
                )
            }
            jdbcTemplate.batchUpdate(sql, batchArgs)
            inserted = batchEnd
            logProgress("posts", inserted, POST_COUNT)
        }
        log.info("posts 시딩 완료: {}건", inserted)
    }

    private fun seedComments() {
        log.info("comments 시딩 시작: {}건", COMMENT_COUNT)
        val sql = "INSERT INTO comments (post_id, user_id, content, created_at) VALUES (?, ?, ?, ?)"

        var inserted = 0
        while (inserted < COMMENT_COUNT) {
            val batchEnd = minOf(inserted + BATCH_SIZE, COMMENT_COUNT)
            val batchArgs = (inserted + 1..batchEnd).map { i ->
                arrayOf<Any?>(
                    Random.nextLong(1, POST_COUNT + 1L),
                    Random.nextLong(1, USER_COUNT + 1L),
                    "댓글 내용 $i 입니다.",
                    randomDateTime(2019, 2024)
                )
            }
            jdbcTemplate.batchUpdate(sql, batchArgs)
            inserted = batchEnd
            logProgress("comments", inserted, COMMENT_COUNT)
        }
        log.info("comments 시딩 완료: {}건", inserted)
    }

    private fun seedLogs() {
        log.info("logs 시딩 시작: {}건", LOG_COUNT)
        val sql = "INSERT INTO logs (user_id, action, status, created_at) VALUES (?, ?, ?, ?)"

        var inserted = 0
        while (inserted < LOG_COUNT) {
            val batchEnd = minOf(inserted + BATCH_SIZE, LOG_COUNT)
            val batchArgs = (inserted + 1..batchEnd).map {
                arrayOf<Any?>(
                    Random.nextLong(1, USER_COUNT + 1L),
                    ACTIONS[Random.nextInt(ACTIONS.size)],
                    STATUSES[Random.nextInt(STATUSES.size)],
                    randomDateTime(2019, 2024)
                )
            }
            jdbcTemplate.batchUpdate(sql, batchArgs)
            inserted = batchEnd
            logProgress("logs", inserted, LOG_COUNT)
        }
        log.info("logs 시딩 완료: {}건", inserted)
    }

    private fun randomDateTime(fromYear: Int, toYear: Int): LocalDateTime {
        val year = Random.nextInt(fromYear, toYear + 1)
        val month = Random.nextInt(1, 13)
        val day = Random.nextInt(1, 29)
        val hour = Random.nextInt(0, 24)
        val minute = Random.nextInt(0, 60)
        val second = Random.nextInt(0, 60)
        return LocalDateTime.of(year, month, day, hour, minute, second)
    }

    private fun logProgress(table: String, inserted: Int, total: Int) {
        val percent = inserted * 100 / total
        val prevPercent = (inserted - BATCH_SIZE).coerceAtLeast(0) * 100 / total
        if (percent / 10 > prevPercent / 10 || inserted == total) {
            log.info("{} 진행: {}/{} ({}%)", table, inserted, total, percent)
        }
    }
}
