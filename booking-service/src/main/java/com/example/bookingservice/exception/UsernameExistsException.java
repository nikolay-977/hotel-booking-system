package com.example.bookingservice.exception;

public class UsernameExistsException extends RuntimeException {
    public UsernameExistsException() {
        super("Username already exists");
    }
}