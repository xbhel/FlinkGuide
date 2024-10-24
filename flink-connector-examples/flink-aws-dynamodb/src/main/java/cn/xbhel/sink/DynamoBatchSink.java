package cn.xbhel.sink;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.util.concurrent.ExecutorThreadFactory;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DynamoBatchSink<T> extends RichSinkFunction<T> implements CheckpointedFunction {

    @Serial
    private static final long serialVersionUID = 8130182545701184852L;
    private static final long DEFAULT_BATCH_INTERVAL_MS = 5_000L;
    private static final int DEFAULT_BATCH_SIZE = DynamoBatchExecutor.DYNAMO_SUPPORTED_MAX_BATCH_SIZE;

    private final long batchSize;
    private final long batchIntervalMs;
    private final TypeInformation<T> elementType;
    private final DynamoBatchExecutor<T> batchExecutor;

    private transient List<T> bufferedElements;
    private transient ListState<T> elementListState;
    private transient long numPendingElement = 0;
    private transient volatile boolean closed = false;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> scheduledFuture;
    private final AtomicReference<Throwable> failureThrowable = new AtomicReference<>();

    public DynamoBatchSink(TypeInformation<T> elementType, DynamoBatchExecutor<T> batchExecutor) {
        this(elementType, batchExecutor, DEFAULT_BATCH_SIZE, DEFAULT_BATCH_INTERVAL_MS);
    }

    public DynamoBatchSink(TypeInformation<T> elementType, DynamoBatchExecutor<T> executor,
                           long batchSize, long batchIntervalMs) {
        this.elementType = elementType;
        this.batchExecutor = executor;
        this.batchSize = batchSize;
        this.batchIntervalMs = batchIntervalMs;
    }

    @Override
    public void open(Configuration parameters) {
        if (this.batchSize > 1 && this.batchIntervalMs > 0) {
            this.scheduler = Executors.newScheduledThreadPool(1, new ExecutorThreadFactory("dynamo-batch-sink"));
            this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(() -> {
                        synchronized (this) {
                            if (!this.closed && failureThrowable.get() != null) {
                                try {
                                    this.flush();
                                } catch (Exception e) {
                                    failureThrowable.compareAndSet(null, e);
                                }
                            }
                        }
                    },
                    batchIntervalMs, batchIntervalMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void invoke(T value, Context context) {
        // Synchronized access is required to prevent the scheduled timer
        // and the main thread from modifying bufferedElements at the same time.
        checkErrorAndRethrow();
        bufferedElements.add(value);
        ++numPendingElement;
        if (batchSize > 0 && numPendingElement > batchSize) {
            flush();
        }
    }

    public synchronized void flush() {
        // Synchronized access is required to prevent the scheduled timer
        // and the main thread from modifying bufferedElements at the same time.
        batchExecutor.executeBatch(bufferedElements);
        bufferedElements.clear();
        numPendingElement = 0;
        checkErrorAndRethrow();
    }

    private void checkErrorAndRethrow() {
        Throwable cause = failureThrowable.get();
        if (cause != null) {
            throw new IllegalStateException("An error occurred in DynamoBatchSink.", cause);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                if (scheduler != null) {
                    scheduler.shutdown();
                }
            }
            if (numPendingElement > 0) {
                flush();
            }
        }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        this.elementListState.clear();
        this.elementListState.addAll(this.bufferedElements);
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        var descriptor = new ListStateDescriptor<>("buffered-element", elementType);
        this.bufferedElements = new ArrayList<>();
        this.elementListState = context.getOperatorStateStore().getListState(descriptor);
        if (context.isRestored()) {
            this.elementListState.get().forEach(this.bufferedElements::add);
        }
    }
}
