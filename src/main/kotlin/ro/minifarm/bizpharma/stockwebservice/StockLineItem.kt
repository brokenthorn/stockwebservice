package ro.minifarm.bizpharma.stockwebservice

import java.math.BigDecimal
import java.util.*

data class StockLineItem(
        val location_id: Int,
        val article_id: Int,
        val acquisition_date: Date?,
        val best_before_date: Date?,
        val lot: String?,
        val net_acquisition_price: BigDecimal,
        val gross_acquisition_price: BigDecimal,
        val added_value_percent: BigDecimal,
        val retail_price: BigDecimal,
        val vat_percent: BigDecimal,
        val fractions: Int,
        val whole_quantity: Int,
        val fraction_quantity: Int
)