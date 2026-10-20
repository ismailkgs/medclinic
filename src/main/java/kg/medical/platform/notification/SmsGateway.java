package kg.medical.platform.notification;
import java.util.UUID;
public interface SmsGateway {
    Result send(UUID notificationId,String phone,String message);
    record Result(String providerId,boolean simulated) { }
}
