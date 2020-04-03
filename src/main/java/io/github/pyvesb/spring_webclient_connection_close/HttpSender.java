package io.github.pyvesb.spring_webclient_connection_close;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class HttpSender {

    private final WebClient webClient;

    public HttpSender(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<ResponseEntity<Void>> sendData(Flux<DataBuffer> data, String uri) {
        return webClient.method(HttpMethod.POST)
                .uri(uri)
                .body(BodyInserters.fromDataBuffers(data))
                .retrieve()
                .toBodilessEntity();
    }
}
