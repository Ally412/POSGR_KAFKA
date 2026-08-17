package io.github.ally412.shelter.common.web;

public class NoSuchAdopterException extends RuntimeException {
    public NoSuchAdopterException(Long id) {
        super("Adopter with " + id + " not found.");
    }
}
