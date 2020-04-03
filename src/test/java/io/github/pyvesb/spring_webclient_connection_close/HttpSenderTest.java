package io.github.pyvesb.spring_webclient_connection_close;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.netty.ConnectionObserver.State;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;

class HttpSenderTest {

    private static final int PORT = 4578;
    private static final String BASE_CDN_URL = "http://localhost:" + PORT;

    private MockWebServer cdnMock;

    @BeforeEach
    void setup() throws IOException {
        cdnMock = new MockWebServer();
        cdnMock.start(PORT);
        cdnMock.enqueue(new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @AfterEach
    void teardown() throws IOException {
        cdnMock.shutdown();
    }

    @Test
    void shouldTerminateWithError() {
        HttpSender underTest = new HttpSender(WebClient.create());

        byte[] bytes = "some data to be sent".getBytes();
        Flux<DataBuffer> data = DataBufferUtils.readInputStream(() -> new ByteArrayInputStream(bytes), new DefaultDataBufferFactory(), 8);
        StepVerifier.create(underTest.sendData(data, BASE_CDN_URL)).verifyError();
    }

    @Test
    void shouldTerminateWithErrorIfConnectionClosedInObserver() {
        TcpClient tcpClient = TcpClient.create()
                .observe((connection, newState) -> {
                    if (newState == State.RELEASED)
                        connection.dispose();
                });
        WebClient webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient))).build();
        HttpSender underTest = new HttpSender(webClient);

        byte[] bytes = "some data to be sent".getBytes();
        Flux<DataBuffer> data = DataBufferUtils.readInputStream(() -> new ByteArrayInputStream(bytes), new DefaultDataBufferFactory(), 8);
        StepVerifier.create(underTest.sendData(data, BASE_CDN_URL)).verifyError();
    }

    @Test
    @Timeout(value = 5)
    void shouldTerminateWithErrorIfConnectionClosedInDoAfterResponseSuccess() {
        HttpClient httpClient = HttpClient.create().doAfterResponseSuccess((r, c) -> c.dispose());
        WebClient webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
        HttpSender underTest = new HttpSender(webClient);

        byte[] bytes = "some data to be sent".getBytes();
        Flux<DataBuffer> data = DataBufferUtils.readInputStream(() -> new ByteArrayInputStream(bytes), new DefaultDataBufferFactory(), 8);
        StepVerifier.create(underTest.sendData(data, BASE_CDN_URL)).verifyError();
    }

}
