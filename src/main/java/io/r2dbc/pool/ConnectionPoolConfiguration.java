/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.pool;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import reactor.pool.PoolBuilder;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Connection pool configuration.
 *
 * @author Mark Paluch
 * @author Tadaya Tsuyukubo
 */
public final class ConnectionPoolConfiguration {

    private final ConnectionFactory connectionFactory;

    private final Duration maxIdleTime;

    private final Duration maxCreateConnectionTime;

    private final Duration maxAcquireTime;

    private final int initialSize;

    private final int maxSize;

    @Nullable
    private final String validationQuery;

    private final Consumer<PoolBuilder<Connection>> customizer;

    private ConnectionPoolConfiguration(ConnectionFactory connectionFactory, Duration maxIdleTime,
                                        int initialSize, int maxSize, @Nullable String validationQuery, Duration maxCreateConnectionTime,
                                        Duration maxAcquireTime, Consumer<PoolBuilder<Connection>> customizer) {
        this.connectionFactory = Assert.requireNonNull(connectionFactory, "ConnectionFactory must not be null");
        this.initialSize = initialSize;
        this.maxSize = maxSize;
        this.maxIdleTime = maxIdleTime;
        this.validationQuery = validationQuery;
        this.maxCreateConnectionTime = maxCreateConnectionTime;
        this.maxAcquireTime = maxAcquireTime;
        this.customizer = customizer;
    }

    /**
     * Returns a new {@link Builder}.
     *
     * @param connectionFactory the {@link ConnectionFactory} to wrap.
     * @return a new {@link Builder}
     */
    public static Builder builder(ConnectionFactory connectionFactory) {
        return new Builder(Assert.requireNonNull(connectionFactory, "ConnectionFactory must not be null"));
    }

    ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    Duration getMaxIdleTime() {
        return maxIdleTime;
    }


    public int getInitialSize() {
        return initialSize;
    }

    int getMaxSize() {
        return maxSize;
    }

    @Nullable
    String getValidationQuery() {
        return validationQuery;
    }

    Duration getMaxCreateConnectionTime() {
        return this.maxCreateConnectionTime;
    }

    Duration getMaxAcquireTime() {
        return this.maxAcquireTime;
    }

    Consumer<PoolBuilder<Connection>> getCustomizer() {
        return this.customizer;
    }

    /**
     * A builder for {@link ConnectionPoolConfiguration} instances.
     * <p>
     * <i>This class is not threadsafe</i>
     */
    public static final class Builder {

        private final ConnectionFactory connectionFactory;

        private int initialSize = 10;

        private int maxSize = 10;

        private Duration maxIdleTime = Duration.ofMinutes(30);

        private Duration maxCreateConnectionTime = Duration.ZERO;  // ZERO indicates no-timeout

        private Duration maxAcquireTime = Duration.ZERO;  // ZERO indicates no-timeout

        private Consumer<PoolBuilder<Connection>> customizer = poolBuilder -> {
        };  // no-op

        @Nullable
        private String validationQuery;

        private Builder(ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
        }

        /**
         * Configure the initial connection pool size. Defaults to {@code 10}.
         *
         * @param initialSize the initial pool size, must be equal or greater than zero.
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code maxSize} is negative or zero.
         */
        public Builder initialSize(int initialSize) {
            if (initialSize < 0) {
                throw new IllegalArgumentException("Initial pool size must be equal greater zero");
            }
            this.initialSize = initialSize;
            return this;
        }

        /**
         * Configure the maximal connection pool size. Defaults to {@code 10}.
         *
         * @param maxSize the maximal pool size, must be greater than zero.
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code maxSize} is negative or zero.
         */
        public Builder maxSize(int maxSize) {
            if (maxSize < 1) {
                throw new IllegalArgumentException("Maximal pool size must be greater zero");
            }
            this.maxSize = maxSize;
            return this;
        }

