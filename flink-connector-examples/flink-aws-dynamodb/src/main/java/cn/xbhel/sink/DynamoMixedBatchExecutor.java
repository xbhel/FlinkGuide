package cn.xbhel.sink;

import org.apache.flink.api.java.tuple.Tuple2;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.List;

public class DynamoMixedBatchExecutor<T> extends DynamoBatchExecutor<Tuple2<WriteMode, T>> {

    private final ExecuteOptions<T> options;
    private final DynamoDbTable<T> dynamoDbTable;

    public DynamoMixedBatchExecutor(ExecuteOptions<T> options) {
        super(options.getItemClass(), options.getTableName(), options.getRetryDelayMs(),
                options.getMaxRetryDelayMs(), options.getMaxRetries());
        this.options = options;
        this.dynamoDbTable = client.table(options.getTableName(),
                TableSchema.fromClass(options.getItemClass()));
    }

    @Override
    protected WriteBatch configureWriteBatch(List<Tuple2<WriteMode, T>> items) {
        var builder = WriteBatch.builder(options.getItemClass())
                .mappedTableResource(dynamoDbTable);
        for (var modeWithItem: items) {
            var mode = modeWithItem.f0;
            var item = modeWithItem.f1;
            if (mode == WriteMode.DELETE) {
                builder.addDeleteItem(item);
            } else if (mode == WriteMode.INSERT || mode == WriteMode.UPDATE) {
                builder.addPutItem(item);
            }
        }
        return builder.build();
    }

    @Override
    protected WriteBatch configureFailureWriteBatch(BatchWriteResult writeResult) {
        return configureFailureWriteBatch(writeResult, dynamoDbTable, options.getItemClass());
    }
}

