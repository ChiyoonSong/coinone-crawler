package cone.blockchain.crawler.configuration.webclient;

import java.util.function.Consumer;

/**
 * https://github.com/Odysseymoon/spring-webflux-template/blob/master/src/main/java/moon/odyssey/webflux/utils/lambda/ThrowingConsumer.java
 * @param <T>
 * @param <E>
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(T t) throws E;

    static <T, E extends Throwable> Consumer<T> unchecked(ThrowingConsumer<T, E> f) {
        return t -> {
            try {
                f.accept(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    static <T, E extends Throwable> Consumer<T> unchecked(ThrowingConsumer<T, E> f, Consumer<Throwable> c) {
        return t -> {
            try {
                f.accept(t);
            } catch (Throwable e) {
                c.accept(e);
            }
        };
    }
}

