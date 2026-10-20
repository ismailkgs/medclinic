package kg.medical.platform;
import static org.assertj.core.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import kg.medical.platform.notification.HttpSmsGateway;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
class HttpSmsGatewayTests {
    @Test
    void httpGatewaySendsUtf8AndStableIdempotencyKey() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        AtomicReference<String> body=new AtomicReference<>();
        AtomicReference<String> key=new AtomicReference<>();
        server.createContext("/sms",exchange->{ body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)); key.set(exchange.getRequestHeaders().getFirst("Idempotency-Key")); exchange.sendResponseHeaders(202,-1); exchange.close(); });
        server.start();
        try {
            HttpSmsGateway gateway=new HttpSmsGateway(JsonMapper.builder().build(),"http://127.0.0.1:"+server.getAddress().getPort()+"/sms","test-key");
            UUID id=UUID.randomUUID();
            var result=gateway.send(id,"+12025550123","Напоминание о приёме");
            assertThat(result.simulated()).isFalse();
            assertThat(key.get()).isEqualTo(id.toString());
            assertThat(body.get()).contains("Напоминание о приёме","+12025550123");
        } finally { server.stop(0); }
    }
    @Test
    void httpGatewayDoesNotTreatFailureAsDelivery() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/sms",exchange->{ exchange.sendResponseHeaders(503,-1); exchange.close(); });
        server.start();
        try {
            HttpSmsGateway gateway=new HttpSmsGateway(JsonMapper.builder().build(),"http://127.0.0.1:"+server.getAddress().getPort()+"/sms","");
            assertThatThrownBy(()->gateway.send(UUID.randomUUID(),"+12025550123","test")).isInstanceOf(IllegalStateException.class).hasMessageContaining("503");
        } finally { server.stop(0); }
    }
}
