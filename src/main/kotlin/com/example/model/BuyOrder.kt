package com.example.model;

import com.google.gson.Gson
import java.util.*


//this represents a market buy order
//a market buy order is an order to buy a product at the best available price in the current market
data class BuyOrder(
    val buyer: UUID,
    val product: String,    //name of the product
    val quantity: Int,
) {
    fun toJson(): String {
        return "{\"buyer\":\"$buyer\",\"product\":\"$product\",\"quantity\":$quantity}"
    }
    companion object {
        fun fromJson(json: String): BuyOrder {
            //convert string to json
            return Gson().fromJson(json, BuyOrder::class.java)
        }
    }
}


// create a fromJson method that takes in a json string and returns a BuyOrder object

