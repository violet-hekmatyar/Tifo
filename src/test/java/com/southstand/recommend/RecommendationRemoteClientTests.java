package com.southstand.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import com.southstand.recommend.client.RecommendationRemoteClient;
import com.southstand.recommend.config.RecommendationProperties;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RecommendationRemoteClientTests {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test
    void acceptsReadyBoundedCandidateResponse() throws Exception {
        serve(200, "{\"success\":true,\"modelReady\":true,\"modelVersion\":\"CF_V1_x\",\"items\":[{\"contentId\":1,\"cfScore\":0.75}]}");
        var result = client().contentScores(2L, List.of(1L));
        assertThat(result.success()).isTrue();
        assertThat(result.scores()).containsEntry(1L, 0.75);
    }

    @Test
    void rejectsNotReadyIllegalIdsScoresAndHttpErrors() throws Exception {
        serve(200, "{\"success\":true,\"modelReady\":false,\"modelVersion\":\"x\",\"items\":[{\"contentId\":1,\"cfScore\":0.5}]}");
        assertThat(client().contentScores(2L, List.of(1L)).success()).isFalse();
        stop(); serve(200, "{\"success\":true,\"modelReady\":true,\"modelVersion\":\"x\",\"items\":[{\"contentId\":99,\"cfScore\":2}]}");
        assertThat(client().contentScores(2L, List.of(1L)).success()).isFalse();
        stop(); serve(500, "failed");
        assertThat(client().contentScores(2L, List.of(1L)).success()).isFalse();
    }

    private void serve(int status, String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/internal/recommend/content-scores", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
    }

    private RecommendationRemoteClient client() {
        RecommendationProperties p = new RecommendationProperties();
        p.getCf().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        return new RecommendationRemoteClient(p);
    }
}
