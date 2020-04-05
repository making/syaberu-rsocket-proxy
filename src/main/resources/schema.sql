CREATE TABLE IF NOT EXISTS scheduled_call
(
    id VARCHAR(36) PRIMARY KEY,
    subscription_id VARCHAR(128) NOT NULL,
    text VARCHAR(200) NOT NULL,
    speaker VARCHAR(16) NOT NULL,
    emotion VARCHAR(16),
    api_key VARCHAR(255) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    state VARCHAR(16) NOT NULL,
    INDEX scheduled_call_subscription_id (subscription_id),
    INDEX scheduled_call_scheduled_at (scheduled_at),
    INDEX scheduled_state (state)
);