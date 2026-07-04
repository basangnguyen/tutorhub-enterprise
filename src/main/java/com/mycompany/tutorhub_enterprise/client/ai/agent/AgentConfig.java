package com.mycompany.tutorhub_enterprise.client.ai.agent;

import java.time.Duration;

public final class AgentConfig {

    private static final int DEFAULT_MAX_TURNS = 6;
    private static final int DEFAULT_MAX_OBSERVATION_CHARS = 6000;
    private static final Duration DEFAULT_MODEL_TIMEOUT = Duration.ofSeconds(120);

    private final int maxTurns;
    private final int maxObservationChars;
    private final Duration modelTimeout;

    private AgentConfig(Builder builder) {
        this.maxTurns = builder.maxTurns;
        this.maxObservationChars = builder.maxObservationChars;
        this.modelTimeout = builder.modelTimeout;
    }

    public static AgentConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public int getMaxObservationChars() {
        return maxObservationChars;
    }

    public Duration getModelTimeout() {
        return modelTimeout;
    }

    public static final class Builder {
        private int maxTurns = DEFAULT_MAX_TURNS;
        private int maxObservationChars = DEFAULT_MAX_OBSERVATION_CHARS;
        private Duration modelTimeout = DEFAULT_MODEL_TIMEOUT;

        public Builder maxTurns(int maxTurns) {
            this.maxTurns = Math.max(1, Math.min(20, maxTurns));
            return this;
        }

        public Builder maxObservationChars(int maxObservationChars) {
            this.maxObservationChars = Math.max(500, Math.min(30000, maxObservationChars));
            return this;
        }

        public Builder modelTimeout(Duration modelTimeout) {
            if (modelTimeout != null && !modelTimeout.isNegative() && !modelTimeout.isZero()) {
                this.modelTimeout = modelTimeout;
            }
            return this;
        }

        public AgentConfig build() {
            return new AgentConfig(this);
        }
    }
}
