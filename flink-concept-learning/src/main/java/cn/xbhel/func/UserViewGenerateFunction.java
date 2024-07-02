package cn.xbhel.func;

import cn.xbhel.model.UserGroupCitation;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.io.Serial;

public class UserViewGenerateFunction extends ProcessFunction<UserGroupCitation, String> {

    @Serial
    private static final long serialVersionUID = 5879416126755542861L;

    @Override
    public void processElement(UserGroupCitation userGroupCitation,
                               ProcessFunction<UserGroupCitation, String>.Context context,
                               Collector<String> collector) throws Exception {

    }


}
