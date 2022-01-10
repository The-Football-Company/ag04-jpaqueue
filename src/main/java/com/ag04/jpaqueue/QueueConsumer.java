package com.ag04.jpaqueue;

import com.ag04.jpaqueue.retry.RetryPolicy;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.transaction.PlatformTransactionManager;

public class QueueConsumer extends QueueConsumerBase {
    
    public QueueConsumer(
            QueueConsumerModule<?> queueConsumerModule,
            RetryPolicy retryPolicy,
            PlatformTransactionManager transactionManager,
            int polledItemsLimit,
            long pollingPeriodInSecs
    ) {
        super(queueConsumerModule, retryPolicy, transactionManager, polledItemsLimit, pollingPeriodInSecs);
    }

    @PostConstruct
    public void init() {
        startProcessingTask();
    }

    @PreDestroy
    public void destroy() throws Exception {
        stopProcessingTask();
        this.scheduledExecutorService.shutdownNow();
    }
    
}
