package com.jam01.mule.tests;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.InitializationStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.agent.builder.AgentBuilder.TypeStrategy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.utility.JavaModule;

public class ByteBuddyAgent {
  private static void log(final String message, final Throwable t) {
    System.err.println(message);
    if (t != null && t instanceof IllegalStateException && t.getMessage().startsWith("Cannot resolve type description for "))
      t.printStackTrace();
  }

  private static void log(final String message) {
    log(message, null);
  }

  private static String getNameId(final Object obj) {
    return obj != null ? obj.getClass().getName() + "@" + Integer.toString(System.identityHashCode(obj), 16) : "null";
  }

  public static void premain(final String agentArgs, final Instrumentation inst) {
    System.out.println("from bytebuddy: agent premain " + ByteBuddyAgent.class.getClassLoader());
    new AgentBuilder.Default(new ByteBuddy().with(TypeValidation.DISABLED))
      .disableClassFormatChanges()
      .ignore(none())
      .with(RedefinitionStrategy.RETRANSFORMATION)
      .with(InitializationStrategy.NoOp.INSTANCE)
      .with(TypeStrategy.Default.REDEFINE)
      .with(new AgentBuilder.Listener() {
        public void onDiscovery(final String typeName, final ClassLoader classLoader, final JavaModule module, final boolean loaded) {
          log("Event::onDiscovery(" + typeName + ", " + getNameId(classLoader) + ", " + module + ", " + loaded + ")");
        }

        public void onTransformation(final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module, final boolean loaded, final DynamicType dynamicType) {
          log("Event::onTransformation(" + typeDescription.getName() + ", " + getNameId(classLoader) + ", " + module + ", " + loaded + ", " + dynamicType + ")");
        }

        public void onIgnored(final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module, final boolean loaded) {
          log("Event::onIgnored(" + typeDescription.getName() + ", " + getNameId(classLoader) + ", " + module + ", " + loaded + ")");
        }

        public void onError(final String typeName, final ClassLoader classLoader, final JavaModule module, final boolean loaded, final Throwable throwable) {
          log("Event::onError(" + typeName + ", " + getNameId(classLoader) + ", " + module + ", " + loaded + ")", throwable);
        }

        public void onComplete(final String typeName, final ClassLoader classLoader, final JavaModule module, final boolean loaded) {
          log("Event::onComplete(" + typeName + ", " + getNameId(classLoader) + ", " + module + ", " + loaded + ")");
        }
      })
      .type(hasSuperType(named("com.ning.http.client.AsyncHttpClientConfig$Builder")))
      .transform(new Transformer() {
        public DynamicType.Builder<?> transform(final DynamicType.Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder.visit(Advice.to(AHCBuilderExit.class).on(isDefaultConstructor()));
        }})
      .installOn(inst);
  }

  public static final CachedClassFileLocator cachedLocator;

  static {
    try {
      cachedLocator = new CachedClassFileLocator(ClassFileLocator.ForClassLoader.ofSystemLoader(), String.class);
    }
    catch (final IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static class CachedClassFileLocator implements ClassFileLocator {
    private final Map<String,Resolution> map = new HashMap<String,Resolution>();

    public CachedClassFileLocator(final ClassFileLocator classFileLocator, final Class<?> ... classes) throws IOException {
      if (classes == null)
        return;

      for (int i = 0; i < classes.length; ++i) {
        final Class<?> cls = classes[i];
        Resolution resolution = classFileLocator.locate(cls.getName());
        if (resolution instanceof ClassFileLocator.Resolution.Illegal)
          resolution = ClassFileLocator.ForClassLoader.of(cls.getClassLoader()).locate(cls.getName());

        map.put(cls.getName(), resolution);
      }
    }

    public void close() throws IOException {
    }

    public Resolution locate(final String name) throws IOException {
      return map.get(name);
    }
  }

  public static class AHCBuilderExit {
    @Advice.OnMethodExit
    public static void exit(final @Advice.Origin String origin, final @Advice.This Object thiz) {
      System.out.println("from byteman: AsyncHttpClientConfig$Builder.<init> triggered");
    }
  }
}