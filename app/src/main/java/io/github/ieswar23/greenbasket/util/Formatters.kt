package io.github.ieswar23.greenbasket.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val indianLocale = Locale("en", "IN")
private val rupeeFormat: NumberFormat = NumberFormat.getIntegerInstance(indianLocale)

/** Formats whole rupees using Indian digit grouping, e.g. 125000 -> "₹1,25,000". */
fun Int.asRupees(): String = "₹" + rupeeFormat.format(this.toLong())

/** "−₹50" style formatting for discounts. */
fun Int.asDiscount(): String = "−" + asRupees()

object DateFormats {
    fun orderDate(millis: Long): String =
        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH).format(Date(millis))

    fun shortDate(millis: Long): String =
        SimpleDateFormat("d MMM, h:mm a", Locale.ENGLISH).format(Date(millis))
}
