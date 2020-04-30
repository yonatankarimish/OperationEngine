package com.sixsense.api.http.overrides;

import com.sixsense.threading.ThreadingManager;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class TomcatServerConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    private final ThreadingManager threadingManager;

    public TomcatServerConfig(ThreadingManager threadingManager) {
        this.threadingManager = threadingManager;
    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        this.threadingManager.injectServletWithPool((TomcatWebServer)factory.getWebServer());
        /*factory.addConnectorCustomizers(connector -> {

        });*/
    }
}
