package de.mw.spring.asyncjpastreaming;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Aspect implementation for {@link AsyncJPAStreaming}
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
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (!signature.getReturnType().isAssignableFrom(Stream.class)) {
            throw new ClassCastException("AsyncJPAStreaming aspect can only applied to repository methods with return type of java.util.Stream");
        }
        return (Stream<T>) joinPoint.proceed();
    }

    @Around("@annotation(AsyncJPAStreaming)")
    public <T> Stream<T> asyncJPAStreaming(ProceedingJoinPoint joinPoint) throws Throwable {
        return streamingSupport.streamAsyncReadonly(() -> getStream(joinPoint));
    }

}