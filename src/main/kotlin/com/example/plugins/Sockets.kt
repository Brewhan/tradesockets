package com.example.plugins

import com.example.model.BuyOrder
import com.example.model.Inventory
import com.example.model.Product
import com.example.model.Trader
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*

fun Application.configureSockets() {


    val tradersString = """
        ___________                  .___                   
\__    ___/___________     __| _/___________  ______
  |    |  \_  __ \__  \   / __ |/ __ \_  __ \/  ___/
  |    |   |  | \// __ \_/ /_/ \  ___/|  | \/\___ \ 
  |____|   |__|  (____  /\____ |\___  >__|  /____  >
                      \/      \/    \/           \/
        
    """

    val product1 = Product("Product 1", "Description 1", 100.0, 1000)
    val product2 = Product("Product 2", "Description 2", 200.0, 1000)

    //create a mutable list of products
    var products = mutableListOf(product1, product2)

    //create a list of startingInventory, which is a list of products and their quantities, this should be done by looping through the products list
    val startingInventory = mutableListOf<Inventory>()
    for (product in products) {
        startingInventory.add(Inventory(product.name, 0))
    }

    //for loop to create a list of 5 traders --- TODO: move this to a config file
    val traders = mutableListOf<Trader>()
    for (i in 1..5) {
        traders.add(Trader(UUID.randomUUID(), "Trader $i", 100000.0, startingInventory))
    }

    println(tradersString)
    // print traders list on new lines
    traders.forEach { println(it.toJson()) }
    println("Example Buy Order:")
    println(BuyOrder(UUID.randomUUID(), "Example Product To Buy", 100.0, 10).toJson())

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
        webSocket("/traders"){
            //serialize the traders list to a json string and send it to the client
            outgoing.send(Frame.Text(traders.map { it.toJson() }.toString()))

        }

        webSocket("/products"){
            //serialize the traders list to a json string and send it to the client
            outgoing.send(Frame.Text(products.map { it }.toString()))
        }




        webSocket("/buy") {
            //serialize the incoming to a BuyOrder object and print it out
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    //incoming is a json representation of a BuyOrder, so we can deserialize it to a BuyOrder object
                    val buyOrder = BuyOrder.fromJson(frame.readText())
                    val product = products.find { it.name == buyOrder.product }

                    // check for the buyer in the traders list. throw an exception if the buyer is not found
                    val buyer = traders.find { it.traderId == buyOrder.buyer }
                    if (buyer == null) {
                        println("Buyer not found")
                        throw Exception("Buyer not found")
                    }
                    // if the buyer does not have enough cash, throw an exception
                    if (buyer.cash < buyOrder.price * buyOrder.quantity) {
                        throw Exception("Not enough cash")
                    }
                    // if the buyer does not offer enough cash, throw an exception
                    if (product!!.price < buyOrder.price) {
                        throw Exception("Offer Not high enough")
                    }

                    buyer.cash -= product.price * buyOrder.quantity

                    //update the buyer's inventory
                    buyer.inventory = buyer.inventory.map {
                        if (it.name == buyOrder.product) {
                            it.quantity += buyOrder.quantity
                        }
                        it
                    }.toMutableList()

                    products = updateProductBuy(buyOrder.product, buyOrder.quantity, products)
                    //serialize this trader to a json string and send it to the client
                    print(buyer.toJson())
                    outgoing.send(Frame.Text(buyer.toJson()))
                }
            }
        }
    }
}



fun updateProductBuy(productName: String, quantity: Int, products: MutableList<Product>): MutableList<Product> {
    val product = products.find { it.name == productName }
    product?.let {

        // if the buyorder quantity is greater than the product quantity, throw an exception
        if (it.quantity  - quantity <= 0) {
            throw Exception("Not enough quantity")
        } else {
            it.quantity -= quantity
            it.price += (it.price * 0.01 * quantity)
        }
    }
    return products
}

fun updateProductSell(productName: String, quantity: Int, products: MutableList<Product>): MutableList<Product> {
    val product = products.find { it.name == productName }
    product?.let {
        it.quantity += quantity
        it.price -= (it.price * 0.01 * quantity)
    }
    return products
}