package com.jam01.mule.tests;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isDefaultConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class BytebuddyAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("from bytebuddy: agent premain");

        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .type(hasSuperType(named("com.ning.http.client.AsyncHttpClientConfig$Builder")))
                .transform(new Transformer() {
                    public DynamicType.Builder<?> transform(final DynamicType.Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
                        return builder.visit(Advice.to(AHCBuilderExit.class).on(isDefaultConstructor()));
                    }
                })
                .installOn(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
    }

    public static class AHCBuilderExit {
        @Advice.OnMethodExit
        public static void exit(final @Advice.Origin String origin, final @Advice.This Object thiz) {
            System.out.println("from byteman: AsyncHttpClientConfig.Builder.<init> triggered");
        }
    }
}
