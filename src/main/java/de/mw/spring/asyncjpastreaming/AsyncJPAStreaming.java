package de.mw.spring.asyncjpastreaming;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;

/**
 * Executes the repository method in an async readonly transactional thread and returns a lazy stream of entities. 
 * This annotation can only be used on methods returning a {@link Stream}.
 * 
 * This way of fetching of entities is most usefull when processing a large ammount of data,
 * e.g. for streaming mapped entities to a REST client. To make most of it use the stream up to the
 * serialization layer (Jackson). This way memory consumtion and GC activity stays low even with many mapping steps inbetween
 * and latency is also low because writing out the response can start already after fetching and processing 
 * the first batch of result set entries.
 * 
 * Be sure to set a clever JDBC fetch size for optimal performance with query hints, e.g.:
 * @QueryHints(value = {
 *      @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "1000"),
 *      @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_CACHEABLE, value = "false"),
 *      @QueryHint(name = org.hibernate.jpa.QueryHints.HINT_READONLY, value = "true")
 * })
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncJPAStreaming {

}