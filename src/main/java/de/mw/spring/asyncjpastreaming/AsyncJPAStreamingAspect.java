package de.mw.spring.asyncjpastreaming;

import com.oath.cyclops.async.adapters.Queue;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Stream;

import lombok.SneakyThrows;

/**
 * Aspect implemenation for {@link AsyncJPAStreaming}
 */
@Aspect
@Component
class AsyncJPAStreamingAspect {
    
    @Autowired
    private AsyncJPAStreamingTransactionSupport transactionSupport;
   
    /**
     * Delegates the execution of the repository method to our async and transactional wrapper.
     * Uses a {@link LinkedBlockingQueue} for communication between this calling and the async thread.
     */
    protected <T> Stream<T> streamAsync(Supplier<Stream<T>> repositorySupplier) {
        Queue<T> queue = new Queue<>(new LinkedBlockingQueue<>());
        transactionSupport.streamAsyncTransactionalToQueue(queue, repositorySupplier); // async in other thread
        return queue.jdkStream();
    }
    
    /**
     * Wrapper method for executing the repository method.
     * Checks the return type and suppresses the checked exception for Supplier usage. 
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    protected <T> Stream<T> getStream(ProceedingJoinPoint joinPoint) {
        Object result = joinPoint.proceed();
        if (result instanceof Stream) {
            return (Stream<T>) result;
        }
        throw new ClassCastException("AsyncJPAStreaming aspect can only applied to repository methods with return type of java.util.Stream");
    }

    @Around("@annotation(AsyncJPAStreaming)")
    public <T> Stream<T> asycJPAStreaming(ProceedingJoinPoint joinPoint) throws Throwable {
        return streamAsync(() -> getStream(joinPoint));
    }
    
}