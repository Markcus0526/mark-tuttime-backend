package com.tuttime.common.exception;

//@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MethodArgumentNotValidException extends RuntimeException {

	public MethodArgumentNotValidException() {
		super();
	}

	public MethodArgumentNotValidException(String message) {
		super(message);
	}
}