package com.example.plugins

import com.example.model.*
import com.example.services.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*


//read starting_products.toml file and create a list of products


var houseUUID: UUID = UUID.randomUUID()


var products= ReadConfig().products(houseUUID)
val traders = mutableListOf<Trader>()

//maintain a list of productNames
val productNames = products.map { it.name }

fun Application.configureSockets() {


    val tradersString = """
___________                  .___       _________              __           __          
\__    ___/___________     __| _/____  /   _____/ ____   ____ |  | __ _____/  |_  ______
  |    |  \_  __ \__  \   / __ |/ __ \ \_____  \ /  _ \_/ ___\|  |/ // __ \   __\/  ___/
  |    |   |  | \// __ \_/ /_/ \  ___/ /        (  <_> )  \___|    <\  ___/|  |  \___ \ 
  |____|   |__|  (____  /\____ |\___  >_______  /\____/ \___  >__|_ \\___  >__| /____  >
                      \/      \/    \/        \/            \/     \/    \/          \/ 
        
    """


    //for loop to create a list of 5 traders --- TODO: move this to a config file
    traders.add(Trader(houseUUID, "House", ReadConfig().houseCash, generateStarterInventory(products), "The House Always Wins."))
    for (i in 1..ReadConfig().numTraders) {
        val startingInventory = generateStarterInventory(products)
        traders.add(Trader(UUID.randomUUID(), "Trader $i", ReadConfig().startingCash, startingInventory, "Welcome to the market!"))
    }

    println(tradersString)
    // print traders list on new lines
    traders.forEach { println(it.toJson()) }
    println("Example Buy Order:")
    println(Product(products[0].name, products[0].description, 100.0, 100, traders[0].traderId, OrderDirection.BUY, OrderType.MARKET).toJson())
    println("NOTE: Example Order uses house UUID as the example trader. Consider using POSTMAN to send the example order to the broker using /order endpoint.")
    println("Order types are: ${OrderType.entries}")
    println("Order directions are: ${OrderDirection.entries}")


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

        //TODO: Add a join room endpoint, this will allow traders to join without us having to manually add them

        //websocket that will send the current price of all products every 5 seconds
        webSocket("/products") {
            while (true) {
                //hide the uuid before sending back products
                val products = products.map { it.toJson() }.toString().replace(Regex("owner\":\"[a-zA-Z0-9-]*\""),
                    "owner\":\"\"")
                outgoing.send(Frame.Text(products))
                Thread.sleep(5000)
            }
        }

        webSocket("/randomEvent") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val randomEvent = randomEvent(products)
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
            // hide the uuid before sending back traders
            val traders = traders.map { it.toJson() }.toString().replace(Regex("traderId\":\"[a-zA-Z0-9-]*\""), "traderId\":\"\"")
            outgoing.send(Frame.Text(traders))
        }
        // list all products including their buy and sell directions
        webSocket("/productsNow"){
            //hide the uuid before sending back products
            val products = products.map { it.toJson() }.toString().replace(Regex("owner\":\"[a-zA-Z0-9-]*\""),
                "owner\":\"\"")
            outgoing.send(Frame.Text(products))
        }

        //here we are performing a market order, which is an order to buy a product at the best available price in the current market
        webSocket("/order") {
            //serialize the incoming to a BuyOrder object and print it out
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val marketOrder = Product.fromJson(frame.readText())
                    val buyer = placeOrder(marketOrder, traders)
                    outgoing.send(Frame.Text(buyer.toJson()))
                    executeOrders()
                }
            }
        }
    }

}