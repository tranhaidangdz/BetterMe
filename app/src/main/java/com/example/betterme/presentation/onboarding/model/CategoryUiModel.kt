package com.example.betterme.presentation.onboarding.model

data class CategoryUiModel(
    val id: Int,
    val name: String,
    val description: String,
    val icon: String, // emoji hoặc path
    val isSelected: Boolean = false
)