package org.example;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import net.bytebuddy.asm.Advice;

public class OtelAdvice {

    public static final Tracer tracer =
            GlobalOpenTelemetry.getTracer("otel.probe.agent");

    //creates span + makes it current → returns Scope.
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Scope onEnter(@Advice.Origin("#t.#m") String methodName) {
        Span parent = Span.current();
        if (!parent.getSpanContext().isValid()) {
            System.err.println("[No active trace] -> " + methodName);
            return null;
        }

        Span span = OtelAdvice.tracer.spanBuilder(methodName)
                .setParent(Context.current())
                .startSpan();

        System.err.println("[Agent] -> " + methodName +
                " traceId=" + span.getSpanContext().getTraceId());

        return span.makeCurrent(); // return only Scope
    }

    //closes Scope (restoring HTTP parent) + ends span.
    //No leaks, no duplicates, spans always attach to the right parent.
    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(@Advice.Enter Scope scope,
                              @Advice.Thrown Throwable throwable) {
        if (scope == null) return;

        try {
            Span span = Span.current(); // get the active span
            if (throwable != null) {
                span.recordException(throwable);
                span.setStatus(StatusCode.ERROR);
            }
            span.end(); // end the span
        } finally {
            scope.close(); // restore parent context
        }
    }

}
