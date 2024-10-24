package cn.xbhel.udf.timerservice;

import cn.xbhel.model.User;
import cn.xbhel.model.UserGroupCitation;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.functions.util.ListCollector;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.guava30.com.google.common.collect.Lists;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static cn.xbhel.util.TimeUtils.toEpochMills;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTopNFunctionTest {

    @Test
    void testProcessElementWithFirstAccess() throws Exception {
        var function = new UserTopNFunction(3, Duration.ofMinutes(15), LocalTime.MIDNIGHT);
        var user = new User("id", "name", "f", "gid", "g-name", 10L);
        try (var testHarness = ProcessFunctionTestHarnesses.forKeyedProcessFunction(
                function, User::getGroupId, TypeInformation.of(String.class))) {
            testHarness.processElement(new StreamRecord<>(user));
            var citationValueState = function.getRuntimeContext().getState(UserTopNFunction.TOP_N_ACTIVE_USER_DESCRIPTOR);
            assertThat(citationValueState.value()).isEqualTo(
                    new UserGroupCitation("gid", "g-name", List.of(user)));
            assertThat(testHarness.getProcessingTimeService().getNumActiveTimers())
                    .isEqualTo(1);
        }
    }

    @Test
    void testProcessElementWithSequentiallyAccess(
            @Mock ValueState<UserGroupCitation> citationValueState,
            @Mock KeyedProcessFunction<String, User, UserGroupCitation>.Context context
    ) throws Exception {
        var user = new User("id", "name", "f", "gid", "g-name", 10L);
        var citation = new UserGroupCitation("g1", "g1-name", Lists.newArrayList(user));
        var function = new UserTopNFunction(3, Duration.ofMinutes(15), LocalTime.MIDNIGHT);
        var runtimeCtx = mock(RuntimeContext.class);

        when(runtimeCtx.getState(UserTopNFunction.TOP_N_ACTIVE_USER_DESCRIPTOR)).thenReturn(citationValueState);
        when(citationValueState.value()).thenReturn(citation);
        function.setRuntimeContext(runtimeCtx);
        function.open(new Configuration());

        for (User item : getUsers(10)) {
            function.processElement(item, context, new ListCollector<>(new ArrayList<>()));
        }

        verify(context, never()).timerService();
        assertThat(citationValueState.value().getUsers()).containsExactly(
                new User("id", "name", "f", "gid", "g-name", 10L),
                new User("user9", "name9", "f", "g1", "g1-name", 9L),
                new User("user8", "name8", "f", "g1", "g1-name", 8L)
        );
    }

    @Test
    void testOnTimerSuccess(
            @Mock ValueState<UserGroupCitation> citationValueState,
            @Mock KeyedProcessFunction<String, User, UserGroupCitation>.OnTimerContext ctx
    ) throws Exception {
        var citation = new UserGroupCitation("g1", "g1-name", getUsers(3));
        var function = new UserTopNFunction(3, Duration.ofMinutes(15), LocalTime.MIDNIGHT);
        var runtimeCtx = mock(RuntimeContext.class);
        var result = new ArrayList<UserGroupCitation>();
        when(runtimeCtx.getState(UserTopNFunction.TOP_N_ACTIVE_USER_DESCRIPTOR)).thenReturn(citationValueState);
        when(citationValueState.value()).thenReturn(citation);
        function.setRuntimeContext(runtimeCtx);
        function.open(new Configuration());
        function.onTimer(1L, ctx, new ListCollector<>(result));

        assertThat(result).containsExactly(citation);
        verify(citationValueState, times(1)).value();
        verify(citationValueState, times(1)).clear();
    }

    @Test
    void testOnTimerWithEmpty(
            @Mock ValueState<UserGroupCitation> citationValueState,
            @Mock KeyedProcessFunction<String, User, UserGroupCitation>.OnTimerContext ctx
    ) throws Exception {
        var citation = new UserGroupCitation("g1", "g1-name", Lists.newArrayList());
        var function = new UserTopNFunction(3, Duration.ofMinutes(15), LocalTime.MIDNIGHT);
        var runtimeCtx = mock(RuntimeContext.class);
        var result = new ArrayList<UserGroupCitation>();
        when(runtimeCtx.getState(UserTopNFunction.TOP_N_ACTIVE_USER_DESCRIPTOR)).thenReturn(citationValueState);
        when(citationValueState.value()).thenReturn(citation);
        function.setRuntimeContext(runtimeCtx);
        function.open(new Configuration());
        function.onTimer(1L, ctx, new ListCollector<>(result));

        assertThat(result).isEmpty();
        verify(citationValueState, times(1)).value();
        verify(citationValueState, never()).clear();
    }

    @Test
    void testRandomDelay() {
        var delay = Duration.ofMinutes(15);
        var function = new UserTopNFunction(5, delay, LocalTime.MIDNIGHT);
        long timeOfCurrentKey = function.getDelayTime(RandomStringUtils.random(20));
        assertThat(timeOfCurrentKey).isNotNegative().isLessThan(delay.toMillis());
    }

    @ParameterizedTest
    @MethodSource({"sourceForTestGetNextTriggerTime"})
    void testGetNextTriggerTime(LocalTime triggerTimeOfDay, LocalDateTime current, LocalDateTime expected) {
        var spyFunction = spy(new UserTopNFunction(5, Duration.ofMinutes(15), triggerTimeOfDay));
        doReturn(0L).when(spyFunction).getDelayTime(anyString());
        var nextTriggerTime = spyFunction.getNextTriggerTime(RandomStringUtils.random(20), toEpochMills(current));
        assertThat(nextTriggerTime).isEqualTo(toEpochMills(expected));
    }

    @Test
    void testGetTopNUsers() {
        var users = getUsers(10);
        Collections.shuffle(users);
        var function = new UserTopNFunction(3, Duration.ofMinutes(15), LocalTime.MIDNIGHT);
        var topNUsers = function.getTopNUsers(users);
        assertThat(topNUsers).contains(
                new User("user9", "name9", "f", "g1", "g1-name", 9L),
                new User("user8", "name8", "f", "g1", "g1-name", 8L),
                new User("user7", "name7", "f", "g1", "g1-name", 7L)
        );

    }

    @Test
    void testGetTopUserWithDeduplicate() {
        var users = getUsers(10);
        users.addAll(getUsers(10));
        Collections.shuffle(users);
        var function = new UserTopNFunction(3, Duration.ofMinutes(15), LocalTime.MIDNIGHT);
        var topNUsers = function.getTopNUsers(users);
        assertThat(topNUsers).containsExactly(
                new User("user9", "name9", "f", "g1", "g1-name", 9L),
                new User("user8", "name8", "f", "g1", "g1-name", 8L),
                new User("user7", "name7", "f", "g1", "g1-name", 7L)
        );
    }

    private static ArrayList<User> getUsers(int num) {
        return IntStream.range(0, num).mapToObj(i -> new User()
                        .setUserId("user" + i)
                        .setUsername("name" + i)
                        .setGender("f")
                        .setGroupId("g1")
                        .setGroupName("g1-name")
                        .setActiveDuration((long) i))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Stream<Arguments> sourceForTestGetNextTriggerTime() {
        var triggerTimeOfDay = LocalTime.of(10, 0, 0);
        return Stream.of(
                // current time is before trigger time of day
                of(triggerTimeOfDay, LocalDateTime.of(2024, 6, 12, 0, 0, 0), LocalDateTime.of(2024, 6, 12, 10, 0, 0)),
                // current time is after trigger time of day
                of(triggerTimeOfDay, LocalDateTime.of(2024, 6, 12, 15, 51, 0), LocalDateTime.of(2024, 6, 13, 10, 0, 0)),
                // current time is equal to trigger time of day
                of(triggerTimeOfDay, LocalDateTime.of(2024, 6, 12, 10, 0, 0), LocalDateTime.of(2024, 6, 13, 10, 0, 0))
        );
    }

}