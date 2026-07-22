package io.github.ally412.shelter.common.web;

public class DuplicateCredException extends RuntimeException {
    public DuplicateCredException(String credName, String cred) {
        super(credName + " " + cred + " is already in use");
    }
}
