package com.slinkydeveloper.vertx.junit5.utils;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import org.opentest4j.AssertionFailedError;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("unchecked")
public class TestRequest {

  WebClient client;
  HttpMethod method;
  String path;
  List<Consumer<HttpRequest<Buffer>>> requestTranformations;
  List<Consumer<HttpResponse<Buffer>>> responseAsserts;
  StackTraceElement[] stackTrace;

  public TestRequest(WebClient client, HttpMethod method, String path) {
    this.client = client;
    this.method = method;
    this.path = path;
    this.requestTranformations = new ArrayList<>();
    this.responseAsserts = new ArrayList<>();
    this.stackTrace = Thread.currentThread().getStackTrace();
  }

  public TestRequest transformations(Consumer<HttpRequest<Buffer>>... transformations) {
    requestTranformations.addAll(Arrays.asList(transformations));
    return this;
  }

  public TestRequest asserts(Consumer<HttpResponse<Buffer>>... asserts) {
    responseAsserts.addAll(Arrays.asList(asserts));
    return this;
  }

  public TestRequest send(VertxTestContext testContext, Checkpoint checkpoint) {
    return send(testContext, (VertxTestContext.ExecutionBlock) checkpoint::flag);
  }

  public TestRequest send(VertxTestContext testContext) {
    return send(testContext, (VertxTestContext.ExecutionBlock) testContext::completeNow);
  }

  public TestRequest send(VertxTestContext testContext, VertxTestContext.ExecutionBlock onEnd) {
    HttpRequest<Buffer> req = client.request(method, path);
    this.requestTranformations.forEach(c -> c.accept(req));
    req.send(generateHandleResponse(testContext, onEnd));
    return this;
  }

  public TestRequest sendJson(Object json, VertxTestContext testContext, Checkpoint checkpoint) {
    return sendJson(json, testContext, (VertxTestContext.ExecutionBlock) checkpoint::flag);
  }

  public TestRequest sendJson(Object json, VertxTestContext testContext) {
    return sendJson(json, testContext, (VertxTestContext.ExecutionBlock) testContext::completeNow);
  }

  public TestRequest sendJson(Object json, VertxTestContext testContext, VertxTestContext.ExecutionBlock onEnd) {
    HttpRequest<Buffer> req = client.request(method, path);
    this.requestTranformations.forEach(c -> c.accept(req));
    req.sendJson(json, generateHandleResponse(testContext, onEnd));
    return this;
  }

  public TestRequest sendBuffer(Buffer buf, VertxTestContext testContext, Checkpoint checkpoint) {
    return sendBuffer(buf, testContext, (VertxTestContext.ExecutionBlock) checkpoint::flag);
  }

  public TestRequest sendBuffer(Buffer buf, VertxTestContext testContext) {
    return sendBuffer(buf, testContext, (VertxTestContext.ExecutionBlock) testContext::completeNow);
  }

  public TestRequest sendBuffer(Buffer buf, VertxTestContext testContext, VertxTestContext.ExecutionBlock onEnd) {
    HttpRequest<Buffer> req = client.request(method, path);
    this.requestTranformations.forEach(c -> c.accept(req));
    req.sendBuffer(buf, generateHandleResponse(testContext, onEnd));
    return this;
  }

  public TestRequest sendURLEncodedForm(MultiMap form, VertxTestContext testContext, Checkpoint checkpoint) {
    return sendURLEncodedForm(form, testContext, (VertxTestContext.ExecutionBlock) checkpoint::flag);
  }

  public TestRequest sendURLEncodedForm(MultiMap form, VertxTestContext testContext) {
    return sendURLEncodedForm(form, testContext, (VertxTestContext.ExecutionBlock) testContext::completeNow);
  }

  public TestRequest sendURLEncodedForm(MultiMap form, VertxTestContext testContext, VertxTestContext.ExecutionBlock onEnd) {
    HttpRequest<Buffer> req = client.request(method, path);
    this.requestTranformations.forEach(c -> c.accept(req));
    req.sendForm(form, generateHandleResponse(testContext, onEnd));
    return this;
  }

  public TestRequest sendMultipartForm(MultipartForm form, VertxTestContext testContext, Checkpoint checkpoint) {
    return sendMultipartForm(form, testContext, (VertxTestContext.ExecutionBlock) checkpoint::flag);
  }

  public TestRequest sendMultipartForm(MultipartForm form, VertxTestContext testContext) {
    return sendMultipartForm(form, testContext, (VertxTestContext.ExecutionBlock) testContext::completeNow);
  }

  public TestRequest sendMultipartForm(MultipartForm form, VertxTestContext testContext, VertxTestContext.ExecutionBlock onEnd) {
    HttpRequest<Buffer> req = client.request(method, path);
    this.requestTranformations.forEach(c -> c.accept(req));
    req.sendMultipartForm(form, generateHandleResponse(testContext, onEnd));
    return this;
  }

  private Handler<AsyncResult<HttpResponse<Buffer>>> generateHandleResponse(VertxTestContext testContext, VertxTestContext.ExecutionBlock onEnd) {
    return ar -> {
      if (ar.failed()) testContext.failNow(ar.cause());
      else {
        testContext.verify(() -> {
          try {
            this.responseAsserts.forEach(c -> c.accept(ar.result()));
          } catch (AssertionFailedError e) {
            e.setStackTrace(stackTrace);
            throw e;
          }
          onEnd.apply();
        });
      }
    };
  }

  public static TestRequest testRequest(WebClient client, HttpMethod method, String path) {
    return new TestRequest(client, method, path);
  }

  public static TestRequest testRequest(WebClient client, HttpMethod method) {
    return new TestRequest(client, method, "/test");
  }

  public static Consumer<HttpResponse<Buffer>> statusCode(int statusCode) {
    return res -> assertEquals(statusCode, res.statusCode());
  }

  public static Consumer<HttpResponse<Buffer>> statusMessage(String statusMessage) {
    return res -> assertEquals(statusMessage, res.statusMessage());
  }

  public static Consumer<HttpRequest<Buffer>> header(String key, String value) {
    return req -> req.putHeader(key, value);
  }

  public static Consumer<HttpRequest<Buffer>> cookie(QueryStringEncoder encoder) {
    return req -> {
      try {
        String rawQuery = encoder.toUri().getRawQuery();
        if (rawQuery != null && !rawQuery.isEmpty())
          req.putHeader("cookie", encoder.toUri().getRawQuery());
      } catch (URISyntaxException e) {
        e.printStackTrace();
      }
    };
  }

  public static Consumer<HttpResponse<Buffer>> jsonBodyResponse(Object expected) {
    return res -> {
      assertEquals("application/json", res.getHeader("content-type"));
      Object json = Json.decodeValue(res.bodyAsBuffer());
      assertEquals(expected, json);
    };
  }

  public static Consumer<HttpResponse<Buffer>> bodyResponse(Buffer expected, String expectedContentType) {
    return res -> {
      assertEquals(expectedContentType, res.getHeader("content-type"));
      assertEquals(expected, res.bodyAsBuffer());
    };
  }

  public static Consumer<HttpResponse<Buffer>> headerResponse(String headerName, String headerValue) {
    return res -> {
      assertEquals(headerValue, res.getHeader(headerName));
    };
  }

  public static String urlEncode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static Consumer<HttpResponse<Buffer>> emptyResponse() {
    return res -> {
      assertNull(res.body());
    };
  }

}
