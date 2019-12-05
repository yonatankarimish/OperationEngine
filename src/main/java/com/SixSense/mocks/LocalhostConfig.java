package com.SixSense.mocks;

import java.util.concurrent.TimeUnit;

public class LocalhostConfig {
    public static final String host = "localhost";
    public static final String username = "root";
    public static final String password = "qwe123";
    public static final int port = 22;

    public static final long connectTimeout = TimeUnit.SECONDS.toMillis(15);
    public static final long keepAliveInterval = TimeUnit.SECONDS.toMillis(30);
    public static final long idleConnectionTimeOut = Long.MAX_VALUE;
}
