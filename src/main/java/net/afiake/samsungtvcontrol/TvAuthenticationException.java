package net.afiake.samsungtvcontrol;

/**
 * Thrown when the television denied access, we are not allowed to control the television. This happens when the television user denies access
 * or on timeout or cancel.
 */
public class TvAuthenticationException extends Exception {
    public TvAuthenticationException(String message) {
        super(message);
    }
}