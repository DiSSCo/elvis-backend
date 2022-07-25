package org.synthesis.infrastructure.cache

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class BadExpiringAsyncReadCache<K, V>(
    private val loadFn: suspend (K) -> V?,
    private val ttl: Duration = Duration.ofSeconds(3600),
    preSize: Int = 100
) {
    private val cache = ConcurrentHashMap<K, CachedValue<V>>(preSize)
    private val maxAge: Long
        get() = System.currentTimeMillis() + ttl.toMillis()

    suspend fun get(key: K): V? =
        cache.getOrPut(key, { CachedValue(loadFn(key), maxAge) })?.let {
            if (it.maxAge > System.currentTimeMillis()) {
                it.value
            } else {
                cache.put(key, CachedValue(loadFn(key), maxAge))?.value
            }
        }

    data class CachedValue<V>(val value: V?, val maxAge: Long)
}
