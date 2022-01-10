package com.ag04.jpaqueue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ag04.jpaqueue.retry.RetryPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class QueueConsumerBase {
    private final Logger logger = LoggerFactory.getLogger(QueueConsumerBase.class);

    protected final TransactionTemplate transactionTemplate;

    protected final QueueConsumerModule queueConsumerModule;
    protected final RetryPolicy retryPolicy;
    protected final long pollingPeriodInSecs;
    protected final int itemsPollSize;

    protected final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> processingTask;

    public QueueConsumerBase(
            QueueConsumerModule<?> queueConsumerModule,
            RetryPolicy retryPolicy,
            PlatformTransactionManager transactionManager,
            int polledItemsLimit,
            long pollingPeriodInSecs
    ) {
        if (polledItemsLimit < 1) {
            throw new IllegalArgumentException("Polled items size cannot be less than 1, but is " + polledItemsLimit);
        }
        if (pollingPeriodInSecs < 1) {
            throw new IllegalArgumentException("Polling period cannot be less than 1, but is " + polledItemsLimit);
        }
        this.queueConsumerModule = Objects.requireNonNull(queueConsumerModule);
        this.retryPolicy = Objects.requireNonNull(retryPolicy);
        this.pollingPeriodInSecs = pollingPeriodInSecs;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.itemsPollSize = polledItemsLimit;
    }


    protected void startProcessingTask() {
        logger.info("Starting queue processing task with delay of {} secs", this.pollingPeriodInSecs);
        Runnable command = this::processQueuedItems;
        this.processingTask = this.scheduledExecutorService.scheduleWithFixedDelay(command, pollingPeriodInSecs, pollingPeriodInSecs, TimeUnit.SECONDS);
    }

    protected void stopProcessingTask() {
        if (this.processingTask != null && !this.processingTask.isCancelled()) {
            logger.info("Stopping queue processing task");
            this.processingTask.cancel(true);
        }
    }

    public void processQueuedItems() {
        try {
            ZonedDateTime now = ZonedDateTime.now();
            List<?> itemIds = this.queueConsumerModule.findItemIdsWhereQueueingNextAttemptTimeIsBefore(now, itemsPollSize);

            if (!itemIds.isEmpty()) {
                int count = 0; 
                int size = itemIds.size();
                logger.info("Fetched {} pending queued items", itemIds.size());
                for (Object itemId : itemIds) {
                    count++;
                    processItemAndHandleErrorIfRaised(itemId, count, size);
                }
            }
        } catch (Throwable th) {
            logger.error("Error while fetching queued items: " + th.getMessage(), th);
        }
    }

    private void processItemAndHandleErrorIfRaised(Object itemId, int count, int size) {
        try {
            executeUnderTransaction(() -> processItem(itemId, count, size));
        } catch (Throwable error) {
            executeUnderTransaction(() -> registerProcessingFailure(itemId, error));
        }
    }

    private void executeUnderTransaction(Runnable runnable) {
        this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                runnable.run();
            }
        });
    }

    public void processItem(Object itemId, int count, int size) {
        Optional<QueueingState> queueingStateOptional = this.queueConsumerModule.processItem(itemId, count, size);
        if (queueingStateOptional.isPresent()) {
            queueingStateOptional.get().registerAttemptSuccess(ZonedDateTime.now());
        } else {
            logger.warn("No queued item found under ID {} to process it", itemId);
        }
    }

    private void registerProcessingFailure(Object itemId, Throwable error) {
        logger.error("Error while processing item by ID " + itemId + ": " + error.getMessage(), error);

        Optional<QueueingState> queueingStateOptional = this.queueConsumerModule.getQueueingStateForItem(itemId);
        if (queueingStateOptional.isPresent()) {
            QueueingState queueingState = queueingStateOptional.get();
            queueingState.registerAttemptFailure(ZonedDateTime.now(), error);

            Optional<ZonedDateTime> retryAttemptTimeOptional = retryPolicy.calculateNextAttemptTime(queueingState.getLastAttemptTime(), queueingState.getAttemptCount());
            if (retryAttemptTimeOptional.isPresent()) {
                ZonedDateTime nextAttemptTime = retryAttemptTimeOptional.get();
                logger.info("Retry for item by ID {} scheduled for time: {}", itemId, nextAttemptTime);
                queueingState.scheduleNextAttempt(nextAttemptTime);
            } else {
                logger.warn("No retry scheduled for item by ID {}", itemId);
            }
        } else {
            logger.warn("No queued item found under ID {} to register failed attempt", itemId);
        }
    }
}
