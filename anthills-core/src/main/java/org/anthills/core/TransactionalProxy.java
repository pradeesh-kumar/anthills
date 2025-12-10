package org.anthills.core;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import org.anthills.core.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class TransactionalProxy {

  static <T> T create(T target, TransactionManager txManager) {
    try {
      Class<?> targetClass = target.getClass();
      Constructor<?> targetCtor = targetClass.getDeclaredConstructors()[0];
      targetCtor.setAccessible(true);
      Object[] args = Arrays.stream(targetCtor.getParameterTypes())
        .map(ProxyConstructorInterceptor::defaultValue)
        .toArray();

      Class<? extends T> proxyClass = new ByteBuddy()
        .subclass((Class<T>) targetClass)
        .name(targetClass.getSimpleName() + "Proxy")

        .defineConstructor(Visibility.PUBLIC)
        .intercept(MethodCall.invoke(targetCtor).with(args).andThen(MethodDelegation.to(ProxyConstructorInterceptor.class)))

        // --- Intercept Transactional Annotated Methods ---
        .method(ElementMatchers.isAnnotatedWith(Transactional.class))
        .intercept(MethodDelegation.to(new TransactionInterceptor<T>(target, txManager)))

        .make()
        .load(targetClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
        .getLoaded();

      T proxyInstance;
      try {
        Constructor<? extends T> constructor = proxyClass.getDeclaredConstructor();
        proxyInstance = constructor.newInstance();
      } catch (NoSuchMethodException e) {
        // This indicates a failure in ByteBuddy's constructor interception logic.
        throw new IllegalStateException("Proxy class failed to create an instantiable constructor.", e);
      }

      // 5. Copy the state (fields) from the original target to the new proxy instance
      copyFields(target, proxyInstance);
      return proxyInstance;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create transactional proxy", e);
    }
  }

  private static void copyFields(Object source, Object destination) throws IllegalAccessException {
    for (Class<?> clazz = source.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Field field : clazz.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          field.setAccessible(true);
          field.set(destination, field.get(source));
        }
      }
    }
  }

  public static class ProxyConstructorInterceptor {

    @RuntimeType
    public static void afterConstruction(@This Object proxy) {
      System.out.println("Proxy constructed safely: " + proxy.getClass());
    }

    private static Object defaultValue(Class<?> type) {
      if (!type.isPrimitive()) return null;

      if (type == boolean.class) return false;
      if (type == char.class) return '\0';
      if (type == byte.class) return (byte) 0;
      if (type == short.class) return (short) 0;
      if (type == int.class) return 0;
      if (type == long.class) return 0L;
      if (type == float.class) return 0f;
      if (type == double.class) return 0D;

      throw new IllegalArgumentException("Unsupported primitive: " + type);
    }
  }

  public static class TransactionInterceptor<T> {

    private final T target;
    private final TransactionManager txManager;

    TransactionInterceptor(T target, TransactionManager txManager) {
      this.target = target;
      this.txManager = txManager;
    }

    public Object intercept(@SuperCall Callable<Object> superCall, @Origin Method method, @AllArguments Object[] args) throws Throwable {
      Method originalMethod = target.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
      return txManager.execute(() -> originalMethod.invoke(args));
    }
  }
}
