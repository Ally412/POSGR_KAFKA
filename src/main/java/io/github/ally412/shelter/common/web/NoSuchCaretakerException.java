package io.github.ally412.shelter.common.web;

public class NoSuchCaretakerException extends RuntimeException {
    public NoSuchCaretakerException(Long id) {
        super("No caretaker with id " + id);
    }
}
