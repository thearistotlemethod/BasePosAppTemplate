package com.payment.app.utils

import com.payment.app.utils.Converter.swap

object EmvUtil {
    @JvmStatic
    fun getPreNameFromAID(emvAid: String): String {
        return if(emvAid.startsWith("A000000003")) "VISA"
        else if(emvAid.startsWith("A000000004")) "MASTERCARD"
        else "UNKNOWN"
    }

    @JvmStatic
    fun getAppNameFromAID(aid: String): String {
        return when(aid){
            "A0000000031010" -> "VISA CREDIT"
            "A0000000032010" -> "VISA ELECTRON"
            "A0000000041010" -> "MCHIP"
            "A0000000043060" -> "MAESTRO"
            "A0000000046000" -> "CIRRUS"
            "A00000002501" -> "AMEX"
            "A0000006723010" -> "TROY CREDIT"
            "A0000006723020" -> "TROY DEBIT"
            else -> ""
        }
    }

    @JvmStatic
    fun isAidPaywave(aid: String): Boolean {
        return with(aid){
            when{
                contains("A000000003") -> true
                else -> false
            }
        }
    }

    @JvmStatic
    fun isAidPaypass(aid: String): Boolean {
        return with(aid){
            when{
                contains("A000000004") -> true
                else -> false
            }
        }
    }

    @JvmStatic
    fun isAidTROY(aid: String): Boolean {
        return with(aid){
            when{
                contains("A0000006723010") -> true
                contains("A0000006723020") -> true
                else -> false
            }
        }
    }
}