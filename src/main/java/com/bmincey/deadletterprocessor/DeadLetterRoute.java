package com.bmincey.deadletterprocessor;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterRoute extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterRoute.class);

    @Autowired
    private MongoDBUtils mongoDBUtils;

    @Override
    public void configure() {


        errorHandler(deadLetterChannel("{{outbound.deadletter.endpoint}}")
                .useOriginalMessage()
                .maximumRedeliveries(5)
                .retryAttemptedLogLevel(LoggingLevel.DEBUG)
                .redeliveryDelay(5000)
                .logStackTrace(true));

        from("{{outbound.deadletter.endpoint}}")
                .transacted()
                .log(LoggingLevel.INFO, logger, "Processing Message in DLQ.")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) {
                        String messageString = exchange.getIn().getBody(String.class);

                        logger.info("Message: {}", messageString);

                        Status status = JsonToJava.jsonToStatus(messageString);

                        logger.info(status.toString());

                        mongoDBUtils.process(status);
                    }
                })
                .log(LoggingLevel.INFO, logger, "Completed writing to MongoDB Atlas.");


    }
}

