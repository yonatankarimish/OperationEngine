package com.SixSense.api.http.overrides;

import com.SixSense.threading.ThreadingManager;
import org.apache.catalina.connector.Connector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TomcatConnectorConfig implements TomcatConnectorCustomizer {
    private static final Logger logger = LogManager.getLogger(TomcatConnectorConfig.class);
    public static List<Connector> connectors = new ArrayList<>();
    private final ThreadingManager threadingManager;

    @Autowired
    public TomcatConnectorConfig(ThreadingManager threadingManager) {
        this.threadingManager = threadingManager;
    }

    @Override
    public void customize(Connector connector) {
        this.threadingManager.injectConnectorWithPool(connector);
        connectors.add(connector);
    }
}