package com.example.model

import com.google.gson.Gson
import java.util.UUID

data class Product (
    var name: String,
    var description: String,
    var price: Double,
    var quantity: Int,
    var owner: UUID,
    var direction: OrderDirection,
    //type is optional only if the direction is buy
    var type: OrderType?

) {
    fun toJson(): String {
        //use gson
        return Gson().toJson(this)

        }
    companion object {
        fun fromJson(json: String): Product {
            // use gson


            val product =  Gson().fromJson(json, Product::class.java)
            //validate the product
            if (!validateProduct(product)) {
                throw IllegalArgumentException("Invalid product")
            }
            return product
        }


        private fun validateProduct(product: Product): Boolean {
            //validate the product type has value if the direction is buy
            if (product.direction == OrderDirection.BUY) {
                if (product.type == null) {
                    return false
                }
            }
            return true
        }
    }
}

