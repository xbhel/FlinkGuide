package cn.xbhel.sink;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class ExecuteOptions<T> implements Serializable {

    private static final long DEFAULT_RETRY_DELAY_MS = 300L;
    private static final long DEFAULT_MAX_RETRY_DELAY_MS = 5_000L;
    private static final int DEFAULT_MAX_RETRIES = 3;

    private String tableName;
    private Class<T> itemClass;
    private long maxRetryDelayMs = DEFAULT_MAX_RETRY_DELAY_MS;
    private long retryDelayMs = DEFAULT_RETRY_DELAY_MS;
    private int maxRetries = DEFAULT_MAX_RETRIES;

    public ExecuteOptions(String tableName, Class<T> itemClass) {
        this.tableName = tableName;
        this.itemClass = itemClass;
    }
}
