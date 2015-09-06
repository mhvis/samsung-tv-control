package nl.maartenvisscher.samsungtvcontrol;

import java.io.IOException;
import java.net.Socket;

import org.apache.commons.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * API for controlling Samsung Smart TVs using a socket connection on port 55000. The protocol information has been gathered from
 * http://sc0ty.pl/2012/02/samsung-tv-network-remote-control-protocol/ .
 *
 * @author Maarten Visscher
 */
public class SamsungRemote {

    private final int PORT = 55000;
    private final int SO_TIMEOUT = 3 * 1000; // Socket connect and read timeout in milliseconds.
    private final int SO_AUTHENTICATE_TIMEOUT = 300 * 1000; // Socket read timeout while authenticating (waiting for user response) in milliseconds.
    private final String APP_STRING = "iphone.iapp.samsung";

    private final char[] ALLOWED = {0x64, 0x00, 0x01, 0x00}; // TV return payload.
    private final char[] DENIED = {0x64, 0x00, 0x00, 0x00};
    private final char[] TIMEOUT = {0x65, 0x00};
//    private final char[] WAIT = {0x0a, 0x00, 0x02, 0x00, 0x00, 0x00}; // Sent when a window popups on TV I think?
//    private final char[] SKIP = {0x0a, 0x00, 0x01, 0x00, 0x00, 0x00}; // Don't know yet what this means, seems like keep-alive, I skip them.

    private final Socket socket;
    private final BufferedWriter out;
    private final BufferedReader in;
    private final boolean debug;
    private final ArrayList<String> log; // A very simple log which will be filled when debug==true and can be obtained from outside using getLog().

    /**
     * Opens a socket connection to the television.
     *
     * @param host the host name.
     * @throws IOException if an I/O error occurs when creating the socket.
     */
    public SamsungRemote(String host) throws IOException {
        this(host, false);
    }

