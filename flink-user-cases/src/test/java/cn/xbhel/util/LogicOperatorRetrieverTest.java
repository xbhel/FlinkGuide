package cn.xbhel.util;

import cn.xbhel.xml.LogicOperatorRetriever;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogicOperatorRetrieverTest {

    @Test
    void createInstance() {
        var operator = LogicOperatorRetriever.builder()
                .push("Hello")
                .and()
                .push("world")
                .or()
                .push("java")
                .build();
        assertNotNull(operator);
    }

    @Test
    void shouldAlwaysReturnsTheFirstValueWhenInvokeFirst() {
        var operator = LogicOperatorRetriever.<String>builder()
                .push("Hello")
                .and()
                .push("world")
                .build();
        assertEquals("Hello", operator.first());
        operator.next(1);
        assertEquals("Hello", operator.first());
    }

    @Test
    void andOperations() {
        var operator = LogicOperatorRetriever.<String>builder()
                .push("Hello")
                .and()
                .push("world")
                .and()
                .push("!")
                .build();
        String first = operator.first();
        String second = operator.next(1);
        String third = operator.next(1);

        assertEquals("Hello", first);
        assertEquals("world", second);
        assertEquals("!", third);
    }

    @Test
    void orOperations() {
        var operator = LogicOperatorRetriever.<String>builder()
                .push("Hello")
                .or()
                .push("world")
                .or()
                .push("!")
                .build();
        String first = operator.first();
        String end = operator.next(1);
        assertEquals("Hello", first);
        assertNull(end);
    }

    @Test
    void shouldBeCorrectlyTriggered_aSetOfOperation() {
        var operator = LogicOperatorRetriever.<String>builder()
                .push("Hello")
                .and()
                .push("world")
                .or()
                .push("java")
                .and()
                .push(",")
                .and()
                .push("please")
                .and()
                .push("advanced")
                .or()
                .push("getting start")
                .and()
                .push(".")
                .build();

        // always pass 1
        {
            var first = operator.first();
            var second = operator.next(1);
            var third = operator.next(1);
            var fourth = operator.next(1);
            var fifth = operator.next(1);
            var sixth = operator.next(1);
            var end = operator.next(1);

            assertEquals("Hello", first);
            assertEquals("world", second);
            assertEquals(",", third);
            assertEquals("please", fourth);
            assertEquals("advanced", fifth);
            assertEquals(".", sixth);
            assertNull(end);
        }

        operator.reset();
        // always pass 0
        {
            var first = operator.first();
            var second = operator.next(0);
            var third = operator.next(0);
            var end = operator.next(0);

            assertEquals("Hello", first);
            assertEquals("java", second);
            assertEquals("getting start", third);
            assertNull(end);
        }
    }
}