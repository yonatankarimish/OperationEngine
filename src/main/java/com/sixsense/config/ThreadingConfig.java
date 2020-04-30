package com.sixsense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConstructorBinding
@ConfigurationProperties(prefix = "sixsense.threads")
public class ThreadingConfig {
    private final ThreadingProperties engine;
    private final ThreadingProperties http;
    private final AMQPThreadingProperties amqp;

    public ThreadingConfig(ThreadingProperties engine, ThreadingProperties http, AMQPThreadingProperties amqp) {
        this.engine = engine;
        this.http = http;
        this.amqp = amqp;
    }

    public static class ThreadingProperties {
        @DurationUnit(ChronoUnit.SECONDS)
        private final Duration allowedIdleTime;
        private final int maximumThreads;
        private final int minimumThreads;
        private final String threadNamePrefix;

        public ThreadingProperties(Duration allowedIdleTime, int maximumThreads, int minimumThreads, String threadNamePrefix) {
            this.allowedIdleTime = allowedIdleTime;
            this.maximumThreads = maximumThreads;
            this.minimumThreads = minimumThreads;
            this.threadNamePrefix = threadNamePrefix;
        }

        public Duration getAllowedIdleTime() {
            return allowedIdleTime;
        }

        public int getMaximumThreads() {
            return maximumThreads;
        }

        public int getMinimumThreads() {
            return minimumThreads;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }
    }

    public static class AMQPThreadingProperties extends ThreadingProperties{
        private final int minimumChannels;
        private final int minimumConnections;

        public AMQPThreadingProperties(Duration allowedIdleTime, int maximumThreads, int minimumThreads, String threadNamePrefix, int minimumChannels, int minimumConnections) {
            super(allowedIdleTime, maximumThreads, minimumThreads, threadNamePrefix);
            this.minimumChannels = minimumChannels;
            this.minimumConnections = minimumConnections;
        }

        public int getMinimumChannels() {
            return minimumChannels;
        }

        public int getMinimumConnections() {
            return minimumConnections;
        }
    }

    public ThreadingProperties getEngine() {
        return engine;
    }

    public ThreadingProperties getHttp() {
        return http;
    }

    public AMQPThreadingProperties getAmqp() {
        return amqp;
    }
}
