package com.ranger.governance.common.protocol;

import com.ranger.governance.common.model.ActionType;
import com.ranger.governance.common.model.DecisionRequest;
import com.ranger.governance.common.model.DecisionResponse;
import com.ranger.governance.common.model.EngineType;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class GovernanceClientHttpImplTest {

    @Test
    void shouldPostDecideAndParseResponse() throws Exception {
        TestHttpServer testHttpServer = startServer(new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String responseBody = "{\"code\":200,\"traceId\":\"trace-1\",\"data\":{\"actionType\":\"CHECK\",\"msg\":\"ok\",\"alertTriggered\":false,\"queryId\":\"q1\"}}";
                respond(exchange, 200, responseBody);
            }
        }, okHandler());
        try {
            GovernanceClientHttpImpl client = new GovernanceClientHttpImpl(configFor(testHttpServer.baseUrl(), 200, 500));
            DecisionResponse response = client.decide(sampleRequest("q1"));

            Assertions.assertTrue(response.success());
            Assertions.assertEquals("trace-1", response.getTraceId());
            Assertions.assertEquals(ActionType.CHECK, response.getData().getActionType());
            Assertions.assertEquals("q1", response.getData().getQueryId());
        } finally {
            testHttpServer.close();
        }
    }

    @Test
    void shouldPostMsgFailPayload() throws Exception {
        final AtomicReference<String> requestBodyRef = new AtomicReference<String>();
        TestHttpServer testHttpServer = startServer(okHandler(), new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                requestBodyRef.set(readBody(exchange.getRequestBody()));
                respond(exchange, 200, "");
            }
        });
        try {
            GovernanceClientHttpImpl client = new GovernanceClientHttpImpl(configFor(testHttpServer.baseUrl(), 200, 500));
            client.msgFail(sampleRequest("q-msg"), "ranger deny");

            String msgFailBody = requestBodyRef.get();
            Assertions.assertNotNull(msgFailBody);
            Assertions.assertTrue(msgFailBody.contains("\"rangerError\":\"ranger deny\""));
            Assertions.assertTrue(msgFailBody.contains("\"queryId\":\"q-msg\""));
        } finally {
            testHttpServer.close();
        }
    }

    @Test
    void shouldTimeoutFastWhenDecideIsSlow() throws Exception {
        TestHttpServer testHttpServer = startServer(new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    Thread.sleep(400L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
                respond(exchange, 200, "{\"code\":200,\"traceId\":\"slow\",\"data\":{\"actionType\":\"BYPASS\",\"msg\":\"\",\"alertTriggered\":false,\"queryId\":\"q\"}}");
            }
        }, okHandler());
        try {
            GovernanceClientHttpImpl client = new GovernanceClientHttpImpl(configFor(testHttpServer.baseUrl(), 200, 100));
            long start = System.nanoTime();
            Assertions.assertThrows(AccessControlException.class, () -> client.decide(sampleRequest("q-timeout")));
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            Assertions.assertTrue(elapsedMs < 3000L);
        } finally {
            testHttpServer.close();
        }
    }

    @Test
    void shouldSupportConcurrentDecideWithSingleClient() throws Exception {
        final AtomicInteger decideCounter = new AtomicInteger();
        TestHttpServer testHttpServer = startServer(new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                int index = decideCounter.incrementAndGet();
                String responseBody = "{\"code\":200,\"traceId\":\"trace-" + index + "\",\"data\":{\"actionType\":\"BYPASS\",\"msg\":\"\",\"alertTriggered\":false,\"queryId\":\"q\"}}";
                respond(exchange, 200, responseBody);
            }
        }, okHandler());
        try {
            final GovernanceClientHttpImpl client = new GovernanceClientHttpImpl(
                    new GovernanceClientHttpImpl.ClientConfig(
                            testHttpServer.baseUrl(),
                            "/api/v1/governance/decision",
                            "/api/v1/governance/msg-fail",
                            200,
                            800,
                            200,
                            128,
                            64,
                            30_000L
                    )
            );

            ExecutorService pool = Executors.newFixedThreadPool(20);
            List<Future<DecisionResponse>> futures = new ArrayList<Future<DecisionResponse>>();
            try {
                for (int i = 0; i < 60; i++) {
                    futures.add(pool.submit(new Callable<DecisionResponse>() {
                        @Override
                        public DecisionResponse call() {
                            return client.decide(sampleRequest("q-concurrent"));
                        }
                    }));
                }
                for (Future<DecisionResponse> future : futures) {
                    DecisionResponse response = future.get();
                    Assertions.assertTrue(response.success());
                    Assertions.assertEquals(ActionType.BYPASS, response.getData().getActionType());
                }
            } finally {
                pool.shutdownNow();
            }
            Assertions.assertEquals(60, decideCounter.get());
        } finally {
            testHttpServer.close();
        }
    }

    private GovernanceClientHttpImpl.ClientConfig configFor(String baseUrl, int connectTimeoutMs, int socketTimeoutMs) {
        return new GovernanceClientHttpImpl.ClientConfig(
                baseUrl,
                "/api/v1/governance/decision",
                "/api/v1/governance/msg-fail",
                connectTimeoutMs,
                socketTimeoutMs,
                100,
                256,
                128,
                30_000L
        );
    }

    private DecisionRequest sampleRequest(String queryId) {
        return new DecisionRequest("etl_finance_daily_01", "zhangsan", EngineType.HIVE_TEZ, "select 1", "127.0.0.1", queryId);
    }

    private HttpHandler okHandler() {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                respond(exchange, 200, "");
            }
        };
    }

    private TestHttpServer startServer(HttpHandler decideHandler, HttpHandler msgFailHandler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/governance/decision", decideHandler);
        server.createContext("/api/v1/governance/msg-fail", msgFailHandler);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        return new TestHttpServer(server);
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json;charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String readBody(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, read);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private static class TestHttpServer {
        private final HttpServer server;

        private TestHttpServer(HttpServer server) {
            this.server = server;
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void close() {
            server.stop(0);
        }
    }
}
