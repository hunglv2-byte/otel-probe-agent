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

    static ElementMatcher.Junction<TypeDescription> multiplePackage(){
        String[] packages = System.getProperty("otel.probe.agent.packages", "all").split(",");

        ElementMatcher.Junction<TypeDescription> matcher;

        if (packages.length == 1 && packages[0].trim().equalsIgnoreCase("all")) {
            // No filtering — instrument everything
            matcher = ElementMatchers.any();
            System.out.println("[OtelAgent] Instrumenting ALL classes");
        } else {
            matcher = ElementMatchers.none();
            for (String pkg : packages) {
                pkg = pkg.trim();
                if (!pkg.isEmpty()) {
                    matcher = matcher.or(ElementMatchers.nameStartsWith(pkg));
                }
            }
            System.out.println("[OtelAgent] Instrumenting package prefixes: " + String.join(", ", packages));
        }

        return matcher;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            System.out.println("[OtelAgent] Starting instrumentation...");

            new AgentBuilder.Default()
                    .ignore(ElementMatchers.nameStartsWith("net.bytebuddy."))
                    .ignore(ElementMatchers.nameStartsWith("io.opentelemetry."))
                    .ignore(ElementMatchers.nameStartsWith("org.example.OtelAdvice"))
                    .ignore(ElementMatchers.nameStartsWith("org.slf4j.")) // prevent logging loops
                    .ignore(ElementMatchers.nameStartsWith("jp.joinsure.core.domain")) // <— skip core.domain
                    .ignore(ElementMatchers.nameStartsWith("jp.joinsure.core.port.adapter.config"))
                    .ignore(ElementMatchers.nameStartsWith("jp.joinsure.core.infra"))
                    .ignore(ElementMatchers.isSynthetic()) //Skip all synthetic / lambda classes
                    .type(multiplePackage()) // scan packages or classes
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