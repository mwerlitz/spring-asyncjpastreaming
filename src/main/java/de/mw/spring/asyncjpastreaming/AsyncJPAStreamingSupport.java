package de.mw.spring.asyncjpastreaming;

import com.oath.cyclops.async.adapters.Queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Aspect implementation for {@link AsyncJPAStreaming}
 */
@Component
public class AsyncJPAStreamingSupport {

    private final AsyncJPAStreamingTransactionSupport transactionSupport;

    private final int maxBufferCapacity;

    private final boolean enabled;


    AsyncJPAStreamingSupport(AsyncJPAStreamingTransactionSupport transactionSupport,
                             @Value("${app.configuration.asyncjpastreaming.maxBufferCapacity:" + Integer.MAX_VALUE + "}") int maxBufferCapacity,
                             @Value("${app.configuration.asyncjpastreaming.enabled:true}") boolean enabled) {
        this.transactionSupport = transactionSupport;
        this.maxBufferCapacity = maxBufferCapacity;
        this.enabled = enabled;
    }

    /**
     * Executes the (repository) method in an {@link Async} optionally readonly {@link Transactional} thread
     * and returns a lazy stream of entities. The stream of entities will be outside of the transaction.
     * <p>
     * Delegates the execution of the method to our async and transactional wrapper {@link AsyncJPAStreamingTransactionSupport}.
     * Uses a readonly transaction.
     * </p>
     * Uses a {@link LinkedBlockingQueue} for communication between this calling and the async thread.
     * So different IO threads will not depend on each other.
     * <p>
     * When streaming is disabled the behaviour is emulated with fetching the whole stream to a List and
     * then returning the List's stream.
     *
     * @see AsyncJPAStreaming
     */
    public <T> Stream<T> streamAsync(Supplier<Stream<T>> repositorySupplier, boolean readonly, boolean clearEntityManager, int bufferCapacity) {
        if (enabled) {
            if (transactionSupport.isAsyncJPAStreaming()) {
                return repositorySupplier.get(); // prevent nested streaming
            }

            int capacity = Math.min(bufferCapacity, maxBufferCapacity);
            BlockingQueue<T> underlyingQueue = new LinkedBlockingQueue<>(capacity);
            Queue<T> queue = new Queue<>(underlyingQueue);
            if (readonly) {
                transactionSupport.streamAsyncTransactionalReadonlyToQueue(queue, repositorySupplier, clearEntityManager) // async in other thread and transaction
                        .exceptionally(handleException(queue));
            } else {
                transactionSupport.streamAsyncTransactionalToQueue(queue, repositorySupplier, clearEntityManager) // async in other thread and transaction
                        .exceptionally(handleException(queue));
            }

            return queue.jdkStream()
                        .onClose(() -> {
                            // make sure we pass on the close action to the queue, otherwise a memory and connection leak
                            // will happen when the capacity of the queue is not unlimited
                            queue.close();
                            queue.closeAndClear();
                            underlyingQueue.clear();
                        });
        } else {
            if (readonly) {
                return transactionSupport.streamTransactionalReadonly(repositorySupplier, clearEntityManager); // same thread joining same transaction
            } else {
                return transactionSupport.streamTransactional(repositorySupplier, clearEntityManager); // same thread joining same transaction
            }
        }
    }

    private <T> Function<Throwable, Void> handleException(Queue<T> queue) {
        return throwable -> {
            if (queue.isOpen()) {
                queue.addError(throwable);
                queue.close();
            }
            return null;
        };
    }

}