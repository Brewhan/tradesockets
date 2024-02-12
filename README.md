
#TradeSockets! 

##Websockets based ultra-basic trading app. Built for education.

Built using Ktor for Kotlin.

Example Buy Order:
{"name":"Product 1","description":"Description 1","price":100.0,"quantity":100,"owner":"4a74f039-4976-4290-a9f9-0b16e5308b7d","direction":"BUY","type":"MARKET"}
NOTE: Example Order uses house UUID as the example trader. Consider using POSTMAN to send the example order to the broker using /order endpoint.
Order types are: [MARKET, LIMIT, BID, ASK, IOC]
Order directions are: [BUY, SELL]

Please see config.toml to modify & add products
