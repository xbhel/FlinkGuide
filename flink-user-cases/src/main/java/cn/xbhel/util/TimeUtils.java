package cn.xbhel.util;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    public static final ZoneId ASIA_SHANG_HAI = ZoneId.of("Asia/Shanghai");

    private TimeUtils() throws InstantiationException {
        throw new InstantiationException("The class cannot be created.");
    }

    public static String format(long epochMills) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMills), ASIA_SHANG_HAI)
                .format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public static long toEpochMills(LocalDateTime dateTime) {
        return dateTime.atZone(ASIA_SHANG_HAI).toInstant().toEpochMilli();
    }

}
