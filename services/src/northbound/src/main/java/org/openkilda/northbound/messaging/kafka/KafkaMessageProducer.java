/* Copyright 2017 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.northbound.messaging.kafka;

import static org.openkilda.messaging.Utils.DEFAULT_CORRELATION_ID;
import static org.openkilda.messaging.Utils.MAPPER;
import static org.openkilda.messaging.error.ErrorType.DATA_INVALID;
import static org.openkilda.messaging.error.ErrorType.INTERNAL_ERROR;

import org.openkilda.messaging.error.MessageException;
import org.openkilda.northbound.messaging.MessageProducer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka message producer.
 */
@Component
@PropertySource("classpath:northbound.properties")
public class KafkaMessageProducer implements MessageProducer {
    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageProducer.class);
    /**
     * Kafka template.
     */
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final String topic, final Object object) {
        ListenableFuture<SendResult<String, String>> future;
        String message;

        try {
            message = MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            String errorMessage = "Unable to serialize object";
            logger.error("{}: object={}", errorMessage, object, exception);
            throw new MessageException(DEFAULT_CORRELATION_ID, System.currentTimeMillis(),
                    DATA_INVALID, errorMessage, object.toString());
        }

        future = kafkaTemplate.send(topic, message);
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                logger.debug("Message sent: topic={}, message={}", topic, message);
            }

            @Override
            public void onFailure(Throwable exception) {
                logger.error("Unable to send message: topic={}, message={}", topic, message, exception);
            }
        });

        try {
            SendResult<String, String> result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            logger.debug("Record sent: record={}, metadata={}", result.getProducerRecord(), result.getRecordMetadata());
        } catch (TimeoutException | ExecutionException | InterruptedException exception) {
            String errorMessage = "Unable to send message";
            logger.error("{}: topic={}, message={}", errorMessage, topic, message, exception);
            throw new MessageException(DEFAULT_CORRELATION_ID, System.currentTimeMillis(),
                    INTERNAL_ERROR, errorMessage, message);
        }
    }
}
