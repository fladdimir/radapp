package com.example.glosa.tracking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationTrackingQueryService {

    private final TrackedTripRepository tripRepository;

    @Transactional(readOnly = true)
    public TrackedTrip getTripWithLocations(long tripId) {
        return tripRepository.findWithLocationsById(tripId).orElseThrow();
    }

}
