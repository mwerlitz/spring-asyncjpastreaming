package de.mw.spring.asyncjpastreaming;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Aspect implemenation for {@link AsyncJPAStreaming}
 */
@Aspect
@Component
@RequiredArgsConstructor
class AsyncJPAStreamingAspect {

    @Autowired
    private final AsyncJPAStreamingSupport streamingSupport;


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
        return streamingSupport.streamAsyncReadonly(() -> getStream(joinPoint));
    }

}