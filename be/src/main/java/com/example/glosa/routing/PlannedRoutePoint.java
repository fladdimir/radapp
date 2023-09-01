package com.example.glosa.routing;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class PlannedRoutePoint {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Double lat;
    @Column(nullable = false)
    private Double lon;

    private String trafficLightId;

    private String datastreamId;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private PlannedRoute route;

    public PlannedRoutePoint(double lat, double lon) {
        this(null, lat, lon, null, null, null);
    }

    public PlannedRoutePoint(double lat, double lon, String trafficLightId, String datastreamId) {
        this(null, lat, lon, trafficLightId, datastreamId, null);
    }
}
