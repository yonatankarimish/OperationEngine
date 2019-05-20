package com.SixSense.engine;

import com.SixSense.Main;
import com.SixSense.queue.WorkerQueue;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testng.annotations.*;

public class EngineTestUtils {
    private static ConfigurableApplicationContext appContext;
    private static SessionEngine engineInstance;
    private static WorkerQueue queueInstance;

    @BeforeGroups("engine")
    public void beforeClass() {
        appContext = SpringApplication.run(Main.class);
        engineInstance = (SessionEngine)appContext.getBean("sessionEngine");
        queueInstance = (WorkerQueue)appContext.getBean("workerQueue");
    }

    @AfterGroups("engine")
    public void afterClass() {
        if(engineInstance != null){
            engineInstance.close();
        }
        if(queueInstance != null){
            queueInstance.close();
        }
        if(appContext != null){
            SpringApplication.exit(appContext);
        }
    }

    public static ConfigurableApplicationContext getAppContext() {
        return appContext;
    }

    public static SessionEngine getEngineInstance() {
        return engineInstance;
    }

    public static WorkerQueue getQueueInstance() {
        return queueInstance;
    }
}
