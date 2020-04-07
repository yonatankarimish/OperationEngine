package com.SixSense.api.http.overrides;

import com.SixSense.threading.ThreadingManager;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.stereotype.Component;

@Component
public class TomcatConnectorConfig implements TomcatConnectorCustomizer {
    private final ThreadingManager threadingManager;

    @Autowired
    public TomcatConnectorConfig(ThreadingManager threadingManager) {
        this.threadingManager = threadingManager;
    }

    @Override
    public void customize(Connector connector) {
        this.threadingManager.injectConnectorWithPool(connector);
    }
}