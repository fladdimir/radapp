package com.example.glosa.tracking;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "trip_id", "measurementTime" }, name = "unique_measurement_times_per_trip")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackedLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tracked_location_id_generator")
    @SequenceGenerator(name = "tracked_location_id_generator", sequenceName = "tracked_location_id_sequence", allocationSize = 10)
    private Long id;

    @ManyToOne(optional = false)
    private TrackedTrip trip;

    @CreationTimestamp
    private OffsetDateTime insertTime;

    @Embedded
    private TrackedPosition position;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Embeddable
    public static class TrackedPosition {

        @Column(nullable = false)
        private Double lat;
        @Column(nullable = false)
        private Double lng;
        private Double accuracy;

        private Double alt;
        private Double altAccuracy;

        private Double heading;
        private Double speed;

        @Column(nullable = false)
        private OffsetDateTime measurementTime;

        public TrackedPosition(Double lat, Double lng, OffsetDateTime measurementTime) {
            this.lat = lat;
            this.lng = lng;
            this.measurementTime = measurementTime;
        }
    }

    public static TrackedLocation create(TrackedTrip trip, TrackedPosition position) {
        return new TrackedLocation(null, trip, null, position);
    }

}
