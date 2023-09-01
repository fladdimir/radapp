package com.example.glosa.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.glosa.tracking.TrackedLocation.TrackedPosition;

@SpringBootTest
class LocationTrackingLifecycleTest {

    @Autowired
    private LocationTrackingService service;

    @Autowired
    private TrackedTripRepository tripRepository;

    @Autowired
    private TrackedLocationRepository locationRepository;

    @Test
    void test_happy_path() {

        // start trip
        var tripId = service.startNewTrip(OffsetDateTime.now(), "route_3");
        assertThat(tripId).isNotNull();

        // save some positions
        service.trackLocations(tripId, List.of(new TrackedPosition(-1.0, 1.5,
                OffsetDateTime.now())));
        service.trackLocations(tripId, List.of(new TrackedPosition(2.0, 2.5,
                OffsetDateTime.now()),
                new TrackedPosition(3.0, 3.5, OffsetDateTime.now())));

        // end trip
        service.endTrip(tripId, OffsetDateTime.now());

        // some assertions
        assertThat(tripRepository.findById(tripId)).isPresent();
        assertThat(tripRepository.findWithLocationsById(tripId).get().getLocations()).hasSize(3);

        // start trip 2
        var tripId2 = service.startNewTrip(OffsetDateTime.now(), "route_3");
        assertThat(tripId2).isNotNull();

        // save some positions
        service.trackLocations(tripId2, List.of(new TrackedPosition(2.0, 2.5,
                OffsetDateTime.now()),
                new TrackedPosition(3.0, -3.5, OffsetDateTime.now())));

        // end trip 2
        service.endTrip(tripId2, OffsetDateTime.now());

        // some assertions
        assertThat(tripRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(locationRepository.count()).isGreaterThanOrEqualTo(5);
        assertThat(tripRepository.findById(tripId2)).isPresent();
        assertThat(tripRepository.findWithLocationsById(tripId2).get().getLocations()).hasSize(2);
    }

    @Test
    void test_some_unhappy_stuff() {

        // start trip
        OffsetDateTime tripStart = OffsetDateTime.now();
        var tripId = service.startNewTrip(tripStart, "route_3");
        assertThat(tripId).isNotNull();

        // save some positions
        OffsetDateTime measurementTime1 = OffsetDateTime.now().plusMinutes(1);
        OffsetDateTime measurementTime2 = OffsetDateTime.now().plusMinutes(2);
        service.trackLocations(tripId, List.of(
                new TrackedPosition(-1.0, 1.5, measurementTime1),
                new TrackedPosition(-1.0, 1.5, measurementTime2)));

        // existing measurement_time
        assertThatThrownBy(() -> service.trackLocations(tripId,
                List.of(
                        new TrackedPosition(-1.0, 1.5, measurementTime1))))
                .isInstanceOf(Exception.class);

        // same measurement_times
        OffsetDateTime measurementTime3 = OffsetDateTime.now().plusMinutes(3);
        assertThatThrownBy(
                () -> service.trackLocations(tripId, List.of(
                        new TrackedPosition(-1.0, 1.5, measurementTime3),
                        new TrackedPosition(-1.0, 1.5, measurementTime3))))
                .isInstanceOf(Exception.class);

        // // measurement_time too early
        // assertThatThrownBy(() -> service.trackLocations(tripId,
        //         List.of(
        //                 new TrackedPosition(-1.0, 1.5, tripStart.minusSeconds(1)))))
        //         .isInstanceOf(Exception.class);

        // end trip before start
        // assertThatThrownBy(() -> service.endTrip(tripId, tripStart.minusSeconds(1)))
        //         .isInstanceOf(Exception.class);

        // end trip before last measurement
        // assertThatThrownBy(() -> service.endTrip(tripId,
        //         measurementTime2.minusSeconds(1)))
        //         .isInstanceOf(Exception.class);

    }

}
