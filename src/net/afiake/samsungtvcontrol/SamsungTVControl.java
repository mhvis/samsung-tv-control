package net.afiake.samsungtvcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;

/**
 * API for controlling Samsung TVs using a socket connection on port 55000.
 */
public class SamsungTVControl {
    // See: http://sc0ty.pl/2012/02/samsung-tv-network-remote-control-protocol/
    private static final int PORT = 55000;
    private static final String CONTROLLER_ID = "pebble";
    private static final String CONTROLLER_NAME = "Pebble";

    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private Base64.Encoder encoder;

    /**
     * Opens a socket connection to given host (a Samsung Smart TV).
     * @param host the ip-address to connect to
     */
    public SamsungTVControl(String host) throws IOException {

        socket = new Socket(host, PORT);
        out = socket.getOutputStream();
        in = socket.getInputStream();
        encoder = Base64.getEncoder();
    }


    public void authenticate() throws Exception {

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
        payload_ip = encoder.encode(socket.getLocalAddress().getAddress());
        payload_id = encoder.encode(CONTROLLER_ID.getBytes());
        payload_name = encoder.encode(CONTROLLER_NAME.getBytes());

        payload_size = 2+2+payload_ip.length+2+payload_id.length+2+payload_name.length; // Getting sizes
        size = 1+2+string.length+2+payload_size;
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
            System.out.println(Integer.toHexString(in.read()));
            res = new byte[2];
            in.read(res);
            len = ByteBuffer.wrap(res).order(ByteOrder.LITTLE_ENDIAN).getShort();
            System.out.println(len);
            res = new byte[len];
            in.read(res);
            resString = new String(res);
            System.out.println(resString);
            res = new byte[2];
            in.read(res);
            len = ByteBuffer.wrap(res).order(ByteOrder.LITTLE_ENDIAN).getShort();
            res = new byte[len];
            in.read(res);
            System.out.println(Arrays.toString(res));
        } while (res[0] == 10);
        if (res[0] == 101) {
            throw new Exception("Authentication timeout or cancelled by user.");
        }
        if (res[0] == 100 && res[2] == 0) {
            throw new Exception("Access denied! User rejected this controller.");
        }
        System.out.println("Access granted!");
    }


    public void keyCode(String keyCode) throws Exception {
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
        payload = encoder.encode(keyCode.getBytes());

        payload_size = 3+2+payload.length; // Getting sizes
        size = 1+2+string.length+2+payload_size;
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
        System.out.println(Integer.toHexString(in.read()));
        res = new byte[2];
        in.read(res);
        len = ByteBuffer.wrap(res).order(ByteOrder.LITTLE_ENDIAN).getShort();
        System.out.println(len);
        res = new byte[len];
        in.read(res);
        resString = new String(res);
        System.out.println(resString);
        res = new byte[2];
        in.read(res);
        len = ByteBuffer.wrap(res).order(ByteOrder.LITTLE_ENDIAN).getShort();
        res = new byte[len];
        in.read(res);
        System.out.println(Arrays.toString(res));
    }
}