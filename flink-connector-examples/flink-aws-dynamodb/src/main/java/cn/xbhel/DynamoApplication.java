package cn.xbhel;

import cn.xbhel.model.entity.User;
import cn.xbhel.sink.*;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;


public final class DynamoApplication {

    public void run(StreamExecutionEnvironment env) {
        var usersStream = env.fromSequence(1, 100)
                .map(i -> new User().setUserId(Math.toIntExact(i)));

        // single mode
        var batchExecutor = new DynamoSingleBatchExecutor<>(
                WriteMode.INSERT,
                new ExecuteOptions<>("dynamo-user", User.class)
        );
        usersStream.addSink(new DynamoBatchSink<>(
                TypeInformation.of(User.class), batchExecutor));

        // mixed mode
        var mixedBatchExecutor = new DynamoMixedBatchExecutor<>(
                new ExecuteOptions<>("dynamo-user", User.class));
        var tuple2DynamoBatchSink = new DynamoBatchSink<>(
                TypeInformation.of(new TypeHint<>() {
                }), mixedBatchExecutor
        );
        usersStream.map(user -> Tuple2.of(WriteMode.INSERT, user))
                .addSink(tuple2DynamoBatchSink);
    }


}
