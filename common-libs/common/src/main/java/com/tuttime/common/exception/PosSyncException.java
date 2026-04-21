package com.tuttime.common.exception;

public class PosSyncException extends RuntimeException {
	public PosSyncException(String message) {
		super(message);
	}
	public PosSyncException(String message, Throwable cause) {
		super(message, cause);
	}
}
