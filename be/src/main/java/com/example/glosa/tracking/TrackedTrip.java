package com.example.glosa.tracking;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.glosa.routing.PlannedRoute;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackedTrip {

    @Id
    @GeneratedValue
    private Long id;

    @Version
    private int updateCounter;

    @Column(nullable = false)
    private OffsetDateTime tripStartTime;
    private OffsetDateTime tripEndTime;

    @ManyToOne(optional = true)
    private PlannedRoute route;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL)
    @OrderBy("position.measurementTime")
    private List<TrackedLocation> locations = new ArrayList<>();

    public static TrackedTrip create(OffsetDateTime startTime, PlannedRoute route) {
        return new TrackedTrip(null, 0, startTime, null, route, null);
    }
}
