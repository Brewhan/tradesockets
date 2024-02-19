package com.example.model

import com.google.gson.Gson
import java.util.*

data class Report(
    val buyer: String,
    val seller: String,
    val price: Double,
    val quantity: Int,
) {
    fun toJson(): String {
        //use gson
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): Report {
            // use gson
            val report = Gson().fromJson(json, Report::class.java)
            //validate the report
            if (!validateReport(report)) {
                throw IllegalArgumentException("Invalid report")
            }
            return report
        }

        private fun validateReport(report: Report): Boolean {
            //validate the report
            return !(report.price < 0 || report.quantity < 0)
        }
    }
}
