package com.raistlin.myvelobike.util

object JSONUtils {

    fun unescape(input: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < input.length) {
            val delimiter = input[i]
            i++ // consume letter or backslash
            if (delimiter == '\\' && i < input.length) {

                // consume first after backslash
                val ch = input[i]
                i++
                if (ch == '\\' || ch == '/' || ch == '"' || ch == '\'') {
                    builder.append(ch)
                } else if (ch == 'n') builder.append('\n') else if (ch == 'r') builder.append('\r') else if (ch == 't') builder.append('\t') else if (ch == 'b') builder.append('\b') else if (ch == 'u') {
                    val hex = StringBuilder()

                    // expect 4 digits
                    if (i + 4 > input.length) {
                        throw RuntimeException("Not enough unicode digits! ")
                    }
                    for (x in input.substring(i, i + 4).toCharArray()) {
                        if (!Character.isLetterOrDigit(x)) {
                            throw RuntimeException("Bad character in unicode escape.")
                        }
                        hex.append(Character.toLowerCase(x))
                    }
                    i += 4 // consume those four digits.
                    val code = hex.toString().toInt(16)
                    builder.append(code.toChar())
                } else {
                    throw RuntimeException("Illegal escape sequence: \\$ch")
                }
            } else { // it's not a backslash, or it's the last character.
                builder.append(delimiter)
            }
        }
        return builder.toString()
    }
}