    /**
     * Opens a socket connection to the television and keeps a simple log when debug is true.
     *
     * @param host the host name.
     * @param debug whether or not to keep a log.
     * @throws IOException if an I/O error occurs when creating the socket.
     */
    public SamsungRemote(String host, boolean debug) throws IOException {
        this.debug = debug;
        this.log = new ArrayList<>();
        this.socket = new Socket();
        socket.connect(new InetSocketAddress(host, PORT), SO_TIMEOUT);
        socket.setSoTimeout(SO_TIMEOUT);
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Authenticates with the television using host IP address for the ip and id parameters.
     *
     * @param name the name for this controller, which is displayed on the television.
     * @return the response from the television.
     * @throws IOException if an I/O error occurs.
     * @see SamsungRemote#authenticate(java.lang.String, java.lang.String, java.lang.String) authenticate
     */
    public TVReply authenticate(String name) throws IOException {
        String hostAddress = socket.getLocalAddress().getHostAddress();

        return authenticate(hostAddress, hostAddress, name);
    }

    /**
     * Authenticates with the television using host IP address for the ip parameter.
     *
     * @param id a parameter for the television.
     * @param name the name for this controller, which is displayed on the television.
     * @return the response from the television.
     * @throws IOException if an I/O error occurs.
     * @see SamsungRemote#authenticate(java.lang.String, java.lang.String, java.lang.String) authenticate
     */
    public TVReply authenticate(String id, String name) throws IOException {
        String hostAddress = socket.getLocalAddress().getHostAddress();

        return authenticate(hostAddress, id, name);
    }

    /**
     * Authenticates with the television. Has to be done every time when a new socket connection has been made, prior to sending key codes. Blocks
     * while waiting for the television response.
     *
     * @param ip a parameter for the television.
     * @param id a parameter for the television.
     * @param name the name for this controller, which is displayed on the television.
     * @return the response from the television.
     * @throws IOException if an I/O error occurs.
     */
    public TVReply authenticate(String ip, String id, String name) throws IOException {
        emptyReaderBuffer(in);

        log("Authenticating with ip: " + ip + ", id: " + id + ", name: " + name + ".");
        out.write(0x00);
        writeString(out, APP_STRING);
        writeString(out, getAuthenticationPayload(ip, id, name));
        out.flush(); // Send authentication.

        socket.setSoTimeout(SO_AUTHENTICATE_TIMEOUT);
        char[] payload = readRelevantMessage(in);
        socket.setSoTimeout(SO_TIMEOUT);

        if (Arrays.equals(payload, ALLOWED)) {
            log("Authentication response: access granted.");
            return TVReply.ALLOWED; // Access granted.
        } else if (Arrays.equals(payload, DENIED)) {
            log("Authentication response: access denied.");
            return TVReply.DENIED; // Access denied.
        } else if (Arrays.equals(payload, TIMEOUT)) {
            log("Authentication response: timeout.");
            return TVReply.TIMEOUT; // Timeout.
        }
        log("Authentication message is unknown.");
        throw new IOException("Got unknown response.");
    }

    /**
     * Sends a key code to TV, blocks shortly waiting for TV response to check delivery. Only works when you are successfully authenticated.
     *
     * @param keycode the key code to send.
     * @throws IOException if an I/O error occurs.
     */
    public void keycode(Keycode keycode) throws IOException {
        keycode(keycode.name());
    }

    /**
     * Sends a key code to TV, blocks shortly waiting for TV response to check delivery. Only works when you are successfully authenticated.
     *
     * @param keycode the key code to send.
     * @throws IOException if an I/O error occurs.
     */
    public void keycode(String keycode) throws IOException {
        emptyReaderBuffer(in);

        log("Sending keycode: " + keycode + ".");
        out.write(0x00);
        writeString(out, APP_STRING);
        writeString(out, getKeycodePayload(keycode));
        out.flush(); // Send key code.

        readMessage(in);
    }

    /**
     * Sends a key code to TV in a non-blocking manner, thus it does not check the delivery (use checkConnection() to poll the TV status). Only works
     * when you are successfully authenticated.
     *
     * @param keycode the key code to send.
     * @throws IOException if an I/O error occurs.
     */
    public void keycodeAsync(Keycode keycode) throws IOException {
        keycodeAsync(keycode.name());
    }

    /**
     * Sends a key code to TV in a non-blocking manner, thus it does not check the delivery (use checkConnection() to poll the TV status). Only works
     * when you are successfully authenticated.
     *
     * @param keycode the key code to send.
     * @throws IOException if an I/O error occurs.
     */
    public void keycodeAsync(String keycode) throws IOException {
        log("Sending keycode without reading: " + keycode + ".");
        out.write(0x00);
        writeString(out, APP_STRING);
        writeString(out, getKeycodePayload(keycode));
        out.flush(); // Send key code.
    }

    /**
     * Checks the connection by sending an empty key code, does not return anything but instead throws an exception when a problem arose (for instance
     * the TV turned off).
     *
     * @throws IOException if an I/O error occurs.
     */
    public void checkConnection() throws IOException {
        keycode("PING");
    }

    /**
     * Returns the authentication payload.
     *
     * @param ip the ip of the controller.
     * @param id the id of the controller.
     * @param name the name of the controller.
     * @return the authentication payload.
     * @throws IOException if an I/O error occurs.
     */
    private String getAuthenticationPayload(String ip, String id, String name) throws IOException {
        StringWriter writer = new StringWriter();
        writer.write(0x64);
        writer.write(0x00);
        writeBase64(writer, ip);
        writeBase64(writer, id);
        writeBase64(writer, name);
        writer.flush();
        return writer.toString();
    }

    /**
     * Returns the key code payload.
     *
     * @param keycode the key code.
     * @return the key code payload.
     * @throws IOException if an I/O error occurs.
     */
    private String getKeycodePayload(String keycode) throws IOException {
        StringWriter writer = new StringWriter();
        writer.write(0x00);
        writer.write(0x00);
        writer.write(0x00);
        writeBase64(writer, keycode);
        writer.flush();
        return writer.toString();
    }

    /**
     * Reads an incoming message or waits for a new one when it is not relevant. I believe non-relevant messages has to do with showing or hiding of
     * windows on the TV, and start with 0x0a. This method returns the payload of the relevant message.
     *
     * @param reader the reader.
     * @return the payload which was sent with the relevant message.
     */
    private char[] readRelevantMessage(Reader reader) throws IOException {
        char[] payload = readMessage(reader);
        while (payload[0] == 0x0a) {
            log("Message is not relevant, waiting for new message.");
            payload = readMessage(reader);
        }
        return payload;
    }

    /**
     * Reads an incoming message from the television and returns the payload.
     *
     * @param reader the reader.
     * @return the payload which was sent with the message.
     */
    private char[] readMessage(Reader reader) throws IOException {
        int first = reader.read();
        if (first == -1) {
            throw new IOException("End of stream has been reached (TV could have powered off).");
        }
        String response = readString(reader);
        char[] payload = readCharArray(reader);
        log("Message: first byte: " + Integer.toHexString(first) + ", response: " + response + ", payload: " + readable(payload));
        return payload;
    }

    /**
     * Returns a human readable string in hexadecimal of the char array.
     *
     * @param charArray the characters to translate.
     * @return the human readable string.
     */
    private String readable(char[] charArray) {
        String readable = Integer.toHexString(charArray[0]);
        for (int i = 1; i < charArray.length; i++) {
            readable += " " + Integer.toHexString(charArray[i]);
        }
        return readable;
    }

    /**
     * Writes the string length and the string itself to the writer.
     *
     * @param writer the writer.
     * @param string the string to write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeString(Writer writer, String string) throws IOException {
        writer.write(string.length());
        writer.write(0x00);
        writer.write(string);
    }

    /**
     * Encodes the string with base64 and writes the result length and the result itself to the writer.
     *
     * @param writer the writer.
     * @param string the string to encode using base64 and write.
     * @throws IOException if an I/O error occurs.
     */
    private void writeBase64(Writer writer, String string) throws IOException {
        String base64 = new String(Base64.encodeBase64(string.getBytes()));
        writeString(writer, base64);
    }

    /**
     * Reads the next string from the reader.
     *
     * @param reader the reader.
     * @return the string which is read.
     * @throws IOException if an I/O error occurs.
     */
    private String readString(Reader reader) throws IOException {
        return new String(readCharArray(reader));
    }

    /**
     * Reads the next characters from the reader using the length given in the first byte.
     *
     * @param reader the reader.
     * @return the characters which were read.
     * @throws IOException if an I/O error occurs.
     */
    private char[] readCharArray(Reader reader) throws IOException {
        int length = reader.read();
        reader.read();
        char[] charArray = new char[length];
        reader.read(charArray);
        return charArray;
    }

    /**
     * Reads all messages which are left in the buffer and therefore empties it.
     *
     * @param reader the reader.
     * @throws IOException if an I/O error occurs.
     */
    private void emptyReaderBuffer(Reader reader) throws IOException {
        log("Emptying reader buffer.");
        while (reader.ready()) {
            readMessage(reader);
        }
    }

    /**
     * Returns a simple log with for instance TV response payloads as string array, will only be filled when this class is constructed with debug true
     * (otherwise the array will be empty).
     *
     * @return a simple log.
     */
    public String[] getLog() {
        return log.toArray(new String[log.size()]);
    }

    /**
     * Logs a message when debug is true.
     *
     * @param message the message to log.
     */
    private void log(String message) {
        if (debug) {
            String time = (System.currentTimeMillis() % 1000) + ""; // Time is current milliseconds between 0 and 1000.
            while (time.length() < 3) {
                time = " " + time;
            }
            log.add(time + ". " + message);
        }
    }

    /**
     * Closes the socket connection. Should always be called at the end of a session.
     */
    public void close() {
        log("Closing socket connection.");
        try {
            socket.close();
        } catch (IOException e) {
            log("IOException when closing connection: " + e.getMessage());
        }
    }
}
