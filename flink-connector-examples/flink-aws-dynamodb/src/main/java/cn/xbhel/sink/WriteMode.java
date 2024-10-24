package cn.xbhel.sink;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum WriteMode {

    INSERT("insert"),
    UPDATE("update"),
    DELETE("delete");

    private final String mode;
}
