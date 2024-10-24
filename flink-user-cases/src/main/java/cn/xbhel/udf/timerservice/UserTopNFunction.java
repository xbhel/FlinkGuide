package cn.xbhel.udf.timerservice;

import cn.hutool.core.lang.hash.MetroHash;
import cn.xbhel.model.User;
import cn.xbhel.model.UserGroupCitation;
import cn.xbhel.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.guava30.com.google.common.collect.Lists;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author xbhel
 */
@Slf4j
@RequiredArgsConstructor
public class UserTopNFunction extends KeyedProcessFunction<String, User, UserGroupCitation> {

    @Serial
    private static final long serialVersionUID = 1872693938577524090L;
    static final ValueStateDescriptor<UserGroupCitation> TOP_N_ACTIVE_USER_DESCRIPTOR
            = new ValueStateDescriptor<>("top-n-active-user-citation", UserGroupCitation.class);

    private final int topNum;
    private final Duration randomDelay;
    private final LocalTime triggerTimeOfDay;
    private transient ValueState<UserGroupCitation> userGroupCitationState;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.userGroupCitationState = getRuntimeContext().getState(TOP_N_ACTIVE_USER_DESCRIPTOR);

    }

    @Override
    public void processElement(
            User user, KeyedProcessFunction<String, User, UserGroupCitation>.Context context,
            Collector<UserGroupCitation> collector) throws Exception {

        UserGroupCitation citation = userGroupCitationState.value();

        if (citation == null) {
            long current = context.timerService().currentProcessingTime();
            long nextTriggerTime = getNextTriggerTime(context.getCurrentKey(), current);
            context.timerService().registerProcessingTimeTimer(nextTriggerTime);

            citation = new UserGroupCitation()
                    .setGroupId(user.getGroupId())
                    .setGroupName(user.getGroupName())
                    .setUsers(Lists.newArrayList());

            log.info("Registered the next trigger timer at {} for {}",
                    TimeUtils.format(nextTriggerTime), context.getCurrentKey());
        }

        List<User> users = citation.getUsers();
        users.add(user);
        citation.setUsers(getTopNUsers(users));

        userGroupCitationState.update(citation);
    }

    @Override
    public void onTimer(
            long timestamp, KeyedProcessFunction<String, User, UserGroupCitation>.OnTimerContext ctx,
            Collector<UserGroupCitation> out) throws Exception {
        super.onTimer(timestamp, ctx, out);
        UserGroupCitation citation = userGroupCitationState.value();
        if(!CollectionUtils.isEmpty(citation.getUsers())) {
            log.info("The timer for {} is triggered at {}, resulting in an output count of: {}",
                    TimeUtils.format(timestamp), ctx.getCurrentKey(), citation.getUsers().size());

            out.collect(citation);
            userGroupCitationState.clear();

            log.info("Successfully Clear the state of {}", ctx.getCurrentKey());
        }
    }

    List<User> getTopNUsers(List<User> users) {
        var uniqueMap = users.stream().collect(Collectors.toMap(
                User::getUserId, Function.identity(), (p, n) -> p, LinkedHashMap::new));
        // Avoid to use Stream#toList method, because Flink cannot to serialize the immutable collection.
        return uniqueMap.values().stream()
                .sorted(Comparator.comparing(User::getActiveDuration).reversed())
                .limit(this.topNum)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    long getNextTriggerTime(String currentKey, long currentTime) {
        var current = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTime), TimeUtils.ASIA_SHANG_HAI);
        var nextTriggerTime = current.toLocalDate().atTime(triggerTimeOfDay);
        // before or equal to the trigger time of day.
        if (!nextTriggerTime.isAfter(current)) {
            nextTriggerTime = nextTriggerTime.plusDays(1);
        }
        return nextTriggerTime.atZone(TimeUtils.ASIA_SHANG_HAI).toInstant().toEpochMilli()
                + getDelayTime(currentKey);
    }

    long getDelayTime(String currentKey) {
        // generate a random delay for key to avoid all the keys trigger at the same time.
        long hash64 = MetroHash.hash64(currentKey.getBytes(StandardCharsets.UTF_8));
        return Math.abs(hash64) % randomDelay.toMillis();
    }

}

