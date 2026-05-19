package com.tuttime.common.event.kafka;

public interface Timestamped<T> {
    T getTimestamp();

    void setTimestamp(T timestamp);
}