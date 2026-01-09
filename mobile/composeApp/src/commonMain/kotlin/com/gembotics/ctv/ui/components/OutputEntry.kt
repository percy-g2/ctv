package com.gembotics.ctv.ui.components

data class OutputEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    var address: String = "",
    var amount: String = "",
    var data: String = ""
) {
    fun toOutputString(): String {
        return "$address:$amount:${data.ifEmpty { "" }}"
    }
    
    companion object {
        fun fromString(line: String): OutputEntry? {
            val parts = line.split(":")
            if (parts.size < 2) return null
            return OutputEntry(
                address = parts[0].trim(),
                amount = parts[1].trim(),
                data = if (parts.size > 2) parts[2].trim() else ""
            )
        }
    }
}
