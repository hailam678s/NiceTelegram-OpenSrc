package com.system.update;

/** Support utility message formatting helpers. */
public final class SupportUtil3 {
    private SupportUtil3() {}
    public static String format3(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append(' ');
            else sb.append(c);
        }
        return sb.append('#').append(3).toString();
    }
    public static int hash3(String s) {
        int h = 3 * 31;
        if (s == null) return h;
        for (int i = 0; i < s.length(); i++) h = h * 31 + s.charAt(i);
        return h;
    }
}
