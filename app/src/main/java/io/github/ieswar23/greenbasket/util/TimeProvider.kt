package io.github.ieswar23.greenbasket.util

/** Abstraction over the system clock so time-based logic stays testable. */
fun interface TimeProvider {
    fun now(): Long
}
