package de.mw.spring.asyncjpastreaming;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * TaskDecorator to copy MDC data from the calling thread context onto the asynchronous threads’ context.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Right now: Web thread context !
        // (Grab the current thread MDC data)
        Map<String, String> contextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(Collections.emptyMap());
        return () -> {
            try {
                // Right now: @Async thread context !
                // (Restore the Web thread context's MDC data)
                MDC.setContextMap(contextMap);
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }

}