package cn.xbhel.dom;

import cn.xbhel.util.LogicOperatorBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.flink.api.java.tuple.Tuple3;
import org.dom4j.Node;

@RequiredArgsConstructor
public class ActionLogicOperatorBuilder extends LogicOperatorBuilder<ActionLogicOperatorBuilder, Tuple3<String, ?, ?>> {

    public Operator action(String action, String xpath, String data) {
        return this.push(Tuple3.of(action, xpath, data));
    }

    public Operator action(String action, NodeSelector<Node> xpath, Node data) {
        return this.push(Tuple3.of(action, xpath, data));
    }

    public static ActionLogicOperatorBuilder builder() {
        return new ActionLogicOperatorBuilder();
    }

}
