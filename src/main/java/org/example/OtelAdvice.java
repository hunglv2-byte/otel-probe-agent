package org.example;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import net.bytebuddy.asm.Advice;

public class OtelAdvice {

    public static final Tracer tracer =
            GlobalOpenTelemetry.getTracer("org.example.auto");

    @Advice.OnMethodEnter
    static Span onEnter(@Advice.Origin("#t.#m") String methodName
    ) {
        System.err.println("[Agent] -> " + methodName);
        Span span = tracer.spanBuilder(methodName).startSpan();
        span.makeCurrent();
        return span;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    static void onExit(@Advice.Enter Span span, @Advice.Thrown Throwable t) {
        if (t != null) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR);
        }
        span.end();
    }

}
