package com.example.plugins

import com.example.model.*
import com.example.services.Sentiment
import com.example.services.randomEvent
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*

var houseUUID: UUID = UUID.randomUUID()

val product1 = Product("Technology", "Description 1", 100.0, 1000, houseUUID, OrderDirection.SELL,null)
val product2 = Product("Food", "Description 2", 100.0, 1000, houseUUID, OrderDirection.SELL, null)
val product3 = Product("Real Estate", "Description 2", 100.0, 1000, houseUUID, OrderDirection.SELL, null)
val product4 = Product("Oil", "Description 2", 100.0, 1000, houseUUID, OrderDirection.SELL, null)
val product5 = Product("Raw Materials", "Description 2", 100.0, 1000, houseUUID, OrderDirection.SELL, null)


var products = mutableListOf(product1, product2, product3, product4, product5)
val traders = mutableListOf<Trader>()
fun Application.configureSockets() {


    val tradersString = """
        ___________                  .___                   
\__    ___/___________     __| _/___________  ______
  |    |  \_  __ \__  \   / __ |/ __ \_  __ \/  ___/
  |    |   |  | \// __ \_/ /_/ \  ___/|  | \/\___ \ 
  |____|   |__|  (____  /\____ |\___  >__|  /____  >
                      \/      \/    \/           \/
        
    """



    //for loop to create a list of 5 traders --- TODO: move this to a config file
    traders.add(Trader(houseUUID, "House", 10000000000.0, mutableListOf(), "The House Always Wins."))
    for (i in 1..5) {
        val startingInventory = mutableListOf<Inventory>()
        for (product in products) {
            startingInventory.add(Inventory(product.name, 0))
        }
        traders.add(Trader(UUID.randomUUID(), "Trader $i", 100000.0, startingInventory, "Welcome to the market!"))

    }


    println(tradersString)
    // print traders list on new lines
    traders.forEach { println(it.toJson()) }
    println("Example Buy Order:")
    println(Product("Product 1", "Description 1", 100.0, 100, traders[0].traderId, OrderDirection.BUY, OrderType.MARKET).toJson())


    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/ws") { // websocketSession
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    outgoing.send(Frame.Text("YOU SAID: $text"))
                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                }
            }
        }
        webSocket("/randomEvent") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val randomEvent = randomEvent()
                    outgoing.send(Frame.Text(randomEvent.toString()))
                    if(randomEvent.third == Sentiment.NEGATIVE) {
                        products.forEach { changePrice(it, 0.9) }
                    } else {
                        products.forEach { changePrice(it, 1.1) }
                    }
                }
            }
        }

        webSocket("/traders"){
            println(traders.map { it.toJson() }.toString())
            outgoing.send(Frame.Text(traders.map { it.toJson() }.toString()))
        }
        // list all products including their buy and sell directions
        webSocket("/products"){
            outgoing.send(Frame.Text(products.map { it.toJson() }.toString()))
        }

        //here we are performing a market order, which is an order to buy a product at the best available price in the current market
        webSocket("/marketOrder") {
            //serialize the incoming to a BuyOrder object and print it out
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val marketOrder = Product.fromJson(frame.readText())
                    val buyer = placeOrder(marketOrder, traders, OrderType.MARKET)
                    outgoing.send(Frame.Text(buyer.toJson()))
                    executeOrders()
                }
            }
        }
        // we will have a buy which will buy at a specific price. or lower, but must execute immediately
        webSocket("/iocOrder") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val buyOrder = Product.fromJson(frame.readText())
                    val buyer = placeOrder(buyOrder, traders, OrderType.IOC)
                    outgoing.send(Frame.Text(buyer.toJson()))
                    executeOrders()
                }
            }
        }
    }

}

// probs don't need an object for market order, just use product

private fun placeOrder(
    productOrder: Product,
    traders: MutableList<Trader>,
    orderType: OrderType
): Trader {

    // check for the buyer in the traders list. throw an exception if the buyer is not found
    val trader = traders.find { it.traderId == productOrder.owner } ?: throw Exception("Trader not found")

    val product = products.find { it.name == productOrder.name }

    if (orderType == OrderType.MARKET) {
        productOrder.price = Double.MAX_VALUE
    }


    //if the product does not exist, set the message to "Product not found" and return the buyer
    if (product == null) {
        return trader.apply { message = "Product not found"}
    }

    // if the buyer does not have enough cash, set the message to "Not enough cash" and return the buyer
    if (trader.cash < product.price * productOrder.quantity) {
        return trader.apply { message = "Not enough cash"}
    }

    // add an order to the market for the product with the direction of BUY
    val order = Product(productOrder.name, "", product.price, productOrder.quantity, trader.traderId, OrderDirection.BUY, orderType)
    products.add(order)

    return trader.apply { message = "Order registered"}
}


// create a thread that will compare the buy and sell orders and execute the orders
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
                                    it.quantity += sellOrder.quantity
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

// function that after 10 seconds will increase or drop the price of a product given in a variable by a percentage
fun changePrice(product: Product, percentage: Double) {
    val thread = Thread {
        Thread.sleep(10000)
        val product = products.find { it.name == product.name }
        if (product != null) {
            product.price *= percentage
        }
    }
    thread.start()
}