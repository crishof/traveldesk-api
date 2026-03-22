package com.crishof.traveldeskapi.exception;

public class AgencyAlreadyExistException extends RuntimeException {

    public AgencyAlreadyExistException(String message) {
        super(message);
    }
}