        /**
         * Configure a idle {@link Duration timeout}. Defaults to 30 minutes.
         *
         * @param maxIdleTime the maximum idle time, must not be {@code null} and must not be negative. {@link Duration#ZERO} means no idle timeout.
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code maxIdleTime} is {@code null} or negative value.
         */
        public Builder maxIdleTime(Duration maxIdleTime) {
            Assert.requireNonNull(maxIdleTime, "MaxIdleTime must not be null");
            if (maxIdleTime.isNegative()) {
                throw new IllegalArgumentException("MaxIdleTime must not be negative");
            }
            this.maxIdleTime = maxIdleTime;
            return this;
        }

        /**
         * Configure {@link Duration timeout} for creating a new {@link Connection} from {@link ConnectionFactory}. Default is no timeout.
         *
         * @param maxCreateConnectionTime the maximum time to create a new {@link Connection} from {@link ConnectionFactory}, must not be {@code null} and must not be negative.
         *                                {@link Duration#ZERO} indicates no timeout.
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code maxCreateConnectionTime} is {@code null} or negative.
         */
        public Builder maxCreateConnectionTime(Duration maxCreateConnectionTime) {
            Assert.requireNonNull(maxCreateConnectionTime, "maxCreateConnectionTime must not be null");
            if (maxCreateConnectionTime.isNegative()) {
                throw new IllegalArgumentException("maxCreateConnectionTime must not be negative");
            }
            this.maxCreateConnectionTime = maxCreateConnectionTime;
            return this;
        }

        /**
         * Configure {@link Duration timeout} for acquiring a {@link Connection} from pool. Default is no timeout.
         * <p>
         * When acquiring a {@link Connection} requires obtaining a new {@link Connection} from underlying {@link ConnectionFactory}, this timeout
         * also applies to get the new one.
         *
         * @param maxAcquireTime the maximum time to acquire connection from pool, must not be {@code null} and must not be negative.
         *                       {@link Duration#ZERO} indicates no timeout.
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code maxAcquireTime} is negative.
         */
        public Builder maxAcquireTime(Duration maxAcquireTime) {
            Assert.requireNonNull(maxAcquireTime, "maxAcquireTime must not be null");
            if (maxAcquireTime.isNegative()) {
                throw new IllegalArgumentException("maxAcquireTime must not be negative");
            }
            this.maxAcquireTime = maxAcquireTime;
            return this;
        }

        /**
         * Configure a validation query.
         *
         * @param validationQuery the validation query to run before returning a {@link Connection} from the pool, must not be {@code null}.
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code validationQuery} is {@code null}
         */
        public Builder validationQuery(String validationQuery) {
            this.validationQuery = Assert.requireNonNull(validationQuery, "ValidationQuery must not be null");
            return this;
        }

        /**
         * Configure a customizer for {@link PoolBuilder} that constructs the {@link Connection} pool.
         *
         * @param customizer customizer for {@link PoolBuilder} that creates the {@link Connection} pool, must not be {@code null}.
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code customizer} is {@code null}
         */
        public Builder customizer(Consumer<PoolBuilder<Connection>> customizer) {
            this.customizer = Assert.requireNonNull(customizer, "PoolBuilder customizer must not be null");
            return this;
        }

        /**
         * Returns a configured {@link ConnectionPoolConfiguration}.
         *
         * @return a configured {@link ConnectionPoolConfiguration}
         */
        public ConnectionPoolConfiguration build() {
            return new ConnectionPoolConfiguration(this.connectionFactory, this.maxIdleTime,
                this.initialSize, this.maxSize, this.validationQuery, this.maxCreateConnectionTime, this.maxAcquireTime, this.customizer);
        }

        @Override
        public String toString() {
            return "Builder{" +
                "connectionFactory='" + this.connectionFactory + '\'' +
                ", maxIdleTime='" + this.maxIdleTime + '\'' +
                ", maxCreateConnectionTime='" + this.maxCreateConnectionTime + '\'' +
                ", maxAcquireTime='" + this.maxAcquireTime + '\'' +
                ", initialSize='" + this.initialSize + '\'' +
                ", maxSize='" + this.maxSize + '\'' +
                ", validationQuery='" + this.validationQuery + '\'' +
                '}';
        }
    }
}
