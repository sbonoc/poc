package com.agnostic.producerspringboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ProducerSpringBootApplication {
    private static final Logger logger = LoggerFactory.getLogger(ProducerSpringBootApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ProducerSpringBootApplication.class, args);
        ProducerConfig config = context.getBean(ProducerConfig.class);
        logger.info(
            "producer-springboot started pubsub={} topic={} daprHttpPort={}",
            config.pubsubName(),
            config.topicName(),
            config.httpPort());
    }
}
