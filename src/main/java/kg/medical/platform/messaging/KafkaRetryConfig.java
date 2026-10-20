package kg.medical.platform.messaging;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;
@Configuration
public class KafkaRetryConfig {
    @Bean
    public NewTopic labDeadLetters(KafkaTopics topics) { return TopicBuilder.name(topics.labRequests()+".DLT").partitions(3).replicas(1).build(); }
    @Bean
    public NewTopic reportDeadLetters(KafkaTopics topics) { return TopicBuilder.name(topics.reportRequests()+".DLT").partitions(3).replicas(1).build(); }
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String,String> template) {
        DeadLetterPublishingRecoverer recoverer=new DeadLetterPublishingRecoverer(template);
        recoverer.setFailIfSendResultIsError(true);
        return new DefaultErrorHandler(recoverer,new FixedBackOff(1000L,3L));
    }
}
