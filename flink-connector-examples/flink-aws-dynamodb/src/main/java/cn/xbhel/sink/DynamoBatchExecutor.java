package cn.xbhel.sink;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.client.program.rest.retry.ExponentialWaitStrategy;
import org.apache.flink.client.program.rest.retry.WaitStrategy;
import org.apache.flink.shaded.guava30.com.google.common.collect.Lists;
import org.apache.flink.util.Preconditions;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class DynamoBatchExecutor<T> {

    static final int DYNAMO_SUPPORTED_MAX_BATCH_SIZE = 25;

    private final String tableName;
    private final long maxRetries;
    protected final WaitStrategy waitStrategy;
    protected final DynamoDbEnhancedClient client;

    protected DynamoBatchExecutor(Class<?> itemClass, String tableName,
                                  long retryDelayMs, long maxRetryDelayMs, long maxRetries) {
        Preconditions.checkNotNull(itemClass);
        Preconditions.checkNotNull(tableName);

        this.tableName = tableName;
        this.maxRetries = maxRetries;
        this.client = DynamoUtil.getInstance();
        this.waitStrategy = new ExponentialWaitStrategy(retryDelayMs, maxRetryDelayMs);
    }

    public void executeBatch(List<T> items) {
        var chunks = Lists.partition(items, DYNAMO_SUPPORTED_MAX_BATCH_SIZE);
        chunks.forEach(this::executeBatchInternal);
    }

    protected abstract WriteBatch configureWriteBatch(List<T> items);

    protected abstract WriteBatch configureFailureWriteBatch(BatchWriteResult writeResult);

    protected void executeBatchInternal(List<T> items) {
        var retries = 0;
        var writeBatch = configureWriteBatch(items);

        while (retries <= maxRetries) {

            var writeRequest = BatchWriteItemEnhancedRequest.builder()
                    .writeBatches(writeBatch).build();
            var writeResult = client.batchWriteItem(writeRequest);
            writeBatch = configureFailureWriteBatch(writeResult);

            if (Objects.isNull(writeBatch) ||
                    CollectionUtils.isEmpty(writeBatch.writeRequests())) {
                log.debug("Successfully executed batch to table:{}, size: {}.",
                        tableName, items.size());
                break;
            }

            log.warn("{} unprocessed write requests to table:{}.",
                    writeBatch.writeRequests().size(), tableName);

            var sleepTime = waitStrategy.sleepTime(++retries);
            try {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", e);
            }
        }
    }


    static <E> WriteBatch configureFailureWriteBatch(
            BatchWriteResult writeResult, DynamoDbTable<E> dynamoDbTable, Class<E> itemClass
    ) {
        var builder = WriteBatch.builder(itemClass);
        var unprocessedPutItems = writeResult.unprocessedPutItemsForTable(dynamoDbTable);
        var unprocessedDeleteItems = writeResult.unprocessedDeleteItemsForTable(dynamoDbTable);
        if (CollectionUtils.isNotEmpty(unprocessedPutItems)) {
            unprocessedPutItems.forEach(builder::addPutItem);
        }
        if (CollectionUtils.isNotEmpty(unprocessedDeleteItems)) {
            unprocessedDeleteItems.forEach(builder::addDeleteItem);
        }
        return builder.build();
    }

}
