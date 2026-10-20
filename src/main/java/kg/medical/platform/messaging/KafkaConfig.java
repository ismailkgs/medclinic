package kg.medical.platform.messaging;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
@Configuration
public class KafkaConfig {
    @Bean
    public KafkaTopics kafkaTopics(@Value("${medical.kafka.topic-prefix:medical}") String prefix) { return new KafkaTopics(prefix+".lab.requests",prefix+".report.requests"); }
    @Bean
    public NewTopic labRequestsTopic(KafkaTopics topics) { return TopicBuilder.name(topics.labRequests()).partitions(3).replicas(1).build(); }
    @Bean
    public NewTopic reportRequestsTopic(KafkaTopics topics) { return TopicBuilder.name(topics.reportRequests()).partitions(3).replicas(1).build(); }
}
