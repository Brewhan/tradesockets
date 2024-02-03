package com.example.model

class Inventory (
    name: String,
    quantity: Int
) {
    var name: String = name
    var quantity: Int = quantity
    fun toJson(): String {
        return "{\"name\":\"$name\",\"quantity\":$quantity}"
    }
    companion object {
        fun fromJson(json: String): Inventory {
            val regex = Regex("[^0-9a-zA-Z]")
            val jsonList = json.split(regex)
            return Inventory(jsonList[1], jsonList[3].toInt())
        }
    }
}