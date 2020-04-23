package com.SixSense;

import com.SixSense.engine.DiagnosticManager;
import com.SixSense.engine.SessionEngine;
import com.SixSense.threading.ThreadingManager;
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
        finalizeCloseableResource(sessionEngine, threadingManager);
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

    public static void finalizeCloseableResource(Closeable... closeables){
        for(Closeable resource : closeables) {
            if(resource == null){
                logger.warn("Attempted to finalize a null resource");
            }else{
                try {
                    resource.close();
                } catch (Exception e) {
                    String resourceClassName = closeables.getClass().toString();
                    logger.error("Failed to close instance of " + resourceClassName, e);
                }
            }
        }
    }
}
