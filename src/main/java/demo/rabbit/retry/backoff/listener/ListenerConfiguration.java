package demo.rabbit.retry.backoff.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.rabbit.retry.backoff.listener.delayed.ListenerDelayedHandler;
import demo.rabbit.retry.backoff.listener.delayed.RetryQueuesDelayedInterceptor;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetrySimulator;

@EnableRabbit
@Configuration
public class ListenerConfiguration {

  // common configuration
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    return new RabbitTemplate(connectionFactory);
  }

  @Bean
  ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  // listener configuration

//  @Bean
//  ListenerHandler listenerHandler(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
//    return new ListenerHandler(rabbitTemplate, objectMapper);
//  }

  // blocking configuration

  @Bean
  public ObservableRejectAndDontRequeueRecoverer observableRecoverer(RabbitTemplate rabbitTemplate,
      ObjectMapper mapper) {
    return new ObservableRejectAndDontRequeueRecoverer(rabbitTemplate, mapper);
  }

  @Bean
  public RetryOperationsInterceptor retryInterceptor(RabbitTemplate rabbitTemplate, ObjectMapper mapper) {

    return RetryInterceptorBuilder.stateless()
        .backOffOptions(5000, 3.0, 7000)
        .maxAttempts(3)
        .recoverer(observableRecoverer(rabbitTemplate, mapper))
        .build();
  }

  @Bean
  public SimpleRabbitListenerContainerFactory retryContainerFactory(ConnectionFactory connectionFactory,
      RetryOperationsInterceptor retryInterceptor) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);

    Advice[] adviceChain = { retryInterceptor };
    factory.setAdviceChain(adviceChain);

    return factory;
  }


  // non-blocking configuration

  @Bean
  public SimpleRabbitListenerContainerFactory retryQueuesContainerFactory(
      ConnectionFactory connectionFactory, RetryQueuesInterceptor retryInterceptor) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);

    Advice[] adviceChain = { retryInterceptor };
    factory.setAdviceChain(adviceChain);

    return factory;
  }

  @Bean
  public RetryQueues retryQueues() {
    return new RetryQueues(10000, 3.0, 20000, 3);
  }

  @Bean
  public RetryQueuesInterceptor retryQueuesInterceptor(RabbitTemplate rabbitTemplate,
      RetryQueues retryQueues, ObjectMapper objectMapper) {
    return new RetryQueuesInterceptor(rabbitTemplate, retryQueues, objectMapper);
  }
}
