package com.mrkongtk.rstpviewer.model

import kotlinx.serialization.Serializable

@Serializable
data class RSTPItem(val id: Long, val name: String, val link: String, val tags: List<String>, val order: Int)
