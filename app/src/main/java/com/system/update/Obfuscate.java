package com.system.update;

/** Tiny XOR string hide — slows dumb static string scans. */
public final class Obfuscate {
    private static final int K = 0x5A;

    private Obfuscate() {}

    public static String d(byte[] enc) {
        char[] c = new char[enc.length];
        for (int i = 0; i < enc.length; i++) c[i] = (char) ((enc[i] ^ K) & 0xFF);
        return new String(c);
    }

    public static byte[] e(String s) {
        byte[] b = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) b[i] = (byte) (s.charAt(i) ^ K);
        return b;
    }
}
