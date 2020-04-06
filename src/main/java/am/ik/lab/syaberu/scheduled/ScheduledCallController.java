package am.ik.lab.syaberu.scheduled;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "scheduled_calls")
@CrossOrigin
public class ScheduledCallController {
    private final ScheduledCallRepository scheduledCallRepository;
    private final Validator<JsonNode> validator = ValidatorBuilder.<JsonNode>of()
            ._object(x -> x.get(_ScheduledCallParameters.SubscriptionId.LOWER_CAMEL), _ScheduledCallParameters.SubscriptionId.LOWER_CAMEL, c -> c.notNull().message("\"" + _ScheduledCallParameters.SubscriptionId.LOWER_CAMEL + "\" is required."))
            ._boolean(x -> !x.has(_ScheduledCallParameters.SubscriptionId.LOWER_CAMEL) || x.get(_ScheduledCallParameters.SubscriptionId.LOWER_CAMEL).isTextual(), _ScheduledCallParameters.SubscriptionId.LOWER_CAMEL, c -> c.isTrue().message("\"" + _ScheduledCallParameters.SubscriptionId.LOWER_CAMEL + "\" must be textual."))
            ._object(x -> x.get(_ScheduledCallParameters.Text.LOWER_CAMEL), _ScheduledCallParameters.Text.LOWER_CAMEL, c -> c.notNull().message("\"" + _ScheduledCallParameters.Text.LOWER_CAMEL + "\" is required."))
            ._boolean(x -> !x.has(_ScheduledCallParameters.Text.LOWER_CAMEL) || x.get(_ScheduledCallParameters.Text.LOWER_CAMEL).isTextual(), _ScheduledCallParameters.Text.LOWER_CAMEL, c -> c.isTrue().message("\"" + _ScheduledCallParameters.Text.LOWER_CAMEL + "\" must be textual."))
            ._boolean(x -> !x.has(_ScheduledCallParameters.Text.LOWER_CAMEL) || !x.get(_ScheduledCallParameters.Text.LOWER_CAMEL).asText().isEmpty(), _ScheduledCallParameters.Text.LOWER_CAMEL, c -> c.isTrue().message("\"" + _ScheduledCallParameters.Text.LOWER_CAMEL + "\" must not be empty."))
            ._boolean(x -> !x.has(_ScheduledCallParameters.Text.LOWER_CAMEL) || x.get(_ScheduledCallParameters.Text.LOWER_CAMEL).asText().length() <= 200, _ScheduledCallParameters.Text.LOWER_CAMEL, c -> c.isTrue().message("The length of \"" + _ScheduledCallParameters.Text.LOWER_CAMEL + "\" must be less than or equal to 200."))
            ._object(x -> x.get(_ScheduledCallParameters.Speaker.LOWER_CAMEL), _ScheduledCallParameters.Speaker.LOWER_CAMEL, c -> c.notNull().message("\"" + _ScheduledCallParameters.Speaker.LOWER_CAMEL + "\" is required."))
            ._boolean(x -> !x.has(_ScheduledCallParameters.Speaker.LOWER_CAMEL) || x.get(_ScheduledCallParameters.Speaker.LOWER_CAMEL).isTextual(), _ScheduledCallParameters.Speaker.LOWER_CAMEL, c -> c.isTrue().message("\"" + _ScheduledCallParameters.Speaker.LOWER_CAMEL + "\" must be textual."))
            ._object(x -> x.get(_ScheduledCallParameters.ApiKey.LOWER_CAMEL), _ScheduledCallParameters.ApiKey.LOWER_CAMEL, c -> c.notNull().message("\"" + _ScheduledCallParameters.ApiKey.LOWER_CAMEL + "\" is required."))
            ._boolean(x -> !x.has(_ScheduledCallParameters.ApiKey.LOWER_CAMEL) || x.get(_ScheduledCallParameters.ApiKey.LOWER_CAMEL).isTextual(), _ScheduledCallParameters.ApiKey.LOWER_CAMEL, c -> c.isTrue().message("\"" + _ScheduledCallParameters.ApiKey.LOWER_CAMEL + "\" must be textual."))
            ._object(x -> x.get(_ScheduledCallParameters.ScheduledAt.LOWER_CAMEL), _ScheduledCallParameters.ScheduledAt.LOWER_CAMEL, c -> c.notNull().message("\"" + _ScheduledCallParameters.ScheduledAt.LOWER_CAMEL + "\" is required."))
            ._boolean(x -> !x.has(_ScheduledCallParameters.ScheduledAt.LOWER_CAMEL) || x.get(_ScheduledCallParameters.ScheduledAt.LOWER_CAMEL).isLong(), _ScheduledCallParameters.ScheduledAt.LOWER_CAMEL, c -> c.isTrue().message("\"" + _ScheduledCallParameters.ScheduledAt.LOWER_CAMEL + "\" must be long."))
            ._boolean(x -> !x.has(_ScheduledCallParameters.ScheduledAt.LOWER_CAMEL) || !x.get(_ScheduledCallParameters.ScheduledAt.LOWER_CAMEL).isLong() || Instant.ofEpochMilli(x.get(_ScheduledCallParameters.ScheduledAt.LOWER_CAMEL).asLong()).isAfter(Instant.now()), _ScheduledCallParameters.ScheduledAt.LOWER_CAMEL, c -> c.isTrue().message("\"" + _ScheduledCallParameters.ScheduledAt.LOWER_CAMEL + "\" must be future."))
            .build();

    public ScheduledCallController(ScheduledCallRepository scheduledCallRepository) {
        this.scheduledCallRepository = scheduledCallRepository;
    }

    @GetMapping(path = "subscriptions/{subscriptionId}")
    public Flux<ScheduledCall> getScheduledCalls(@PathVariable String subscriptionId) {
        return this.scheduledCallRepository.findOrderByScheduledAt(subscriptionId);
    }

    @PostMapping(path = "")
    public Mono<ResponseEntity<?>> postScheduledCalls(@RequestBody JsonNode json) {
        return this.validator.validateToEither(json)
                .bimap(violations -> Map.of("error", "Bad Request", "details", violations.details()),
                        body -> new ScheduledCall(UUID.randomUUID().toString(),
                                body.get(_ScheduledCallParameters.SubscriptionId.LOWER_CAMEL).asText(), body.get(_ScheduledCallParameters.Text.LOWER_CAMEL).asText(),
                                body.get(_ScheduledCallParameters.Speaker.LOWER_CAMEL).asText(),
                                body.has(_ScheduledCallParameters.Emotion.LOWER_CAMEL) ? body.get(_ScheduledCallParameters.Emotion.LOWER_CAMEL).asText() : null,
                                body.get(_ScheduledCallParameters.ApiKey.LOWER_CAMEL).asText(), Instant.ofEpochMilli(body.get(_ScheduledCallParameters.ScheduledAt.LOWER_CAMEL).asLong()).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                                CallState.SCHEDULED))
                .fold(details -> Mono.just(ResponseEntity.badRequest().body(details)),
                        scheduledCall ->
                                this.scheduledCallRepository.save(scheduledCall)
                                        .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created)));
    }

    @DeleteMapping(path = "/{id}")
    public Mono<Void> deleteScheduledCall(@PathVariable String id) {
        return this.scheduledCallRepository.delete(id);
    }
}
