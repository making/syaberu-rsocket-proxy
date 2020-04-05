package am.ik.lab.syaberu.scheduled;

import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

public class ScheduledCall {
    @Id
    private final String id;
    private final String subscriptionId;
    private final String text;
    private final String speaker;
    private final String emotion;
    private final String apiKey;
    private final LocalDateTime scheduledAt;
    private final CallState state;

    public ScheduledCall(String id, String subscriptionId, String text, String speaker, String emotion, String apiKey, LocalDateTime scheduledAt, CallState state) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.text = text;
        this.speaker = speaker;
        this.emotion = emotion;
        this.apiKey = apiKey;
        this.scheduledAt = scheduledAt;
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getText() {
        return text;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getEmotion() {
        return emotion;
    }

    public String getApiKey() {
        return apiKey;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public CallState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "ScheduledCall{" +
                "id='" + id + '\'' +
                ", subscriptionId='" + subscriptionId + '\'' +
                ", text='" + text + '\'' +
                ", speaker='" + speaker + '\'' +
                ", emotion='" + emotion + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", scheduledAt=" + scheduledAt +
                ", state=" + state +
                '}';
    }
}
