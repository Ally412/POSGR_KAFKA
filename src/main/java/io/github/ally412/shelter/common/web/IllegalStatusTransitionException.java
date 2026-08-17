package io.github.ally412.shelter.common.web;

public class IllegalStatusTransitionException extends RuntimeException {
    public IllegalStatusTransitionException(String message) {
        super(message);
    }
}
