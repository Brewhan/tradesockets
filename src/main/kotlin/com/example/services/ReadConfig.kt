package com.example.services

import com.example.model.OrderDirection
import com.example.model.Product
import com.moandjiezana.toml.Toml
import java.io.File
import java.util.*

class ReadConfig {
    private val toml: Toml = Toml().read(File("src/main/resources/config.toml"))
    fun products(houseUUID: UUID): MutableList<Product> {
        //read the toml file
        //create a list of products

        val productTable = toml.getTables("products")
        val pList = mutableListOf<Product>()

        productTable.forEach { pt ->
            val p = Product(
                name=pt.getString("name"),
                description=pt.getString("description"),
                price=pt.getDouble("price"),
                quantity=pt.getLong("quantity").toInt(),
                owner= houseUUID,
                direction= OrderDirection.valueOf(pt.getString("direction")),
                type=null,
            )
            pList.add(p)
        }
        return pList;
    }
    val numTraders: Int = toml.getLong("traders.numTraders").toInt()
    val startingCash: Double = toml.getDouble("traders.startingCash")
    val houseCash: Double = toml.getDouble("traders.houseCash")


}