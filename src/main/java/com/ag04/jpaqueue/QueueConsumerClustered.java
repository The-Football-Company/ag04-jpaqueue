package com.ag04.jpaqueue;

import com.ag04.jpaqueue.retry.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;
import javax.annotation.PreDestroy;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.leader.event.OnRevokedEvent;

/**
 * This is the version of the QueueConsumer that should be used in clustered envirinment, 
 * when multiple instances of the application are runing at the same time, and there is a need to ensure that only
 * one instance of QueueConsumer is running.
 * 
 * @author dmadunic
 */
public class QueueConsumerClustered extends QueueConsumerBase {
    private final Logger logger = LoggerFactory.getLogger(QueueConsumerClustered.class);

    public QueueConsumerClustered(
            QueueConsumerModule<?> queueConsumerModule,
            RetryPolicy retryPolicy,
            PlatformTransactionManager transactionManager,
            int polledItemsLimit,
            long pollingPeriodInSecs
    ) {
        super(queueConsumerModule, retryPolicy, transactionManager, polledItemsLimit, pollingPeriodInSecs);
    }
    
    @EventListener(OnGrantedEvent.class)
    public void onGrantedEvent() {
        logger.info("--> Granted leadership to this app instance");
        startProcessingTask();
    }

    @EventListener(OnRevokedEvent.class)
    public void onRevokedEvent() {
        logger.info("--> Revoked leadership from this app instance");
        stopProcessingTask();
    }

    @PreDestroy
    public void destroy() throws Exception {
        this.scheduledExecutorService.shutdownNow();
    }
}
