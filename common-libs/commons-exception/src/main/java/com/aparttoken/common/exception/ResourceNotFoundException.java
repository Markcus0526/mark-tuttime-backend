package com.aparttoken.common.exception;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message){
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id){
        super(String.format("%s with id %d not found", resource, id));
    }
}
