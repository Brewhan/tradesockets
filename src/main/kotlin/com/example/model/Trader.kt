package com.example.model

import com.google.gson.Gson
import java.util.UUID

data class Trader (
    val traderId: UUID,
    val name: String,
    var cash: Double,
    var inventory: MutableList<Inventory> = mutableListOf(),
    var message: String
){
fun toJson(): String {
        return Gson().toJson(this)

    }
    companion object {
        fun fromJson(json: String): Trader {
            return Gson().fromJson(json, Trader::class.java)
        }
    }

}