package com.perflab.sqllab.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["com.perflab.sqllab.domain.repository"])
class JpaConfig
