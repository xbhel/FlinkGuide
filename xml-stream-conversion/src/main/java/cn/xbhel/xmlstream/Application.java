package cn.xbhel.xmlstream;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Application {

    public static void main(String[] args) throws Exception {
        var env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1)
                .fromSequence(0, 100)
                .print();
        env.execute("xml-stream-conversion");
    }

}
