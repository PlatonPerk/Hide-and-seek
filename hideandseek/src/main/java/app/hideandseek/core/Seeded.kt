package app.hideandseek.core

/**
 * Deterministic "randomness": the same seed string gives the same pick on every
 * device and platform. That is what makes every kid see the creature in the same
 * place on the same day without any server.
 *
 * Algorithm (re-implement exactly this on iOS):
 *   hash = FNV-1a 32-bit over the UTF-8 bytes of the seed,
 *          then the MurmurHash3 fmix32 finalizer;
 *   weighted pick = walk the list, subtracting weights from (hash mod totalWeight).
 */
object Seeded {

    fun hash(seed: String): UInt {
        var h = 0x811C9DC5u
        for (b in seed.encodeToByteArray()) {
            h = h xor (b.toUInt() and 0xFFu)
            h *= 0x01000193u
        }
        h = h xor (h shr 16)
        h *= 0x85EBCA6Bu
        h = h xor (h shr 13)
        h *= 0xC2B2AE35u
        h = h xor (h shr 16)
        return h
    }

    fun <T> pickWeighted(items: List<T>, seed: String, weight: (T) -> Int): T? {
        var total = 0L
        for (item in items) total += weight(item).coerceAtLeast(0)
        if (total <= 0L) return null
        var r = hash(seed).toLong() % total
        for (item in items) {
            val w = weight(item).coerceAtLeast(0)
            if (r < w) return item
            r -= w
        }
        return null
    }

    fun <T> pickUniform(items: List<T>, seed: String): T? =
        if (items.isEmpty()) null else items[(hash(seed) % items.size.toUInt()).toInt()]
}
