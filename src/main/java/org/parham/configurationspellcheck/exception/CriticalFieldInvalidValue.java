package org.parham.configurationspellcheck.exception;

/**
 * @author Parham Ahmady
 * @since 3/4/2022
 */
public class CriticalFieldInvalidValue extends RuntimeException {
    public CriticalFieldInvalidValue(String message) {
        super(message);
    }

    public CriticalFieldInvalidValue(String message, Throwable cause) {
        super(message, cause);
    }
}
