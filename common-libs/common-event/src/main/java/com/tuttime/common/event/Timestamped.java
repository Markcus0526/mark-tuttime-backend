package com.tuttime.common.event;

public interface Timestamped<T> {
	T getTimestamp();
	void setTimestamp(T timestamp);
}