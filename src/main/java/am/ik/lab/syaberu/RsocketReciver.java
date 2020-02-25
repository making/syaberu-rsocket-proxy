package am.ik.lab.syaberu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UncheckedIOException;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
public class RsocketReciver {
    private final Logger log = LoggerFactory.getLogger(RsocketReciver.class);
    private final ConcurrentHashMap<String, List<RSocketRequester>> requestersMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RsocketReciver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ConnectMapping("subscribe.{subscriptionId}")
    public Mono<Void> subscribe(@DestinationVariable("subscriptionId") String subscriptionId,
                                RSocketRequester requester) {
        final List<RSocketRequester> requesters = this.requestersMap.computeIfAbsent(subscriptionId, k -> new ArrayList<>());
        requesters.add(requester);
        log.info("Connected {}", subscriptionId);
        return Mono.empty();
    }

    @PostMapping(path = "proxy/{subscriptionId}")
    public Mono<?> proxy(@PathVariable("subscriptionId") String subscriptionId,
                         @RequestHeader(name = "X-Api-Key") String apiKey,
                         ServerWebExchange exchange) {
        final List<RSocketRequester> requesters = this.requestersMap.getOrDefault(subscriptionId, Collections.emptyList());
        log.info("Number of subscriber for {}: {}", subscriptionId, requesters.size());
        return exchange.getFormData()
                .map(form -> new LinkedHashMap<String, String>() {
                    {
                        put("apiKey", apiKey);
                        put("text", form.getFirst("text"));
                        put("speaker", form.getFirst("speaker"));
                        put("emotion", form.getFirst("emotion"));
                    }
                })
                .map(data -> {
                    try {
                        return this.objectMapper.writeValueAsString(data);
                    } catch (JsonProcessingException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .flatMap(data -> Flux.fromIterable(new ArrayList<>(requesters) /* shallow copy */)
                        .flatMap(requester -> requester.route("syaberu")
                                .data(data)
                                .retrieveMono(String.class)
                                .onErrorResume(throwable -> {
                                    if (throwable instanceof ClosedChannelException) {
                                        requesters.remove(requester);
                                    }
                                    return Mono.just("Closed");
                                }))
                        .collect(Collectors.toList()));
    }
}
