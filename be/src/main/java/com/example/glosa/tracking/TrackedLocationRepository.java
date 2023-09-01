package com.example.glosa.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackedLocationRepository extends JpaRepository<TrackedLocation, Long> {

}
