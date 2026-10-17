package app.hideandseek.core

class JsonException(message: String) : RuntimeException(message)

/**
 * Small dependency-free JSON reader, so the core runs anywhere Kotlin runs
 * (Android, plain JVM tests, Kotlin Multiplatform for iOS) without extra libraries.
 *
 * Objects become `Map<String, Any?>`, arrays `List<Any?>`, numbers `Double`.
 * Trailing commas are tolerated because content files are edited by hand.
 */
object MiniJson {

    fun parse(text: String): Any? {
        val reader = Reader(text)
        reader.skipWhitespace()
        val value = reader.readValue()
        reader.skipWhitespace()
        if (!reader.atEnd) reader.fail("Unexpected trailing characters")
        return value
    }

    /** Escapes [value] as a JSON string literal, quotes included. */
    fun quote(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (c in value) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ' || c == ' ' || c == ' ') {
                    sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(c)
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private class Reader(private val s: String) {
        private var i = 0

        val atEnd: Boolean get() = i >= s.length

        fun fail(message: String): Nothing = throw JsonException("$message at position $i")

        private fun peek(): Char = if (i < s.length) s[i] else '\u0000'

        fun skipWhitespace() {
            while (i < s.length) {
                val c = s[i]
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '﻿') i++ else break
            }
        }

        fun readValue(): Any? {
            if (atEnd) fail("Unexpected end of input")
            val c = s[i]
            return when {
                c == '{' -> readObject()
                c == '[' -> readArray()
                c == '"' -> readString()
                c == 't' -> readLiteral("true", true)
                c == 'f' -> readLiteral("false", false)
                c == 'n' -> readLiteral("null", null)
                c == '-' || c in '0'..'9' -> readNumber()
                else -> fail("Unexpected character '$c'")
            }
        }

        private fun readLiteral(word: String, value: Any?): Any? {
            if (!s.startsWith(word, i)) fail("Expected '$word'")
            i += word.length
            return value
        }

        private fun readObject(): Map<String, Any?> {
            i++ // '{'
            val result = LinkedHashMap<String, Any?>()
            while (true) {
                skipWhitespace()
                if (peek() == '}') {
                    i++
                    return result
                }
                if (peek() != '"') fail("Expected a quoted key")
                val key = readString()
                skipWhitespace()
                if (peek() != ':') fail("Expected ':'")
                i++
                skipWhitespace()
                result[key] = readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> i++
                    '}' -> {
                        i++
                        return result
                    }
                    else -> fail("Expected ',' or '}'")
                }
            }
        }

        private fun readArray(): List<Any?> {
            i++ // '['
            val result = ArrayList<Any?>()
            while (true) {
                skipWhitespace()
                if (peek() == ']') {
                    i++
                    return result
                }
                result.add(readValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> i++
                    ']' -> {
                        i++
                        return result
                    }
                    else -> fail("Expected ',' or ']'")
                }
            }
        }

        private fun readString(): String {
            i++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (atEnd) fail("Unterminated string")
                val c = s[i++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (atEnd) fail("Unterminated escape")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) fail("Bad unicode escape")
                                val code = s.substring(i, i + 4).toIntOrNull(16) ?: fail("Bad unicode escape")
                                sb.append(code.toChar())
                                i += 4
                            }
                            else -> fail("Bad escape '\\$e'")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun readNumber(): Double {
            val start = i
            if (peek() == '-') i++
            while (peek() in '0'..'9') i++
            if (peek() == '.') {
                i++
                while (peek() in '0'..'9') i++
            }
            if (peek() == 'e' || peek() == 'E') {
                i++
                if (peek() == '+' || peek() == '-') i++
                while (peek() in '0'..'9') i++
            }
            return s.substring(start, i).toDoubleOrNull() ?: fail("Bad number")
        }
    }
}

// ---- Typed access helpers used by the content parser and the web bridge ----

@Suppress("UNCHECKED_CAST")
internal fun Any?.asObject(): Map<String, Any?>? = this as? Map<String, Any?>

internal fun Map<String, Any?>.string(key: String): String? =
    (this[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }

internal fun Map<String, Any?>.number(key: String): Double? = (this[key] as? Number)?.toDouble()

internal fun Map<String, Any?>.int(key: String): Int? = number(key)?.toInt()

internal fun Map<String, Any?>.boolean(key: String): Boolean? = this[key] as? Boolean

internal fun Map<String, Any?>.obj(key: String): Map<String, Any?>? = this[key].asObject()

internal fun Map<String, Any?>.array(key: String): List<Any?> = this[key] as? List<Any?> ?: emptyList()

internal fun Map<String, Any?>.objects(key: String): List<Map<String, Any?>> =
    array(key).mapNotNull { it.asObject() }
