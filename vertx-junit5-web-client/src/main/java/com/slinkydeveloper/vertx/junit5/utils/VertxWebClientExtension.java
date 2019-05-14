package com.slinkydeveloper.vertx.junit5.utils;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class VertxWebClientExtension implements ParameterResolver {

  private static String WEB_CLIENT = "WebClient";
  private static String RX2_WEB_CLIENT = "Rx2WebClient";

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    Class<?> type = parameterContext.getParameter().getType();
    return type.equals(WebClient.class) || type.equals(io.vertx.reactivex.ext.web.client.WebClient.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    Class<?> type = parameterContext.getParameter().getType();
    ExtensionContext.Store store = extensionContext.getStore(ExtensionContext.Namespace.create(VertxWebClientExtension.class, extensionContext));
    if (WebClient.class.equals(type))
      return getWebClient(parameterContext, extensionContext, store);
    if (io.vertx.reactivex.ext.web.client.WebClient.class.equals(type))
      return getRx2WebClient(parameterContext, extensionContext, store);
    throw new IllegalStateException("Looks like the ParameterResolver needs a fix...");
  }

  private WebClient getWebClient(ParameterContext parameterContext, ExtensionContext extensionContext, ExtensionContext.Store myStore) {
    Vertx vertx = VertxExtension.retrieveVertx(parameterContext.getDeclaringExecutable(), extensionContext);
    return myStore.getOrComputeIfAbsent(WEB_CLIENT, s -> WebClient.create(vertx), WebClient.class);
  }

  private io.vertx.reactivex.ext.web.client.WebClient getRx2WebClient(ParameterContext parameterContext, ExtensionContext extensionContext, ExtensionContext.Store myStore) {
    io.vertx.reactivex.core.Vertx vertx = VertxExtension.retrieveRxJava2Vertx(parameterContext.getDeclaringExecutable(), extensionContext);
    return myStore.getOrComputeIfAbsent(RX2_WEB_CLIENT, s -> io.vertx.reactivex.ext.web.client.WebClient.create(vertx), io.vertx.reactivex.ext.web.client.WebClient.class);
  }
}
