package com.SixSense.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.Map;

@ConstructorBinding
@ConfigurationProperties(prefix = "sixsense.session")
public class SessionConfig {
    private final Map<String, String> prompt;
    private final String version;

    public SessionConfig(Map<String, String> prompt, String version) {
        this.prompt = prompt;
        this.version = version;
    }

    public Map<String, String> getPrompt() {
        return prompt;
    }

    public String getVersion() {
        return version;
    }
}
