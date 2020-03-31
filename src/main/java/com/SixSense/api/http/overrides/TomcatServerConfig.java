package com.SixSense.api.http.overrides;

import com.SixSense.threading.ThreadingManager;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class TomcatServerConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    public static TomcatWebServer embeddedServer;
    private final ThreadingManager threadingManager;

    public TomcatServerConfig(ThreadingManager threadingManager) {
        this.threadingManager = threadingManager;
    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        embeddedServer = (TomcatWebServer)factory.getWebServer();
        this.threadingManager.injectServletWithPool(embeddedServer);

        /*factory.addConnectorCustomizers(connector -> {

        });*/
    }
}
