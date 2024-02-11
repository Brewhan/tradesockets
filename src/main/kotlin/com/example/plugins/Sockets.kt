package com.example.plugins

import com.example.model.MarketOrder
import com.example.model.Inventory
import com.example.model.Product
import com.example.model.Trader
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*

val product1 = Product("Product 1", "Description 1", 100.0, 1000)
val product2 = Product("Product 2", "Description 2", 200.0, 1000)

var products = mutableListOf(product1, product2)
fun Application.configureSockets() {


    val tradersString = """
        ___________                  .___                   
\__    ___/___________     __| _/___________  ______
  |    |  \_  __ \__  \   / __ |/ __ \_  __ \/  ___/
  |    |   |  | \// __ \_/ /_/ \  ___/|  | \/\___ \ 
  |____|   |__|  (____  /\____ |\___  >__|  /____  >
                      \/      \/    \/           \/
        
    """



    //create a list of startingInventory, which is a list of products and their quantities, this should be done by looping through the products list
    val startingInventory = mutableListOf<Inventory>()
    for (product in products) {
        startingInventory.add(Inventory(product.name, 0))
    }

    //for loop to create a list of 5 traders --- TODO: move this to a config file
    val traders = mutableListOf<Trader>()
    for (i in 1..5) {
        traders.add(Trader(UUID.randomUUID(), "Trader $i", 100000.0, startingInventory, "Welcome to the market!"))
    }

    println(tradersString)
    // print traders list on new lines
    traders.forEach { println(it.toJson()) }
    println("Example Buy Order:")
    println(MarketOrder(UUID.randomUUID(), "Example Product To Buy", 10).toJson())

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


        webSocket("/marketOrder") {
            //serialize the incoming to a BuyOrder object and print it out
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    //incoming is a json representation of a BuyOrder, so we can deserialize it to a BuyOrder object
                    val marketOrder = MarketOrder.fromJson(frame.readText())
                    val buyer = marketOrder(marketOrder, traders)
                    outgoing.send(Frame.Text(buyer.toJson()))
                }
            }
        }

        // we will have a buy which will buy at a specific price.
        // If quantity is greater than one, it will buy until the quantity is reached or the price is reached
        webSocket("/buyOrder") {
            //serialize the incoming to a BuyOrder object and print it out
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    //incoming is a json representation of a BuyOrder, so we can deserialize it to a BuyOrder object
                    val buyOrder = MarketOrder.fromJson(frame.readText())
                    val buyer = marketOrder(buyOrder, traders)
                    outgoing.send(Frame.Text(buyer.toJson()))
                }
            }
        }


    }

}

private fun marketOrder(
    marketOrder: MarketOrder,
    traders: MutableList<Trader>
): Trader {

    // check for the buyer in the traders list. throw an exception if the buyer is not found
    val buyer = traders.find { it.traderId == marketOrder.buyer } ?: throw Exception("Trader not found")

    val product = products.find { it.name == marketOrder.product }

    //if the product does not exist, set the message to "Product not found" and return the buyer
    if (product == null) {
        return buyer.apply { message = "Product not found"}
    }

    // if the buyer does not have enough cash, set the message to "Not enough cash" and return the buyer
    if (buyer.cash < product.price * marketOrder.quantity) {
        return buyer.apply { message = "Not enough cash"}
    }

    buyer.cash -= product.price * marketOrder.quantity

    //update the buyer's inventory
    buyer.inventory = buyer.inventory.map {
        if (it.name == marketOrder.product) {
            it.quantity += marketOrder.quantity
        }
        it
    }.toMutableList()

    updateProductBuy(marketOrder.product, marketOrder.quantity)

    //if the message is set to "Not enough quantity of product available", return the buyer
    if (buyer.message == "Not enough quantity of product available") {
        return buyer
    }

    //serialize this trader to a json string and send it to the client
    print(buyer.toJson())
    //assuming everything is ok, set the message to "Order successful"
    return buyer
}


fun updateProductBuy(productName: String, quantity: Int): String {
    val product = products.find { it.name == productName }
    product?.let {

        // if the buyorder quantity is greater than the product quantity, throw an exception
        if (it.quantity  - quantity <= 0) {
            return "Not enough quantity of product available"
        } else {
            it.quantity -= quantity
            it.price += (it.price * 0.01 * quantity)
        }
    }
    return "Order successful"
}




fun updateProductSell(productName: String, quantity: Int, products: MutableList<Product>): MutableList<Product> {
    val product = products.find { it.name == productName }
    product?.let {
        it.quantity += quantity
        it.price -= (it.price * 0.01 * quantity)
    }
    return products
}