package cn.xbhel;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public final class DynamoJob {

    public static void main(String[] args) throws Exception {
        var parameterTool = ParameterTool.fromArgs(args);
        var env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(parameterTool);
        new DynamoApplication().run(env);
        env.execute("DynamoDB Streaming Job");
    }
}
