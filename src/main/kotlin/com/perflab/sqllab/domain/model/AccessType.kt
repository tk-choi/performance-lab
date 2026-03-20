package com.perflab.sqllab.domain.model

enum class AccessType {
    ALL, INDEX, RANGE, REF, EQ_REF, CONST, SYSTEM, NULL_TYPE;

    companion object {
        fun from(value: String): AccessType =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: ALL
    }
}
