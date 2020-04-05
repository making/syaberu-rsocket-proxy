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
            ._object(x -> x.get("subscriptionId"), "subscriptionId", c -> c.notNull().message("\"subscriptionId\" is required."))
            ._boolean(x -> !x.has("subscriptionId") || x.get("subscriptionId").isTextual(), "subscriptionId", c -> c.isTrue().message("\"subscriptionId\" must be textual."))
            ._object(x -> x.get("text"), "text", c -> c.notNull().message("\"text\" is required."))
            ._boolean(x -> !x.has("text") || x.get("text").isTextual(), "text", c -> c.isTrue().message("\"text\" must be textual."))
            ._boolean(x -> !x.has("text") || !x.get("text").asText().isEmpty(), "text", c -> c.isTrue().message("\"text\" must not be empty."))
            ._boolean(x -> !x.has("text") || x.get("text").asText().length() <= 200, "text", c -> c.isTrue().message("The length of \"text\" must be less than or equal to 200."))
            ._object(x -> x.get("speaker"), "speaker", c -> c.notNull().message("\"speaker\" is required."))
            ._boolean(x -> !x.has("speaker") || x.get("speaker").isTextual(), "speaker", c -> c.isTrue().message("\"speaker\" must be textual."))
            ._object(x -> x.get("apiKey"), "apiKey", c -> c.notNull().message("\"apiKey\" is required."))
            ._boolean(x -> !x.has("apiKey") || x.get("apiKey").isTextual(), "apiKey", c -> c.isTrue().message("\"apiKey\" must be textual."))
            ._object(x -> x.get("scheduledAt"), "scheduledAt", c -> c.notNull().message("\"scheduledAt\" is required."))
            ._boolean(x -> !x.has("scheduledAt") || x.get("scheduledAt").isLong(), "scheduledAt", c -> c.isTrue().message("\"scheduledAt\" must be long."))
            ._boolean(x -> !x.has("scheduledAt") || !x.get("scheduledAt").isLong() || Instant.ofEpochMilli(x.get("scheduledAt").asLong()).isAfter(Instant.now()), "scheduledAt", c -> c.isTrue().message("\"scheduledAt\" must be future."))
            .build();

    public ScheduledCallController(ScheduledCallRepository scheduledCallRepository) {
        this.scheduledCallRepository = scheduledCallRepository;
    }

    @GetMapping(path = "subscriptions/{subscriptionId}")
    public Flux<ScheduledCall> getScheduledCalls(@PathVariable("subscriptionId") String subscriptionId) {
        return this.scheduledCallRepository.findOrderByScheduledAt(subscriptionId);
    }

    @PostMapping(path = "")
    public Mono<ResponseEntity<?>> postScheduledCalls(@RequestBody JsonNode json) {
        return this.validator.validateToEither(json)
                .bimap(violations -> Map.of("error", "Bad Request", "details", violations.details()),
                        body -> new ScheduledCall(UUID.randomUUID().toString(),
                                body.get("subscriptionId").asText(), body.get("text").asText(),
                                body.get("speaker").asText(),
                                body.has("emotion") ? body.get("emotion").asText() : null,
                                body.get("apiKey").asText(), Instant.ofEpochMilli(body.get("scheduledAt").asLong()).atZone(ZoneId.systemDefault()).toLocalDateTime(),
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
