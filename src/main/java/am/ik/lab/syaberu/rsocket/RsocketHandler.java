package am.ik.lab.syaberu.rsocket;

import am.ik.lab.syaberu.encypt.ApiKeyEncryptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UncheckedIOException;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class RsocketHandler {
    private final Logger log = LoggerFactory.getLogger(RsocketHandler.class);
    private final ApiKeyEncryptor apiKeyEncryptor;
    private final ConcurrentHashMap<String, List<RSocketRequester>> requestersMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RsocketHandler(ApiKeyEncryptor apiKeyEncryptor, ObjectMapper objectMapper) {
        this.apiKeyEncryptor = apiKeyEncryptor;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> subscribe(String subscriptionId,
                                RSocketRequester requester) {
        final List<RSocketRequester> requesters = this.requestersMap.computeIfAbsent(subscriptionId, k -> new ArrayList<>());
        requesters.add(requester);
        log.info("Connected {}", subscriptionId);
        return Mono.empty();
    }

    public Mono<?> send(String subscriptionId, String text, String speaker, String emotion, String apiKey) {
        final List<RSocketRequester> requesters = this.requestersMap.getOrDefault(subscriptionId, Collections.emptyList());
        log.info("Number of subscriber for {}: {}", subscriptionId, requesters.size());
        final Map<String, String> data = new LinkedHashMap<>() {
            {
                put("apiKey", RsocketHandler.this.apiKeyEncryptor.encrypt(apiKey));
                put("text", text);
                put("speaker", speaker);
                if (emotion != null) {
                    put("emotion", emotion);
                }
            }
        };
        try {
            final String json = this.objectMapper.writeValueAsString(data);
            return Flux.fromIterable(new ArrayList<>(requesters) /* shallow copy */)
                    .flatMap(requester -> requester.route("syaberu")
                            .data(json)
                            .retrieveMono(String.class)
                            .onErrorResume(throwable -> {
                                if (throwable instanceof ClosedChannelException) {
                                    requesters.remove(requester);
                                }
                                return Mono.just("Closed");
                            }))
                    .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            return Mono.error(new UncheckedIOException(e));
        }
    }
}
