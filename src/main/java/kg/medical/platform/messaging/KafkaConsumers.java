package kg.medical.platform.messaging;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
public class KafkaConsumers {
    private final EventProcessor processor;
    public KafkaConsumers(EventProcessor processor) { this.processor=processor; }
    @KafkaListener(topics="#{@kafkaTopics.labRequests()}")
    public void lab(String payload) { processor.lab(payload); }
    @KafkaListener(topics="#{@kafkaTopics.reportRequests()}")
    public void report(String payload) { processor.report(payload); }
}
