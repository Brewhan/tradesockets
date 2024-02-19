package com.example.services

import com.example.model.Inventory
import com.example.model.Product
import com.example.plugins.products

fun generateStarterInventory(products: MutableList<Product>): MutableList<Inventory> {
    val startingInventory = mutableListOf<Inventory>()
    for (product in products) {
        startingInventory.add(Inventory(product.name, 0))
    }
    return startingInventory
}


fun changePrice(product: Product, percentage: Double) {
    val thread = Thread {
        Thread.sleep(10000)
        val productMatch = products.find { it.name == product.name }
        if (productMatch != null) {
            productMatch.price *= percentage
        }
    }
    thread.start()
}