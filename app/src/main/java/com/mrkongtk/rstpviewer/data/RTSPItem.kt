package com.mrkongtk.rstpviewer.data

import kotlinx.serialization.Serializable

@Serializable
data class RTSPItem(
    val id: Long,
    val name: String,
    val url: String,
    val tags: List<String>,
    val order: Int
)
