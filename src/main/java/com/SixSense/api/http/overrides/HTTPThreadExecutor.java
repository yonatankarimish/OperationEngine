package com.SixSense.api.http.overrides;

import com.SixSense.config.ThreadingConfig;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HTTPThreadExecutor extends StandardThreadExecutor {
    private static final Logger logger = LogManager.getLogger(HTTPThreadExecutor.class);
    private final ThreadingConfig.ThreadingProperties threadingProperties;

    /*Unfortuntley, startInternal() initializes a private member of the base class, which cannot be overridden without changing the executor's behaviour.
     * Therefore, we set the relevant variables to "null-ish" values, and update/set them in this overriding implementation of startInternal()*/
    public HTTPThreadExecutor(ThreadingConfig.ThreadingProperties threadingProperties) {
        super();

        this.threadingProperties = threadingProperties;
        this.namePrefix = "premature-thread-";
        this.maxThreads = 1; //must be higher than minSpareThreads to avoid an illegalArgumentException in the executor's constructor
        this.minSpareThreads = 0; //do not start or prestart any core threads
        this.maxIdleTime = 1; //must be greater than 0 to avoid an illegalArgumentException in the executor's constructor
    }

    @Override
    //please read the comment above the class constructor
    protected void startInternal() throws LifecycleException {
        super.startInternal();

        int allowedIdleMilliseconds = Math.toIntExact(Math.min(threadingProperties.getAllowedIdleTime().toMillis(),  Integer.MAX_VALUE)); //if this line of caution is ever needed, our tomcat is in a baaaad state
        this.namePrefix = threadingProperties.getThreadNamePrefix();
        this.maxThreads = threadingProperties.getMaximumThreads();
        this.minSpareThreads = threadingProperties.getMinimumThreads();
        this.maxIdleTime = allowedIdleMilliseconds;

        this.executor.setThreadFactory(new HTTPThreadFactory(this.namePrefix, this.daemon, this.getThreadPriority()));
        this.executor.setMaximumPoolSize(this.maxThreads); //must be set before setCorePoolSize is set to avoid an illegalArgumentException in the setter method
        this.executor.setCorePoolSize(this.minSpareThreads); //must be set after setMaximumPoolSize is set to avoid an illegalArgumentException in the setter method
        this.executor.setKeepAliveTime(this.maxIdleTime, TimeUnit.MILLISECONDS);

        this.executor.prestartAllCoreThreads(); //once the workaround has finished, prestart the executor with new core threads from our thread factory
    }

    public ThreadPoolExecutor getExecutor(){
        return this.executor;
    }

    public HTTPThreadFactory getThreadFactory(){
        return (HTTPThreadFactory)this.executor.getThreadFactory();
    }
}
