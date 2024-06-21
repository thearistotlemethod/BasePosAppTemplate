package com.payment.app.utils

class Tuple<A : Any, B : Any>(val a: A, val b: B) {
    override fun equals(other: Any?): Boolean {
        if (other !is Tuple<*, *>) {
            return false
        }
        val t = other
        return equalsEx(t.a, a) && equalsEx(t.b, b)
    }

    override fun hashCode(): Int {
        var result = 17
        result = result * 31 + hashCodeEx(a)
        result = result * 31 + hashCodeEx(b)
        return result
    }

    fun equalsEx(a: Any?, b: Any): Boolean {
        return a === b || a != null && a == b
    }

    fun hashCodeEx(o: Any?): Int {
        return o?.hashCode() ?: 0
    }
}