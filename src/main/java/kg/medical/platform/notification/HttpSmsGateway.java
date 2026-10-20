package kg.medical.platform.notification;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
@Service
@ConditionalOnProperty(name="medical.sms.mode",havingValue="http")
public class HttpSmsGateway implements SmsGateway {
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper;
    private final URI url;
    private final String apiKey;
    public HttpSmsGateway(ObjectMapper mapper,@Value("${medical.sms.url:}") String url,@Value("${medical.sms.api-key:}") String apiKey) {
        this.mapper=mapper; this.apiKey=apiKey;
        this.url=URI.create(url);
        if (!"https".equals(this.url.getScheme()) && !("http".equals(this.url.getScheme()) && java.util.Set.of("localhost","127.0.0.1").contains(this.url.getHost()))) { throw new IllegalArgumentException("SMS URL должен использовать HTTPS; HTTP разрешён для локального тестового шлюза"); }
    }
    public Result send(UUID id,String phone,String message) {
        HttpRequest.Builder builder=HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json;charset=UTF-8").header("Idempotency-Key",id.toString());
        if (!apiKey.isBlank()) { builder.header("Authorization","Bearer "+apiKey); }
        HttpRequest request=builder.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("to",phone,"message",message,"idempotencyKey",id.toString())))).build();
        try {
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if (response.statusCode()<200 || response.statusCode()>=300) { throw new IllegalStateException("SMS-шлюз ответил HTTP "+response.statusCode()); }
            return new Result("HTTP-"+id,false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SMS-запрос прерван",exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("SMS-шлюз недоступен",exception);
        }
    }
}
