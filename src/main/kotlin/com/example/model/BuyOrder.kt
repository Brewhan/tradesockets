package com.example.model;

import com.google.gson.Gson
import java.util.*

data class BuyOrder(
    val buyer: UUID,
    val product: String,
    val price: Double,
    val quantity: Int,
) {
    fun toJson(): String {
        return "{\"buyer\":\"$buyer\",\"product\":\"$product\",\"price\":$price,\"quantity\":$quantity}"
    }
    // fromJson method that takes in a json string and returns a BuyOrder object
    companion object {
        fun fromJson(json: String): BuyOrder {
            //convert string to json
            return Gson().fromJson(json, BuyOrder::class.java)
        }
    }
}


// create a fromJson method that takes in a json string and returns a BuyOrder object

