package am.ik.lab.syaberu.rsocket;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static am.ik.lab.syaberu.scheduled._ScheduledCallArgumentsMeta.*;

@RestController
@CrossOrigin
public class RsocketReciver {
    private final RsocketHandler rsocketHandler;

    public RsocketReciver(RsocketHandler rsocketHandler) {
        this.rsocketHandler = rsocketHandler;
    }

    @ConnectMapping("subscribe.{subscriptionId}")
    public Mono<Void> subscribe(@DestinationVariable("subscriptionId") String subscriptionId,
                                RSocketRequester requester) {
        return this.rsocketHandler.subscribe(subscriptionId, requester);
    }

    @PostMapping(path = "proxy/{subscriptionId}")
    public Mono<?> proxy(@PathVariable("subscriptionId") String subscriptionId,
                         @RequestHeader(name = "X-Api-Key") String apiKey,
                         ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(form -> this.rsocketHandler
                        .send(subscriptionId,
                                form.getFirst(TEXT.name()),
                                form.getFirst(SPEAKER.name()),
                                form.getFirst(EMOTION.name()),
                                apiKey));
    }
}
