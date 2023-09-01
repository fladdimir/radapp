package com.example.glosa.tracking;

import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.stereotype.Service;

import com.example.glosa.routing.PlannedRoute.PlannedRouteRepository;
import com.example.glosa.tracking.TrackedLocation.TrackedPosition;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationTrackingService {

    private final TrackedTripRepository tripRepository;
    private final PlannedRouteRepository routeRepository;
    private final TrackedLocationRepository locationRepository;
    private final EntityManager em;

    @Transactional
    public long startNewTrip(OffsetDateTime startTime, String routeName) {

        var route = routeRepository.findByName(routeName).orElseThrow();

        var trip = TrackedTrip.create(startTime, route);
        trip = tripRepository.saveAndFlush(trip);

        return trip.getId();
    }

    @Transactional
    public TrackedTrip trackLocations(long tripId,
            Collection<TrackedPosition> positions) {

        // tbd: idempotency useful (filter already present measurement-times) ?

        var trip = tripRepository.findById(tripId).orElseThrow();

        var locations = positions.stream().map(p -> TrackedLocation.create(trip, p)).toList();
        locations = locationRepository.saveAll(locations);
        em.lock(trip, LockModeType.OPTIMISTIC_FORCE_INCREMENT); // prevent concurrent trip modification

        return trip;
    }

    @Transactional
    public TrackedTrip endTrip(long tripId, OffsetDateTime endTime) {

        var trip = tripRepository.findById(tripId).orElseThrow();

        trip.setTripEndTime(endTime);

        return trip;
    }

}
