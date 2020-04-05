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

import static am.ik.lab.syaberu.scheduled._ScheduledCallArgumentsMeta.*;

@RestController
@RequestMapping(path = "scheduled_calls")
@CrossOrigin
public class ScheduledCallController {
    private final ScheduledCallRepository scheduledCallRepository;
    private final Validator<JsonNode> validator = ValidatorBuilder.<JsonNode>of()
            ._object(x -> x.get(SUBSCRIPTIONID.name()), SUBSCRIPTIONID.name(), c -> c.notNull().message("\"" + SUBSCRIPTIONID.name() + "\" is required."))
            ._boolean(x -> !x.has(SUBSCRIPTIONID.name()) || x.get(SUBSCRIPTIONID.name()).isTextual(), SUBSCRIPTIONID.name(), c -> c.isTrue().message("\"" + SUBSCRIPTIONID.name() + "\" must be textual."))
            ._object(x -> x.get(TEXT.name()), TEXT.name(), c -> c.notNull().message("\"" + TEXT.name() + "\" is required."))
            ._boolean(x -> !x.has(TEXT.name()) || x.get(TEXT.name()).isTextual(), TEXT.name(), c -> c.isTrue().message("\"" + TEXT.name() + "\" must be textual."))
            ._boolean(x -> !x.has(TEXT.name()) || !x.get(TEXT.name()).asText().isEmpty(), TEXT.name(), c -> c.isTrue().message("\"" + TEXT.name() + "\" must not be empty."))
            ._boolean(x -> !x.has(TEXT.name()) || x.get(TEXT.name()).asText().length() <= 200, TEXT.name(), c -> c.isTrue().message("The length of \"" + TEXT.name() + "\" must be less than or equal to 200."))
            ._object(x -> x.get(SPEAKER.name()), SPEAKER.name(), c -> c.notNull().message("\"" + SPEAKER.name() + "\" is required."))
            ._boolean(x -> !x.has(SPEAKER.name()) || x.get(SPEAKER.name()).isTextual(), SPEAKER.name(), c -> c.isTrue().message("\"" + SPEAKER.name() + "\" must be textual."))
            ._object(x -> x.get(APIKEY.name()), APIKEY.name(), c -> c.notNull().message("\"" + APIKEY.name() + "\" is required."))
            ._boolean(x -> !x.has(APIKEY.name()) || x.get(APIKEY.name()).isTextual(), APIKEY.name(), c -> c.isTrue().message("\"" + APIKEY.name() + "\" must be textual."))
            ._object(x -> x.get(SCHEDULEDAT.name()), SCHEDULEDAT.name(), c -> c.notNull().message("\"" + SCHEDULEDAT.name() + "\" is required."))
            ._boolean(x -> !x.has(SCHEDULEDAT.name()) || x.get(SCHEDULEDAT.name()).isLong(), SCHEDULEDAT.name(), c -> c.isTrue().message("\"" + SCHEDULEDAT.name() + "\" must be long."))
            ._boolean(x -> !x.has(SCHEDULEDAT.name()) || !x.get(SCHEDULEDAT.name()).isLong() || Instant.ofEpochMilli(x.get(SCHEDULEDAT.name()).asLong()).isAfter(Instant.now()), SCHEDULEDAT.name(), c -> c.isTrue().message("\"" + SCHEDULEDAT.name() + "\" must be future."))
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
                                body.get(SUBSCRIPTIONID.name()).asText(), body.get(TEXT.name()).asText(),
                                body.get(SPEAKER.name()).asText(),
                                body.has(EMOTION.name()) ? body.get(EMOTION.name()).asText() : null,
                                body.get(APIKEY.name()).asText(), Instant.ofEpochMilli(body.get(SCHEDULEDAT.name()).asLong()).atZone(ZoneId.systemDefault()).toLocalDateTime(),
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
