package org.jcnc.snow.vm.runtime;

/**
 * SnowPanicException indicates a structural runtime invariant violation.
 * It is intended to fail fast when non-specifiable host objects attempt to
 * cross Snow's runtime value boundary.
 */
public final class SnowPanicException extends RuntimeException {
    public SnowPanicException(String message) {
        super(message);
    }

    public SnowPanicException(String message, Throwable cause) {
        super(message, cause);
    }
}