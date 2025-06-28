package de.mw.spring.asyncjpastreaming;

import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.Query;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Executes the repository method in an {@link Async} readonly {@link Transactional} thread and returns a lazy stream of entities.
 * This annotation can only be used on methods returning a {@link Stream}, e.g. use {@link Query#getResultStream()}.
 * <p>
 * The stream of entities will be outside of the transaction. Make sure all data for further processing is loaded.
 * Wrapping the call inside a {@link Transactional} method will not help, as the processing is async.
 * <p>
 * <p>
 * If you want a non-readonly transaction or further processing of inside the transaction try to use
 * {@link AsyncJPAStreamingSupport#streamAsync(Supplier, boolean, boolean, int)} directly.
 * </p>
 * <p>
 * This way of fetching of entities is most usefull when processing a large amount of data,
 * e.g. for streaming mapped entities to a REST client. To make most of it use the stream up to the
 * serialization layer (Jackson). This way memory consumption and GC activity stays low even with many mapping steps inbetween
 * and latency is also low because writing out the response can start already after fetching and processing 
 * the first batch of result set entries.
 * </p>
 * <p>
 * Make sure you enable {@link Async} via {@link org.springframework.scheduling.annotation.EnableAsync}.
 * </p>
 * Be sure to set a clever JDBC fetch size for optimal performance with query hints for your underlying database, e.g.:
 * <pre>
 * @QueryHints(value = {
 *      @QueryHint(name = org.hibernate.jpa.AvailableHints.HINT_FETCH_SIZE, value = "1000"),
 *      @QueryHint(name = org.hibernate.jpa.AvailableHints.HINT_CACHEABLE, value = "false"),
 *      @QueryHint(name = org.hibernate.jpa.AvailableHints.HINT_READONLY, value = "true")
 * })
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncJPAStreaming {

    /**
     * Configure the buffer capacity of the underlying queue
     */
    int bufferCapacity() default Integer.MAX_VALUE;
    
    /**
     * Clear the entity manager after 1000 items while streaming, default false.
     * It is highly recommended to use it when streaming a large set of managed entities.
     */
    boolean clearEntityManager() default false;

}