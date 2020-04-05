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

    public Mono<ScheduledCall> findScheduledOneOrderByScheduledAt() {
        final LocalDateTime now = LocalDateTime.now();
        return this.databaseClient.select()
                .from(ScheduledCall.class)
                .matching(where("state").is(CallState.SCHEDULED)
                        .and(where("scheduled_at").lessThanOrEquals(now)))
                .orderBy(Sort.Order.asc("scheduled_at"))
                .page(PageRequest.of(0, 1))
                .fetch()
                .one();
    }

    public Flux<ScheduledCall> findOrderByScheduledAt(String subscriptionId) {
        final LocalDateTime ago = LocalDateTime.now().minusHours(3);
        return this.databaseClient.select()
                .from(ScheduledCall.class)
                .matching(where("subscription_id").is(subscriptionId)
                        .and(where("scheduled_at").greaterThanOrEquals(ago)))
                .orderBy(Sort.Order.asc("scheduled_at"))
                .page(PageRequest.of(0, 50))
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
    public Mono<Void> changeStateById(String id, CallState state) {
        return this.databaseClient.update()
                .table("scheduled_call")
                .using(Update.update("state", state))
                .matching(where("id").is(id))
                .then();
    }

    @Transactional
    public Mono<Void> delete(String id) {
        return this.databaseClient.delete()
                .from(ScheduledCall.class)
                .matching(where("id").is(id))
                .then();
    }
}
