package com.mrkongtk.rtspviewer.data

import kotlinx.serialization.Serializable

@Serializable
data class RTSPItem(
    val id: Long,
    val name: String,
    val uri: String,
    val tags: List<String>,
    val order: Int,
    val forceTcp: Boolean = false,
)
