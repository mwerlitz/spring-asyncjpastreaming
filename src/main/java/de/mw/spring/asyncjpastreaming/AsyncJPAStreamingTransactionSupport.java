package de.mw.spring.asyncjpastreaming;

import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.MDC;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.oath.cyclops.async.adapters.Queue;

/**
 * Async and transactional support for {@link AsyncJPAStreaming} aspect.
 */
@Component
class AsyncJPAStreamingTransactionSupport {
    
    /**
     * Executes the given repository method asyncronously in a readonly transaction.
     * Uses an own threadPoolTaskExecutor.
     * 
     * @param <T> entity type
     * @param queue communication bridge between calling and the async thread 
     * @param repositorySupplier repository method returning a {@link Stream} of entities
     */
    @Transactional(readOnly = true)
    @Async("asyncJPAStreamingTaskExecutor")
    public <T> void streamAsyncTransactionalReadonlyToQueue(Queue<T> queue, Supplier<Stream<T>> repositorySupplier) {
        streamToQueue(queue, repositorySupplier);
    }
    
    /**
     * Executes the given repository method asyncronously in a transaction.
     * Uses an own threadPoolTaskExecutor.
     * 
     * @param <T> entity type
     * @param queue communication bridge between calling and the async thread 
     * @param repositorySupplier repository method returning a {@link Stream} of entities
     */
    @Transactional
    @Async("asyncJPAStreamingTaskExecutor")
    public <T> void streamAsyncTransactionalToQueue(Queue<T> queue, Supplier<Stream<T>> repositorySupplier) {
        streamToQueue(queue, repositorySupplier);
    }
    
    protected <T> void streamToQueue(Queue<T> queue, Supplier<Stream<T>> repositorySupplier) {
        try (Stream<T> entityStream = repositorySupplier.get()) {
            queue.fromStream(entityStream);
        } catch (Exception e) {
            queue.addError(e);
            throw e;
        } finally {
            queue.close();
        }
    }

    /**
     * Produces a threadPoolTaskExecutor with a core/max pool size matching the max connection pool size of the datasource.
     * Also copies the {@link MDC} from the calling thread to the async thread.
     */
    @Bean(name = "asyncJPAStreamingTaskExecutor")
    public Executor threadPoolTaskExecutor(DataSourcePoolMetadataProvider meta, DataSource dataSource) {
        int threads = meta.getDataSourcePoolMetadata(dataSource).getMax();
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("asyncJPA-");
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setTaskDecorator(new MdcTaskDecorator());
        return executor;
    }
    
}
