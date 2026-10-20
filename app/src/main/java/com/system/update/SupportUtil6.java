package com.system.update;

/** Support utility message formatting helpers. */
public final class SupportUtil6 {
    private SupportUtil6() {}
    public static String format6(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append(' ');
            else sb.append(c);
        }
        return sb.append('#').append(6).toString();
    }
    public static int hash6(String s) {
        int h = 6 * 31;
        if (s == null) return h;
        for (int i = 0; i < s.length(); i++) h = h * 31 + s.charAt(i);
        return h;
    }
}
