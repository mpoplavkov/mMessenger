package edu.technopolis.homework.messenger;

import edu.technopolis.homework.messenger.net.BitProtocol;

import java.nio.ByteBuffer;

public class Utils {
    public static byte[] concat(byte[]... arrays) {
        int bytesCount = 0;
        for (byte[] array : arrays) {
            bytesCount += array.length;
        }
        byte[] res = new byte[bytesCount];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, res, pos, array.length);
            pos += array.length;
        }
        return res;
    }

    public static byte[] getBytes(long l) {
        return ByteBuffer.allocate(Long.BYTES).putLong(l).array();
    }

    public static byte[] getBytes(int i) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(i).array();
    }

    public static byte[] getBytes(boolean b) {
        byte[] res = new byte[1];
        res[0] = (byte) (b ? 1 : 0);
        return res;
    }

    public static int cipherPassword(String password) {
        return password.hashCode();
    }
}
