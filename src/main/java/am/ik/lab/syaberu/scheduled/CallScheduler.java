package am.ik.lab.syaberu.scheduled;

import am.ik.lab.syaberu.rsocket.RsocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class CallScheduler {
    private final Logger log = LoggerFactory.getLogger(CallScheduler.class);
    private final RsocketHandler rsocketHandler;
    private final ScheduledCallRepository scheduledCallRepository;

    public CallScheduler(RsocketHandler rsocketHandler, ScheduledCallRepository scheduledCallRepository) {
        this.rsocketHandler = rsocketHandler;
        this.scheduledCallRepository = scheduledCallRepository;
    }


    @Scheduled(fixedRateString = "${scheduler.interval}")
    public void schedule() {
        final int index = Stream.ofNullable(System.getenv("CF_INSTANCE_INDEX"))
                .mapToInt(Integer::parseInt)
                .findAny()
                .orElse(0);
        if (index != 0) {
            return;
        }
        this.scheduledCallRepository.findScheduledOrderByScheduledAt()
                .flatMapSequential(scheduledCall -> {
                    log.info("Calling {}", scheduledCall);
                    return this.rsocketHandler
                            .send(scheduledCall.getSubscriptionId(),
                                    scheduledCall.getText(),
                                    scheduledCall.getSpeaker(),
                                    scheduledCall.getEmotion(),
                                    scheduledCall.getApiKey())
                            .flatMap(__ -> this.scheduledCallRepository.changeStateById(scheduledCall.getId(), CallState.SUCCEEDED, scheduledCall.getScheduledAt()))
                            .onErrorResume(e -> this.scheduledCallRepository.changeStateById(scheduledCall.getId(), CallState.FAILED, scheduledCall.getScheduledAt()));
                }, 1)
                .subscribe();
    }
}
