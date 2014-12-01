package net.afiake.samsungtvcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;

/**
 * API for controlling Samsung Smart TVs using a socket connection on port 55000.
 */
public class SmartRemote {

    // Protocol infomation has been gathered from: http://sc0ty.pl/2012/02/samsung-tv-network-remote-control-protocol/
    // TODO: device discovery using http://www.lewisbenge.net/2012/11/13/device-discovery-ssdp-in-windows-8-and-winrt/

    private static final int PORT = 55000;

    private final String controllerId; // Unique ID which is used at Samsung TV internally to distinguish controllers
    private final String controllerName; // Name for this controller, which is displayed on the television
    private final Socket socket;
    private final OutputStream out;
    private final InputStream in;

    /**
     * Opens a socket connection to given host (a Samsung Smart TV) and tries to authenticate with the television.
     *
     * @param controllerId a unique ID which is used at the Samsung TV internally to distinguish controllers
     * @param controllerName the name for this controller, which is displayed on the television
     * @param host the ip-address to connect to
     * @throws IOException there was a problem with the socket connection
     * @throws AuthenticationException the television user denied our control request
     */
    public SmartRemote(String controllerId, String controllerName, String host) throws IOException, AuthenticationException {
        this.controllerId = controllerId;
        this.controllerName = controllerName;
        this.socket = new Socket(host, PORT);
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
        authenticate();
    }

    /**
     * Tries to authenticate with the television, has to be run every time when a new socket connection has been made, prior to sending key codes.
     *
     * @throws IOException there was a problem with the socket connection
     * @throws AuthenticationException the television user denied our control request
     */
    private void authenticate() throws IOException, AuthenticationException {
        String stringText = "iphone.iapp.samsung";

        byte[] string; // String byte array
        byte[] payload_ip;
        byte[] payload_id;
        byte[] payload_name;
        int payload_size;
        int size;
        ByteBuffer outBuf;
        byte[] res;
        short len;
        String resString;

        string = stringText.getBytes(); // Gathering all byte arrays
        payload_ip = Base64.encodeBase64(socket.getLocalAddress().getAddress());
        payload_id = Base64.encodeBase64(controllerId.getBytes());
        payload_name = Base64.encodeBase64(controllerName.getBytes());

        payload_size = 2 + 2 + payload_ip.length + 2 + payload_id.length + 2 + payload_name.length; // Getting sizes
        size = 1 + 2 + string.length + 2 + payload_size;
        outBuf = ByteBuffer.allocateDirect(size); // Allocate buffer using size
        outBuf.order(ByteOrder.LITTLE_ENDIAN); // Little-endian order for the bytes
        outBuf.put((byte) 0);
        outBuf.putShort((short) string.length);
        outBuf.put(string);
        outBuf.putShort((short) payload_size);
        outBuf.put((byte) 100);
        outBuf.put((byte) 0);
        outBuf.putShort((short) payload_ip.length);
        outBuf.put(payload_ip);
        outBuf.putShort((short) payload_id.length);
        outBuf.put(payload_id);
        outBuf.putShort((short) payload_name.length);
        outBuf.put(payload_name);

        outBuf.rewind();
        res = new byte[outBuf.remaining()]; // Put buffer in 'res' byte array
        outBuf.get(res);
        out.write(res); // Write byte array to socket

        // Reader
        do {
            res = new byte[1]; // Change to skip or put in ArrayList or something else
            in.read(res);
            res = new byte[2];
            in.read(res);
            len = ByteBuffer.wrap(res).order(ByteOrder.LITTLE_ENDIAN).getShort();
            res = new byte[len];
            in.read(res);
            //resString = new String(res);
            res = new byte[2];
            in.read(res);
            len = ByteBuffer.wrap(res).order(ByteOrder.LITTLE_ENDIAN).getShort();
            res = new byte[len];
            in.read(res);
        } while (res[0] == 10);
        if (res[0] == 101) {
            throw new AuthenticationException("Authentication timeout or cancelled by user.");
        }
        if (res[0] == 100 && res[2] == 0) {
            throw new AuthenticationException("Access denied! User rejected this controller.");
        }
    }

    /**
     * Sends a key code over current socket connection. Only works when you are successfully authenticated.
     *
     * @param keyCode the key code to send
     * @throws IOException there was a problem with the socket connection
     */
    public void keyCode(KeyCode keyCode) throws IOException {
        keyCode(keyCode.name());
    }

    /**
     * Sends a key code over current socket connection. Only works when you are successfully authenticated.
     *
     * @param keyCode the key code to send
     * @throws IOException there was a problem with the socket connection
     */
    public void keyCode(String keyCode) throws IOException {
        String stringText = "iphone.iapp.samsung";

        byte[] string; // String byte array
        byte[] payload;
        int payload_size;
        int size;
        ByteBuffer outBuf;
        byte[] res;
        short len;
        String resString;

        string = stringText.getBytes(); // Gathering all byte arrays
        payload = Base64.encodeBase64(keyCode.getBytes());

        payload_size = 3 + 2 + payload.length; // Getting sizes
        size = 1 + 2 + string.length + 2 + payload_size;
        outBuf = ByteBuffer.allocateDirect(size); // Allocate buffer using size
        outBuf.order(ByteOrder.LITTLE_ENDIAN); // Little-endian order for the bytes
        outBuf.put((byte) 0);
        outBuf.putShort((short) string.length);
        outBuf.put(string);
        outBuf.putShort((short) payload_size);
        outBuf.put((byte) 0);
        outBuf.put((byte) 0);
        outBuf.put((byte) 0);
        outBuf.putShort((short) payload.length);
        outBuf.put(payload);

        outBuf.rewind();
        res = new byte[outBuf.remaining()]; // Put buffer in 'res' byte array
        outBuf.get(res);
        out.write(res); // Write byte array to socket

        // Reader
        /*
        res = new byte[1];
        in.read(res);
        res = new byte[2];
        in.read(res);
        len = ByteBuffer.wrap(res).order(ByteOrder.LITTLE_ENDIAN).getShort();
        res = new byte[len];
        in.read(res);
        //resString = new String(res);
        res = new byte[2];
        in.read(res);
        len = ByteBuffer.wrap(res).order(ByteOrder.LITTLE_ENDIAN).getShort();
        res = new byte[len];
        in.read(res);
        */
    }
}
