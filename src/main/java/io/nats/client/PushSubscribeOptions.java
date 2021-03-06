// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client;

import io.nats.client.api.ConsumerConfiguration;

import static io.nats.client.support.Validator.*;

/**
 * The PushSubscribeOptions class specifies the options for subscribing with JetStream enabled servers.
 * Options are created using the constructors or a {@link Builder}.
 */
public class PushSubscribeOptions extends SubscribeOptions {

    private PushSubscribeOptions(String stream, ConsumerConfiguration consumerConfig) {
        super(stream, consumerConfig);
    }

    /**
     * Gets the deliver subject held in the consumer configuration.
     * @return the Deliver subject
     */
    public String getDeliverSubject() {
        return consumerConfig.getDeliverSubject();
    }

    /**
     * Create PushSubscribeOptions where you are binding to
     * a specific stream, which could be a stream or a mirror
     * @param stream the stream name to bind to
     * @return subscribe options
     */
    public static PushSubscribeOptions bind(String stream) {
        return new Builder().stream(stream).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * PushSubscribeOptions can be created using a Builder. The builder supports chaining and will
     * create a default set of options if no methods are calls.
     */
    public static class Builder
            extends SubscribeOptions.Builder<Builder, PushSubscribeOptions> {
        private String deliverSubject;

        @Override
        protected Builder getThis() {
            return this;
        }

        /**
         * Setting this specifies the push model to a delivery subject.
         * Null or empty clears the field.
         * @param deliverSubject the subject to deliver on.
         * @return the builder.
         */
        public Builder deliverSubject(String deliverSubject) {
            this.deliverSubject = deliverSubject;
            return this;
        }

        /**
         * Builds the subscribe options.
         * @return subscribe options
         */
        @Override
        public PushSubscribeOptions build() {
            validateStreamNameOrEmptyAsNull(stream);

            durable = validateDurableOrEmptyAsNull(durable);
            if (durable == null && consumerConfig != null) {
                durable = validateDurableOrEmptyAsNull(consumerConfig.getDurable());
            }

            this.consumerConfig = ConsumerConfiguration.builder(consumerConfig)
                    .durable(durable)
                    .deliverSubject(emptyAsNull(deliverSubject))
                    .build();

            return new PushSubscribeOptions(stream, consumerConfig);
        }
    }
}
