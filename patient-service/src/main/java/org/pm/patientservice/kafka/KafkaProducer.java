package org.pm.patientservice.kafka;

import org.pm.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class KafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String,byte[]> kafkaTemplate;
    public KafkaProducer(KafkaTemplate<String,byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(Patient patient){
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();
        try{
            kafkaTemplate.send("patients", event.toByteArray())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Message sent successfully: {}", result.getRecordMetadata());
                        } else {
                            log.error("Failed to send message", ex);
                        }
                    });
        }catch (Exception e){
            log.error("Error sending PatientCreated event: {}",event);
        }
    }

}
