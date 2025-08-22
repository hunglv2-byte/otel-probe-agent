package org.example;


import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static ElementMatcher.Junction<? super TypeDescription> buildTypeMatcher() {
        // 1. Include matcher
        String includeProp = System.getProperty("otel.probe.agent.packages");
        ElementMatcher.Junction<? super TypeDescription> includeMatcher = ElementMatchers.none();

        if (includeProp != null && !includeProp.isEmpty()) {
            for (String pkg : includeProp.split(",")) {
                String trimmed = pkg.trim();
                if (!trimmed.isEmpty()) {
                    System.out.println("[OtelAgent] Including: " + trimmed);
                    includeMatcher = includeMatcher.or(ElementMatchers.nameStartsWith(trimmed));
                }
            }
        }

        // 2. Exclude matcher (sub-packages inside include)
        String ignoreProp = System.getProperty("otel.probe.agent.packages.ignore");
        ElementMatcher.Junction<? super TypeDescription> excludeMatcher = ElementMatchers.none();

        if (ignoreProp != null && !ignoreProp.isEmpty()) {
            for (String pkg : ignoreProp.split(",")) {
                String trimmed = pkg.trim();
                if (!trimmed.isEmpty()) {
                    System.out.println("[OtelAgent] Ignoring: " + trimmed);
                    excludeMatcher = excludeMatcher.or(ElementMatchers.nameStartsWith(trimmed));
                }
            }
        }

        // 3. Final matcher: include minus exclude
        return includeMatcher.and(ElementMatchers.not(excludeMatcher));
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            System.out.println("[OtelAgent] Starting instrumentation...");
            AgentBuilder agentBuilder = new AgentBuilder.Default();

            agentBuilder
                    .ignore(ElementMatchers.isSynthetic()) //Skip all synthetic / lambda classes
                    .type(buildTypeMatcher()) // scan packages or classes
                    .transform(new AgentBuilder.Transformer() {
                        @Override
                        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                                TypeDescription typeDescription,
                                                                ClassLoader classLoader,
                                                                JavaModule javaModule, ProtectionDomain protectionDomain) {
                            return builder.visit(Advice.to(OtelAdvice.class)
                                    .on(ElementMatchers.isMethod()
                                            .and(ElementMatchers.not(ElementMatchers.isConstructor()))
                                            .and(ElementMatchers.not(ElementMatchers.isTypeInitializer())) // skips <clinit>
                                            .and(ElementMatchers.not(ElementMatchers.namedIgnoreCase("construct")))
                                            .and(ElementMatchers.not(ElementMatchers.isGetter()))
                                            .and(ElementMatchers.not(ElementMatchers.isSetter()))
                                            .and(ElementMatchers.not(ElementMatchers.named("toResponse")))
                                            .and(ElementMatchers.not(ElementMatchers.nameMatches("^(invoke|invokeSuspend|emit)$")))
                                            .and(ElementMatchers.not(ElementMatchers.nameContains("$$"))) // skips many coroutine internals
                                            .and(ElementMatchers.not(ElementMatchers.isSynthetic()))
                                            .and(ElementMatchers.not(ElementMatchers.isDeclaredBy(ElementMatchers.isEnum())))
                                    ));
                        }

                    })
                    .installOn(inst);
        } catch (Throwable t) {
            t.printStackTrace(); // force log the error
        }

    }
}