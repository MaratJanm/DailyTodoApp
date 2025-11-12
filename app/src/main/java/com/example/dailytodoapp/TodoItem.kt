package com.example.dailytodoapp

data class TodoItem(
    val id: Int,
    var title: String,
    var isCompleted: Boolean = false,
    val isProgressive: Boolean = false,
    val baseValue: Int? = null,
    val increment: Int? = null,
    val numberPosition: Int? = null
)