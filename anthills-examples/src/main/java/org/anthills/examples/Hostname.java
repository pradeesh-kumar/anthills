package org.anthills.examples;

import java.net.InetAddress;

public final class Hostname {

    private Hostname() {}

    public static String current() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }
}
