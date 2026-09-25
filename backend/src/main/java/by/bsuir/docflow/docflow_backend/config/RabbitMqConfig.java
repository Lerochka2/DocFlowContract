package by.bsuir.docflow.docflow_backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "docflow.rabbitmq.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "docflow.events";

    public static final String DOCUMENT_UPLOADED_QUEUE =
            "document.uploaded.queue";

    public static final String DOCUMENT_PROCESSED_QUEUE =
            "document.processed.queue";

    public static final String DOCUMENT_UPLOADED_ROUTING_KEY =
            "document.uploaded";

    public static final String DOCUMENT_PROCESSED_ROUTING_KEY =
            "document.processed";

    @Bean
    public TopicExchange docflowExchange() {
        return new TopicExchange(
                EXCHANGE_NAME,
                true,
                false
        );
    }

    @Bean
    public Queue documentUploadedQueue() {
        return QueueBuilder
                .durable(DOCUMENT_UPLOADED_QUEUE)
                .build();
    }

    @Bean
    public Queue documentProcessedQueue() {
        return QueueBuilder
                .durable(DOCUMENT_PROCESSED_QUEUE)
                .build();
    }

    @Bean
    public Binding documentUploadedBinding(
            @Qualifier("documentUploadedQueue")
            Queue documentUploadedQueue,
            TopicExchange docflowExchange
    ) {
        return BindingBuilder
                .bind(documentUploadedQueue)
                .to(docflowExchange)
                .with(DOCUMENT_UPLOADED_ROUTING_KEY);
    }

    @Bean
    public Binding documentProcessedBinding(
            @Qualifier("documentProcessedQueue")
            Queue documentProcessedQueue,
            TopicExchange docflowExchange
    ) {
        return BindingBuilder
                .bind(documentProcessedQueue)
                .to(docflowExchange)
                .with(DOCUMENT_PROCESSED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter
    ) {
        RabbitTemplate template =
                new RabbitTemplate(connectionFactory);

        template.setMessageConverter(jsonMessageConverter);

        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(
            ConnectionFactory connectionFactory
    ) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public ApplicationRunner rabbitTopologyInitializer(
            RabbitAdmin rabbitAdmin
    ) {
        return args -> rabbitAdmin.initialize();
    }
}