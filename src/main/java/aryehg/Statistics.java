package aryehg;

import com.google.gson.JsonObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {
    private static final AtomicInteger DEFAULT_COUNT = new AtomicInteger(0);

    private ConcurrentMap<String, AtomicInteger> eventCountsByType;
    private ConcurrentMap<String, AtomicInteger> wordOccurrences;

    Statistics() {
        eventCountsByType = new ConcurrentHashMap<>();
        wordOccurrences = new ConcurrentHashMap<>();
    }

    void updateForEvent(JsonObject event) {
        updateCount(eventCountsByType, event.get("event_type").getAsString());
        updateCount(wordOccurrences, event.get("data").getAsString());
    }

    private void updateCount(ConcurrentMap<String, AtomicInteger> counts, String countKey) {
        counts.putIfAbsent(countKey, new AtomicInteger(0));
        counts.get(countKey).incrementAndGet();
    }

    public int getEventCount(String eventType) {
        return eventCountsByType.getOrDefault(eventType, DEFAULT_COUNT).get();
    }

    public int getWordOccurences(String word) {
        return wordOccurrences.getOrDefault(word, DEFAULT_COUNT).get();
    }
}
