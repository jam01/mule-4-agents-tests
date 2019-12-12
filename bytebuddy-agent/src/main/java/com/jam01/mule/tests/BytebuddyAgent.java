package com.jam01.mule.tests;

//import javax.annotation.Nullable;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

//import org.mule.runtime.module.artifact.api.classloader.DelegateOnlyLookupStrategy;
//import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;

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
//                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
//                .type(hasSuperType(named("org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader")))
//                .transform(new Transformer() {
//                    public DynamicType.Builder<?> transform(final DynamicType.Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
//                        return builder.visit(Advice.to(MuleCLOnExit.class).on(isConstructor()));
//                    }
//                })
//                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
//                .type(not(isInterface()).and(hasSuperType(named("org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy"))))
//                .transform(new Transformer() {
//                    public DynamicType.Builder<?> transform(final DynamicType.Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
//                        return builder.visit(Advice.to(Mule4OverrideClassLoaderLookupAdvice.class).on(named("getPackageLookupStrategy").and(takesArgument(0, String.class))));
//                    }
//                })
                .installOn(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
    }

    public static class AHCBuilderExit {
        @Advice.OnMethodExit
        public static void exit(final @Advice.Origin String origin, final @Advice.This Object thiz) {
            System.out.println("from bytebuddy: com.ning.http.client.AsyncHttpClientConfig$Builder.<init>");
        }
    }

    public static class Mule4OverrideClassLoaderLookupAdvice {
        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
        public static void makeParentOnlyForAgentClasses(@Advice.Argument(0) final String packageName) {
            System.out.println("from bytebuddy: org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy.getPackageLookupStrategy");
            System.out.println(packageName);
        }
    }

    public static class MuleCLOnExit {
        @Advice.OnMethodExit
        public static void exit(final @Advice.Origin String origin, final @Advice.This Object thiz) {
            System.out.println("from bytebuddy: org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader.<init>");
            System.out.println(thiz.getClass().getSimpleName());
        }
    }
}
