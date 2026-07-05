package com.mycompany.tutorhub_enterprise.client.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleAiAgentServiceTest {

    @Test
    void streamsChatCompletionsFromOpenAiCompatibleEndpoint() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> requestBody = new AtomicReference<>("");
        AtomicReference<String> authorization = new AtomicReference<>("");
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(readRequest(exchange));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = ("data: {\"choices\":[{\"delta\":{\"content\":\"Xin \"}}]}\n\n"
                    + "data: {\"choices\":[{\"delta\":{\"content\":\"chao\"}}]}\n\n"
                    + "data: [DONE]\n\n").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            OpenAiCompatibleAiAgentService service =
                    new OpenAiCompatibleAiAgentService(baseUrl, "test-model", "test-key");
            CountDownLatch done = new CountDownLatch(1);
            StringBuilder output = new StringBuilder();
            AtomicReference<Exception> error = new AtomicReference<>();

            service.streamChat(AiAgentRequest.builder().message("hello").build(), new AiAgentStreamCallback() {
                @Override
                public void onDelta(String delta) {
                    output.append(delta);
                }

                @Override
                public void onComplete() {
                    done.countDown();
                }

                @Override
                public void onError(Exception ex) {
                    error.set(ex);
                    done.countDown();
                }
            });

            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertNull(error.get());
            assertEquals("Xin chao", output.toString());
            assertEquals("Bearer test-key", authorization.get());
            assertTrue(requestBody.get().contains("\"model\":\"test-model\""));
        } finally {
            server.stop(0);
        }
    }

    private static String readRequest(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
