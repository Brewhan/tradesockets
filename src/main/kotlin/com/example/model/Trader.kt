package com.example.model

import java.util.UUID

data class Trader (
    val traderId: UUID,
    val name: String,
    var cash: Double,
    var inventory: MutableList<Inventory> = mutableListOf(),
    var message: String
){
fun toJson(): String {
        return "{\"traderId\":\"$traderId\",\"name\":\"$name\",\"cash\":$cash,\"inventory\":${inventory.map { it.toJson() }},\"message\":\"$message\"}"
    }
    companion object {
        fun fromJson(json: String): Trader {
            val regex = Regex("[^0-9a-zA-Z]")
            val jsonList = json.split(regex)
            return Trader(UUID.fromString(jsonList[1]), jsonList[3], jsonList[5].toDouble(), mutableListOf(), jsonList[7])
        }
    }

}