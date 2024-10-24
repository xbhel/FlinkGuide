package cn.xbhel;

import cn.xbhel.model.dynamo.Customer;
import cn.xbhel.sink.*;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;

public class DynamoConnectorExample {

    void sinkConnector(DataStream<Customer> dataStream) {
        var executeOptions = new ExecuteOptions<>("your-dynamo-table", Customer.class);

        // single mode
        dataStream.addSink(new DynamoBatchSink<>(
                TypeInformation.of(Customer.class),
                () -> new DynamoSingleBatchExecutor<>(WriteMode.INSERT, executeOptions)
        ));

        // mixed mode
        dataStream.map(user -> Tuple2.of(WriteMode.INSERT, user))
                .addSink(new DynamoBatchSink<>(
                        TypeInformation.of(new TypeHint<>() {
                        }),
                        () -> new DynamoMixedBatchExecutor<>(executeOptions)
                ));
    }

}
