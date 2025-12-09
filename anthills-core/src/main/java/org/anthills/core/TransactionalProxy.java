package org.anthills.core;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.anthills.core.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Callable;

public class TransactionalProxy {

  static <T> T create(T target, TransactionManager txManager) {
    try {
      Class<?> targetClass = target.getClass();

      // 1. Define the dynamic subclass
      Class<? extends T> proxyClass = new ByteBuddy()
        .subclass((Class<T>) targetClass)
        .name(targetClass.getSimpleName() + "Proxy")
        // --- Interception Setup ---
        .method(ElementMatchers.isAnnotatedWith(Transactional.class))
        .intercept(MethodDelegation.to(new TransactionInterceptor<T>(target, txManager)))

        .make()
        .load(targetClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
        .getLoaded();

      // 3. Instantiate the proxy using the dynamically generated no-args constructor
      T proxyInstance;
      try {
        // Since we intercepted all constructors, we can now use the no-args constructor
        // on the generated proxy class.
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
    // Traverse the class hierarchy to ensure all inherited private fields are copied
    for (Class<?> clazz = source.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
      for (Field field : clazz.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          field.setAccessible(true);
          field.set(destination, field.get(source));
        }
      }
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
