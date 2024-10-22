package cn.xbhel.util;

public class LogicOperatorBuilder<T extends LogicOperatorBuilder<T, D>, D> {

    private final LogicOperatorRetriever.Entry<D> pseudoHead
            = new LogicOperatorRetriever.Entry<>();
    private LogicOperatorRetriever.Entry<D> tail = pseudoHead;

    public Operator push(D data) {
        var entry = new LogicOperatorRetriever.Entry<D>();
        entry.data = data;
        tail.next = entry;
        tail = entry;
        return new Operator();
    }

    public class Operator {

        @SuppressWarnings("unchecked")
        private final T outer = (T) LogicOperatorBuilder.this;

        public T and() {
            tail.state = 1;
            return outer;
        }

        public T or() {
            tail.state = 0;
            return outer;
        }

        public LogicOperatorRetriever<D> build() {
            return new LogicOperatorRetriever<>(pseudoHead.next);
        }
    }
}
