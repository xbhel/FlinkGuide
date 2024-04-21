package cn.xbhel.xmlstream.func;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class NodeActionBuilderTest {

    @Test
    void shouldCreateAnInstance() {
        var retriever = ActionLogicOperatorBuilder.builder()
                .action("", "", "")
                .and()
                .action("", "", "")
                .build();
        Assertions.assertNotNull(retriever);
    }

}