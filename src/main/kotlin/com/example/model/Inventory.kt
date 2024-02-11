package com.example.model

import com.google.gson.Gson

data class Inventory (
    var name: String,
    var quantity: Int
) {
    fun toJson(): String {
        // use gson
        return Gson().toJson(this)
    }
    companion object {
        fun fromJson(json: String): Inventory {
            return Gson().fromJson(json, Inventory::class.java)
        }
    }
}