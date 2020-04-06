package am.ik.lab.syaberu.scheduled;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.springframework.data.relational.core.query.Criteria.where;

@Repository
public class ScheduledCallRepository {
    private final DatabaseClient databaseClient;

    public ScheduledCallRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Flux<ScheduledCall> findScheduledOrderByScheduledAt() {
        final LocalDateTime now = LocalDateTime.now();
        return this.databaseClient.select()
                .from(ScheduledCall.class)
                .matching(where(_ScheduledCallParameters.State.LOWER_UNDERSCORE).is(CallState.SCHEDULED)
                        .and(_ScheduledCallParameters.ScheduledAt.LOWER_UNDERSCORE).lessThanOrEquals(now))
                .orderBy(Sort.Order.asc(_ScheduledCallParameters.ScheduledAt.LOWER_UNDERSCORE))
                .page(PageRequest.of(0, 8))
                .fetch()
                .all();
    }

    public Flux<ScheduledCall> findOrderByScheduledAt(String subscriptionId) {
        final LocalDateTime ago = LocalDateTime.now().minusHours(3);
        return this.databaseClient.select()
                .from(ScheduledCall.class)
                .matching(where(_ScheduledCallParameters.SubscriptionId.LOWER_UNDERSCORE).is(subscriptionId)
                        .and(_ScheduledCallParameters.ScheduledAt.LOWER_UNDERSCORE).greaterThanOrEquals(ago))
                .orderBy(Sort.Order.asc(_ScheduledCallParameters.ScheduledAt.LOWER_UNDERSCORE))
                .page(PageRequest.of(0, 20))
                .fetch()
                .all();
    }

    @Transactional
    public Mono<ScheduledCall> save(ScheduledCall scheduledCall) {
        return this.databaseClient.insert()
                .into(ScheduledCall.class)
                .using(scheduledCall)
                .then()
                .thenReturn(scheduledCall);
    }

    @Transactional
    public Mono<Void> changeStateById(String id, CallState state, LocalDateTime scheduledAt) {
        return this.databaseClient.update()
                .table(_ScheduledCallParameters.LOWER_UNDERSCORE)
                .using(Update
                        .update(_ScheduledCallParameters.State.LOWER_UNDERSCORE, state)
                        .set(_ScheduledCallParameters.ScheduledAt.LOWER_UNDERSCORE, scheduledAt))
                .matching(where(_ScheduledCallParameters.Id.LOWER_UNDERSCORE).is(id))
                .then();
    }

    @Transactional
    public Mono<Void> delete(String id) {
        return this.databaseClient.delete()
                .from(ScheduledCall.class)
                .matching(where(_ScheduledCallParameters.Id.LOWER_UNDERSCORE).is(id))
                .then();
    }
}
