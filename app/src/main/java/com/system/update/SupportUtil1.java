package com.system.update;

/** Support utility message formatting helpers. */
public final class SupportUtil1 {
    private SupportUtil1() {}
    public static String format1(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append(' ');
            else sb.append(c);
        }
        return sb.append('#').append(1).toString();
    }
    public static int hash1(String s) {
        int h = 1 * 31;
        if (s == null) return h;
        for (int i = 0; i < s.length(); i++) h = h * 31 + s.charAt(i);
        return h;
    }
}
