package cn.xbhel.sink;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.List;

public class DynamoSingleBatchExecutor<T> extends DynamoBatchExecutor<T> {

    private final WriteMode writeMode;
    private final ExecuteOptions<T> options;
    private final DynamoDbTable<T> dynamoDbTable;

    public DynamoSingleBatchExecutor(WriteMode writeMode, ExecuteOptions<T> options) {
        super(options.getItemClass(), options.getTableName(), options.getRetryDelayMs(),
                options.getMaxRetryDelayMs(), options.getMaxRetries());
        this.writeMode = writeMode;
        this.options = options;
        this.dynamoDbTable = client.table(options.getTableName(),
                TableSchema.fromClass(options.getItemClass()));
    }

    @Override
    protected WriteBatch configureWriteBatch(List<T> items) {
        var builder = WriteBatch.builder(options.getItemClass());
        if (writeMode == WriteMode.DELETE) {
            items.forEach(builder::addDeleteItem);
        } else if (writeMode == WriteMode.INSERT || writeMode == WriteMode.UPDATE) {
            items.forEach(builder::addPutItem);
        }
        return builder.build();
    }

    @Override
    protected WriteBatch configureFailureWriteBatch(BatchWriteResult writeResult) {
        return configureFailureWriteBatch(writeResult, dynamoDbTable, options.getItemClass());
    }

}

