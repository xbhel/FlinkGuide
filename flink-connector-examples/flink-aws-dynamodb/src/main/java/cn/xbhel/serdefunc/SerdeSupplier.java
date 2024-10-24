package cn.xbhel.serdefunc;


import java.io.Serializable;
import java.util.function.Supplier;

public interface SerdeSupplier<T> extends Supplier<T>, Serializable {
}
