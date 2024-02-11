package com.example.services

fun randomEvent(): Triple<String, String, Sentiment> {

    // list of product names
    val productNames = listOf("Technology", "Food", "Real Estate", "Oil", "Raw Materials")

// list of random events that would affect the economy, given as a list of strings
    val randomEvents = listOf(
        "A new product has been released ",
        "A new competitor has entered the market",
        "A new law has been passed",
        "A new tax has been introduced",
        "A new technology has been developed",
        "A new regulation has been introduced",
        "A new trade deal has been signed",
        "A new war has started",
        "A new peace treaty has been signed")


    val randomEvent = randomEvents.random()
    val randomProduct = productNames.random()
    val sentiment = Sentiment.entries.toTypedArray().random()
    return Triple(randomEvent, randomProduct, sentiment)
}


enum class Sentiment {
    POSITIVE, NEGATIVE
}