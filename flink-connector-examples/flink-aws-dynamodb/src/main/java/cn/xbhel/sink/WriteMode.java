package cn.xbhel.sink;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WriteMode {

    INSERT("insert"),
    UPDATE("update"),
    DELETE("delete");

    private final String mode;
}
