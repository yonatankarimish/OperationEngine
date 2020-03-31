package com.SixSense.api.http.overrides;

import com.SixSense.config.ThreadingConfig;
import org.apache.catalina.core.StandardThreadExecutor;

public class HTTPThreadExecutor extends StandardThreadExecutor {
    public HTTPThreadExecutor(ThreadingConfig.ThreadingProperties threadingProperties) {
        super();

        //Override tomcat default values
        int allowedIdleMilliseconds = Math.toIntExact(Math.min(threadingProperties.getAllowedIdleTime().toMillis(),  Integer.MAX_VALUE)); //if this line of caution is ever needed, our tomcat is in a baaaad state

        this.namePrefix = threadingProperties.getThreadNamePrefix();
        this.maxThreads = threadingProperties.getMaximumThreads();
        this.minSpareThreads = threadingProperties.getMinimumThreads();
        this.maxIdleTime = allowedIdleMilliseconds;
    }
}
