package com.rreganjr.validator;

public class InvalidStateException extends RuntimeException {
    private final InvalidValue[] invalidValues;

    public InvalidStateException(String message, InvalidValue[] invalidValues) {
        super(message);
        this.invalidValues = invalidValues != null ? invalidValues : new InvalidValue[0];
    }

    public InvalidStateException(Throwable cause, InvalidValue[] invalidValues) {
        super(cause);
        this.invalidValues = invalidValues != null ? invalidValues : new InvalidValue[0];
    }

    public InvalidStateException(String message) { this(message, null); }
    public InvalidStateException(Throwable cause) { this(cause, null); }

    public InvalidValue[] getInvalidValues() { return invalidValues; }
}

