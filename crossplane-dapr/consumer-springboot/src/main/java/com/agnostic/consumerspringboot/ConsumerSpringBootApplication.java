package com.agnostic.consumerspringboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ConsumerSpringBootApplication {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerSpringBootApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ConsumerSpringBootApplication.class, args);
        ConsumerConfig config = context.getBean(ConsumerConfig.class);
        logger.info(
            "consumer-springboot started pubsub={} topic={} route={}",
            config.pubsubName(),
            config.topicName(),
            config.normalizedRoute());
    }
}
