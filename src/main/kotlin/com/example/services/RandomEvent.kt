package com.example.services

import com.example.plugins.products

fun randomEvent(): Triple<String, String, Sentiment> {

    val productNames = ReadConfig().products().map { it.name }

    val randomEvents = listOf(
        "A new product has been released ",
        "A new competitor has entered the market",
        "A new law has been passed",
        "A new tax has been introduced",
        "A new technology has been developed",
        "A new regulation has been introduced",
        "A new trade deal has been signed",
        "A new war has started",
        "A new peace treaty has been signed",
        "Subprime mortgages are on the rise")


    val randomEvent = randomEvents.random()
    val randomProduct = productNames.random()

    //time for some not so random events

    if (randomEvent == "A new war has started"){
       return Triple(randomEvent, "Oil", Sentiment.POSITIVE)
    } else if (randomEvent == "Subprime mortgages are on the rise") {
        return Triple(randomEvent, "Real Estate", Sentiment.NEGATIVE)
    }

    val sentiment = Sentiment.entries.toTypedArray().random()
    return Triple(randomEvent, randomProduct, sentiment)
}

enum class Sentiment {
    POSITIVE, NEGATIVE
}