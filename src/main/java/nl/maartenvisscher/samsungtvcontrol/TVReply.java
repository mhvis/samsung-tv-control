package nl.maartenvisscher.samsungtvcontrol;

/**
 * TV response after authentication.
 *
 * @author Maarten Visscher
 */
public enum TVReply {

    /**
     * Authenticated, TV will respond to key codes.
     */
    ALLOWED,
    /**
     * We are not allowed to send key codes (TV user denied this controller).
     */
    DENIED,
    /**
     * Control request timed out or was canceled by the TV user.
     */
    TIMEOUT;
}
