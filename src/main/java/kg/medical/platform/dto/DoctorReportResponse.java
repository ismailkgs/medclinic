package kg.medical.platform.dto;
import java.time.Instant;
public record DoctorReportResponse(Long id,Long doctorId,int year,String status,long total,long created,long completed,long canceled,Instant generatedAt) { }
