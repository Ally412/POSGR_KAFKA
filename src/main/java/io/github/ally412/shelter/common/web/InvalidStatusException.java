package io.github.ally412.shelter.common.web;

public class InvalidStatusException extends RuntimeException {
    public InvalidStatusException(String invalidStatus) {
        super("Unknown Status \"" + invalidStatus + "\"");
    }
}
