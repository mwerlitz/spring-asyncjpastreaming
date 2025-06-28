package de.mw.spring.asyncjpastreaming;

import com.oath.cyclops.async.adapters.Queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

/**
 * Aspect implementation for {@link AsyncJPAStreaming}
 */
@Component
@RequiredArgsConstructor
public class AsyncJPAStreamingSupport {
    
    @Autowired
    private final AsyncJPAStreamingTransactionSupport transactionSupport;
   
    /**
     * Executes the (repository) method in an {@link Async} readonly {@link Transactional} thread and returns a lazy stream of entities.
     * The stream of entities will be outside of the transaction.
     * <p>
     * Delegates the execution of the method to our async and transactional wrapper {@link AsyncJPAStreamingTransactionSupport}.
     * Uses a readonly transaction.
     * </p>
     * Uses a {@link LinkedBlockingQueue} for communication between this calling and the async thread.
     * So different IO threads will not depend on each other.
     * 
     * @see AsyncJPAStreaming
     */
    public <T> Stream<T> streamAsyncReadonly(Supplier<Stream<T>> repositorySupplier) {
        Queue<T> queue = new Queue<>(new LinkedBlockingQueue<>());
        transactionSupport.streamAsyncTransactionalReadonlyToQueue(queue, repositorySupplier); // async in other thread
        return queue.jdkStream();
    }
    
    /**
     * Executes the (repository) method in an {@link Async} and {@link Transactional} thread and returns a lazy stream of entities.
     * The stream of entities will be outside of the transaction.
     * <p>
     * Delegates the execution of the method to our async and transactional wrapper {@link AsyncJPAStreamingTransactionSupport}.
     * Uses a normal non-readonly transaction.
     * </p>
     * Uses a {@link LinkedBlockingQueue} for communication between this calling and the async thread.
     * So different IO threads will not depend on each other.
     * 
     * @see AsyncJPAStreaming
     */
    public <T> Stream<T> streamAsync(Supplier<Stream<T>> repositorySupplier) {
        Queue<T> queue = new Queue<>(new LinkedBlockingQueue<>());
        transactionSupport.streamAsyncTransactionalToQueue(queue, repositorySupplier); // async in other thread
        return queue.jdkStream();
    }
}