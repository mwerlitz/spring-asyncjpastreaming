package de.mw.spring.asyncjpastreaming;

import com.oath.cyclops.async.adapters.Queue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Async and transactional support for {@link AsyncJPAStreaming} aspect.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class AsyncJPAStreamingTransactionSupport {

    private static final int CLEAR_ENTITYMANAGER_INTERVAL = 1000;

    private final EntityManager entityManager;

    private ThreadPoolTaskExecutor executor;


    /**
     * Executes the given repository method asynchronously in a readonly transaction.
     * Uses an own threadPoolTaskExecutor.
     *
     * @param <T> entity type
     * @param queue communication bridge between calling and the async thread
     * @param repositorySupplier repository method returning a {@link Stream} of entities
     * @return CompletableFuture used to transport an exception if the async thread fails (e.g. when there is a connection timeout)
     */
    @Transactional(readOnly = true)
    @Async("asyncJPAStreamingTaskExecutor")
    public <T> CompletableFuture<Void> streamAsyncTransactionalReadonlyToQueue(Queue<T> queue, Supplier<Stream<T>> repositorySupplier, boolean clearEntityManager) {
        streamToQueue(queue, repositorySupplier, clearEntityManager);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Executes the given repository method asynchronously in a transaction.
     * Uses an own threadPoolTaskExecutor.
     *
     * @param <T> entity type
     * @param queue communication bridge between calling and the async thread
     * @param repositorySupplier repository method returning a {@link Stream} of entities
     * @return CompletableFuture used to transport an exception if the async thread fails (e.g. when there is a connection timeout)
     */
    @Transactional
    @Async("asyncJPAStreamingTaskExecutor")
    public <T> CompletableFuture<Void> streamAsyncTransactionalToQueue(Queue<T> queue, Supplier<Stream<T>> repositorySupplier, boolean clearEntityManager) {
        streamToQueue(queue, repositorySupplier, clearEntityManager);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Executes the given repository method synchronously in a readonly transaction.
     * This method effectively circumvents real streaming of data. Streaming is emulated via a temporary list.
     *
     * Data is stored temporary completely in the list to load all the data in the transaction.
     * It will behave like {@link TypedQuery#getResultList()}.
     *
     * @param <T> entity type
     * @param repositorySupplier repository method returning a {@link Stream} of entities
     */
    @Transactional(readOnly = true)
    public <T> Stream<T> streamTransactionalReadonly(Supplier<Stream<T>> repositorySupplier, boolean clearEntityManager) {
        return streamWithList(repositorySupplier, clearEntityManager);
    }

    /**
     * Executes the given repository method synchronously in a transaction.
     * This method effectively circumvents real streaming of data. Streaming is emulated via a temporary list.
     *
     * Data is stored temporary completely in the list to load all the data in the transaction.
     * It will behave like {@link TypedQuery#getResultList()}.
     *
     * @param <T> entity type
     * @param repositorySupplier repository method returning a {@link Stream} of entities
     */
    @Transactional
    public <T> Stream<T> streamTransactional(Supplier<Stream<T>> repositorySupplier, boolean clearEntityManager) {
        return streamWithList(repositorySupplier, clearEntityManager);
    }

    protected <T> void streamToQueue(Queue<T> queue, Supplier<Stream<T>> repositorySupplier, boolean clearEntityManager) {
        try (Stream<T> entityStream = repositorySupplier.get()) {
            AtomicInteger clearIntervalCounter = new AtomicInteger();

            log.trace("Streaming JPA results asynchronously...");
            entityStream.forEach(item -> {
                queue.offer(item);
                if (clearEntityManager) {
                    clearEntityManager(clearIntervalCounter);
                }
            });
        } catch (Queue.ClosedQueueException e) {
            // queue was closed in consuming thread, NOOP
        } catch (Exception e) {
            queue.addError(e);
            throw e;
        } finally {
            queue.close();
        }
    }

    protected <T> Stream<T> streamWithList(Supplier<Stream<T>> repositorySupplier, boolean clearEntityManager) {
        try (Stream<T> entityStream = repositorySupplier.get()) {
            AtomicInteger clearIntervalCounter = new AtomicInteger();

            log.trace("Fetching and streaming JPA results synchronously...");
            return entityStream.peek(item -> {
                                   if (clearEntityManager) {
                                       clearEntityManager(clearIntervalCounter);
                                   }
                                })
                               .toList()
                               .stream();
        }
    }

    private void clearEntityManager(AtomicInteger clearIntervalCounter) {
        // clear the entity manager to reduce memory consumption
        // when streaming a large set of managed entity objects
        //
        // otherwise Hibernate does keep ALL entity objects in the session
        // this can require a lot of memory even if the queue size is limited
        // and the entity is mapped to a business model object and not used anymore
        int itemCount = clearIntervalCounter.incrementAndGet();
        if (itemCount > CLEAR_ENTITYMANAGER_INTERVAL) { // do not clear after each item for performance reasons
            log.trace("Clearing EntityManager, next clearing after {} entities", CLEAR_ENTITYMANAGER_INTERVAL);
            entityManager.clear();
            clearIntervalCounter.set(0);
        }
    }

    /**
     * Produces an own specialized threadPoolTaskExecutor with a core/max pool size matching the max connection pool size of the datasource.
     */
    @Bean(name = "asyncJPAStreamingTaskExecutor")
    public Executor threadPoolTaskExecutor(@Qualifier("hikariPoolDataSourceMetadataProvider") DataSourcePoolMetadataProvider meta,
                                           @Value("${app.configuration.asyncjpastreaming.threads:}") Integer threads,
                                           DataSource dataSource,
                                           ThreadPoolTaskExecutorBuilder builder) {
        if (threads == null) {
            threads = meta.getDataSourcePoolMetadata(dataSource).getMax();
        }
        
        executor = builder.threadNamePrefix("asyncJPA-")
                          .corePoolSize(threads)
                          .maxPoolSize(threads)
                          .build();
        return executor;
    }
    
    public boolean isAsyncJPAStreaming() {
        return Objects.equals(executor.getThreadGroup(), Thread.currentThread().getThreadGroup());
    }

}
