package com.example.glosa.tracking;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackedTripRepository extends JpaRepository<TrackedTrip, Long> {

    @EntityGraph(attributePaths = "locations")
    Optional<TrackedTrip> findWithLocationsById(long tripId);

}
