package cn.xbhel.util;

public class LogicOperatorRetriever<T> {

    private final Entry<T> head;
    private Entry<T> cursor;

    LogicOperatorRetriever(Entry<T> head) {
        this.head = head;
        this.cursor = head;
    }

    public T first() {
        if (head == null) {
            return null;
        }
        return head.data;
    }

    public T next(int stateOfPreTime) {
        if(cursor == null) {
            return null;
        }
        Entry<T> next;
        while ((next = cursor.next) != null) {
            if(stateOfPreTime == cursor.state) {
                cursor = next;
                return next.data;
            }
            cursor = next;
        }
       return null;
    }

    public void reset() {
        this.cursor = head;
    }

    public static <T> LogicOperatorBuilder<?, T> builder() {
        return new LogicOperatorBuilder<>();
    }

    static class Entry<T> {
        T data;
        Entry<T> next;
        /**
         * The {@code state} represent the execution state from previous time.
         * <p>
         * The {@code state} may introduce numerous options in the future, therefore,
         * it's advisable to use numbers to represent the state rather than boolean values.
         * {@code ToIntFunction<Integer> state;}
         * <p>
         * This seems to be simpler, using numbers to represent the state rather than a ToIntFunction.
         */
        int state;
    }

}
