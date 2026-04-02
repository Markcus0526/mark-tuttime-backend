package com.tuttime.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

	public ConflictException() {
		super();
	}

	public ConflictException(String message) {
		super(message);
	}
}
