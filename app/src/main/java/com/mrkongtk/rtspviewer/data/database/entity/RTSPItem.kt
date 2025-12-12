package com.mrkongtk.rtspviewer.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "rtsp_item")
data class RTSPItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val uri: String,
    val tags: List<String>,
    val order: Int,
    @ColumnInfo(name = "force_tcp")
    val forceTcp: Boolean = false,
)

data class RTSPItemOrderUpdate(
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "order")
    val order: Int,
)
