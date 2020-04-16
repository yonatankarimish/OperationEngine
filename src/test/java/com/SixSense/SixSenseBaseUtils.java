package com.SixSense;

import com.SixSense.engine.DiagnosticManager;
import com.SixSense.engine.SessionEngine;
import com.SixSense.threading.ThreadingManager;
import com.SixSense.util.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

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
        FileUtils.finalizeCloseableResource(sessionEngine, threadingManager);
        if(appContext != null){
            SpringApplication.exit(appContext);
        }
    }

    public static DiagnosticManager getDiagnosticManager() {
        return diagnosticManager;
    }
}
