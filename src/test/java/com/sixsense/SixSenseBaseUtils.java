package com.sixsense;

import com.sixsense.services.DiagnosticManager;
import com.sixsense.services.SessionEngine;
import com.sixsense.threading.ThreadingManager;
import com.sixsense.utillity.OperatingSystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.io.Closeable;

public class SixSenseBaseUtils {
    private static final Logger logger = LogManager.getLogger(SixSenseBaseUtils.class);
    private static ConfigurableApplicationContext appContext;

    protected static SessionEngine sessionEngine;
    protected static DiagnosticManager diagnosticManager;
    protected static ThreadingManager threadingManager;

    @BeforeSuite(alwaysRun = true)
    public void initSpring() {
        appContext = SpringApplication.run(Main.class);

        sessionEngine = (SessionEngine)appContext.getBean("sessionEngine");
        diagnosticManager = (DiagnosticManager)appContext.getBean("diagnosticManager");
        threadingManager = (ThreadingManager)appContext.getBean("threadingManager");
    }

    @AfterSuite(alwaysRun = true)
    public void finalizeSpring() {
        OperatingSystemUtils.finalizeCloseableResource(threadingManager);
        if(appContext != null){
            SpringApplication.exit(appContext);
        }
    }

    public static ConfigurableApplicationContext getAppContext() {
        return appContext;
    }

    public static SessionEngine getSessionEngine() {
        return sessionEngine;
    }

    public static DiagnosticManager getDiagnosticManager() {
        return diagnosticManager;
    }

    public static ThreadingManager getThreadingManager() {
        return threadingManager;
    }
}
