package com.example.glosa.trafficlightdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * persistent caching of static traffic-light data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class TrafficLightData {

    @Id
    private String trafficLightId;

    @Column(nullable = false)
    private Double lat;
    @Column(nullable = false)
    private Double lon;

    @Column(unique = true, nullable = false)
    private String datastreamId;

    @Repository
    public static interface TrafficLightDataRepository extends JpaRepository<TrafficLightData, String> {
    }

}
