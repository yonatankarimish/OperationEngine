package com.SixSense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "sixsense.hosts")
public class HostConfig {
    private final Host local;
    private final RabbitHost rabbit;

    public HostConfig(Host local, RabbitHost rabbit) {
        this.local = local;
        this.rabbit = rabbit;
    }

    public static class Host{
        private final String host;
        private final String username;
        private final String password;
        private final int port;

        public Host(String host, String username, String password, int port) {
            this.host = host;
            this.username = username;
            this.password = password;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public int getPort() {
            return port;
        }
    }

    public static class RabbitHost extends Host{
        private final String vhost;

        public RabbitHost(String host, String username, String password, int port, String vhost) {
            super(host, username, password, port);
            this.vhost = vhost;
        }

        public String getVhost() {
            return vhost;
        }
    }

    public Host getLocal() {
        return local;
    }

    public RabbitHost getRabbit() {
        return rabbit;
    }
}
