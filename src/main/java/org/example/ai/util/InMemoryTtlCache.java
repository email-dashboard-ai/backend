package org.example.ai.util;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryTtlCache<K, V> {
  private final int maxEntries;
  private final long ttlMillis;
  private final Clock clock;

  private final Map<K, Entry<V>> map;

  public InMemoryTtlCache(int maxEntries, int ttlSeconds) {
    this(maxEntries, ttlSeconds, Clock.systemUTC());
  }

  InMemoryTtlCache(int maxEntries, int ttlSeconds, Clock clock) {
    this.maxEntries = Math.max(1, maxEntries);
    this.ttlMillis = Math.max(1, ttlSeconds) * 1000L;
    this.clock = clock;
    this.map =
        new LinkedHashMap<>(16, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<K, Entry<V>> eldest) {
            return size() > InMemoryTtlCache.this.maxEntries;
          }
        };
  }

  public synchronized Optional<V> get(K key) {
    Entry<V> entry = map.get(key);
    if (entry == null) {
      return Optional.empty();
    }
    if (isExpired(entry.createdAtMillis)) {
      map.remove(key);
      return Optional.empty();
    }
    return Optional.of(entry.value);
  }

  public synchronized void put(K key, V value) {
    map.put(key, new Entry<>(value, clock.millis()));
  }

  private boolean isExpired(long createdAtMillis) {
    return clock.millis() - createdAtMillis > ttlMillis;
  }

  private record Entry<V>(V value, long createdAtMillis) {}
}
