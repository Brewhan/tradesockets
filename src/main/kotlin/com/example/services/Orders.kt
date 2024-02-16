package com.example.services

import com.example.model.OrderDirection
import com.example.model.OrderType
import com.example.model.Product
import com.example.model.Trader
import com.example.plugins.productNames
import com.example.plugins.products
import com.example.plugins.traders

fun executeOrders() {
    // create a thread that will compare the buy and sell orders and execute the orders
    val thread = Thread {
        while (true) {
            // get all the buy orders
            val buyOrders = products.filter { it.direction == OrderDirection.BUY }
            // get all the sell orders
            val sellOrders = products.filter { it.direction == OrderDirection.SELL }

            // for each buy order, check if there is a matching sell order
            for (buyOrder in buyOrders) {
                for (sellOrder in sellOrders) {
                    // print buy and sell orders
                    if (buyOrder.name == sellOrder.name) {

                        // if the buy order price is greater than or equal to the sell order price, execute the order
                        if (buyOrder.price >= sellOrder.price) {
                            // get the buyer and seller
                            val buyer =
                                traders.find { it.traderId == buyOrder.owner } ?: throw Exception("Buyer not found")
                            val seller =
                                traders.find { it.traderId == sellOrder.owner } ?: throw Exception("Seller not found")

                            if (buyer.traderId == seller.traderId) {
                                buyer.message = "You cannot buy from yourself"
                                continue
                            }

                            // if the buyer does not have enough cash, set the message to "Not enough cash" and return the buyer
                            if (buyer.cash < sellOrder.price * sellOrder.quantity) {
                                buyer.message = "Not enough cash"
                                if(buyOrder.type == OrderType.IOC) {
                                    return@Thread
                                }
                                continue
                            }

                            // if there is not enough quantity of the product in the sell order to fulfill the buy order, set the message to "Not enough quantity of product available" and return the buyer
                            if (sellOrder.quantity < buyOrder.quantity) {
                                seller.message = "Not enough quantity of product available"
                                if (buyOrder.type == OrderType.IOC) {
                                    return@Thread
                                }
                                continue
                            }

                            // update the buyer's cash
                            buyer.cash -= sellOrder.price * sellOrder.quantity

                            // update the seller's cash
                            seller.cash += sellOrder.price * sellOrder.quantity

                            // update the buyer's inventory
                            buyer.inventory = buyer.inventory.map {
                                if (it.name == sellOrder.name) {
                                    it.quantity += buyOrder.quantity
                                }
                                it
                            }.toMutableList()

                            //update the sell quantity of the sell order
                            sellOrder.quantity -= buyOrder.quantity

                            //if the sell order quantity is now 0, remove the sell order from the list
                            if (sellOrder.quantity == 0) {
                                products.remove(sellOrder)
                            }

                            //remove the buy order from the list
                            products.remove(buyOrder)

                            println("Product ${sellOrder.name} sold for ${sellOrder.price} to ${buyer.name} from ${seller.name}")
                            //complete the thread
                            return@Thread

                        }
                    }
                }
            }
        }
    }
    thread.start()
}



fun placeOrder(
    productOrder: Product,
    traders: MutableList<Trader>,
): Trader {

    // check for the buyer in the traders list. throw an exception if the buyer is not found
    val trader = traders.find { it.traderId == productOrder.owner } ?: throw Exception("Trader not found")

    val product = products.find { it.name == productOrder.name }

    if (productOrder.type == OrderType.MARKET) {
        productOrder.price = Double.MAX_VALUE
    }

    if (productOrder.direction == OrderDirection.BUY) {
        productOrder.price = Double.MAX_VALUE

        //if the product does not exist, set the message to "Product not found" and return the buyer
        if (product == null) {
            return trader.apply { message = "Product not found" }
        }


        // if the buyer does not have enough cash, set the message to "Not enough cash" and return the buyer
        if (trader.cash < product.price * productOrder.quantity) {
            return trader.apply { message = "Not enough cash" }
        }


        // add an order to the market for the product with the direction of BUY
        val order = Product(
            productOrder.name,
            product.description,
            product.price,
            productOrder.quantity,
            trader.traderId,
            OrderDirection.BUY,
            productOrder.type
        )
        products.add(order)
    } else {
        // if productorder name is not in the list of product names, set the message to "Product not found" and return the buyer
        if (!productNames.contains(productOrder.name)) {
            return trader.apply { message = "Product not found" }
        }

        // if the buyer does not have enough quantity of the product, set the message to "Not enough quantity of product available" and return the buyer
        if ((trader.inventory.find { it.name == productOrder.name }?.quantity ?: 0) < productOrder.quantity) {
            return trader.apply { message = "Not enough quantity of product available" }
        }

        // add an order to the market for the product with the direction of SELL
        val order = Product(
            productOrder.name,
            productOrder.description,
            productOrder.price,
            productOrder.quantity,
            trader.traderId,
            OrderDirection.SELL,
            productOrder.type
        )
        products.add(order)
    }

    return trader.apply { message = "Order registered"}
}