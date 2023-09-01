package com.example.glosa;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.glosa.calculation.SpeedRecommender;
import com.example.glosa.calculation.SpeedRecommender.NextTrafficLightDto;
import com.example.glosa.routing.PlannedRoute;
import com.example.glosa.tracking.LocationTrackingQueryService;
import com.example.glosa.tracking.LocationTrackingService;
import com.example.glosa.tracking.TrackedLocation;
import com.example.glosa.tracking.TrackedLocation.TrackedPosition;
import com.example.glosa.tracking.TrackedTrip;
import com.example.glosa.tracking.TrackedTripRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RestController
@RequestMapping("/tour")
@RequiredArgsConstructor
class TourController {

    private final TrackedTripRepository tripRepository;
    private final LocationTrackingService trackingService;
    private final TrackedPositionDtoMapper positionDtoMapper;
    private final SpeedRecommender routeSnapper;
    private final TrackedTripAllInfoDtoMapper tripInfoDtoMapper;
    private final LocationTrackingQueryService tourQueryService;
    private final TripDtoMapper tripDtoMapper;
    private final LocationTrackingService service;

    @GetMapping(path = "/{tourId}/route")
    PlannedRoute getRouteByTourId(@PathVariable("tourId") long tourId) {
        return tripRepository.findById(tourId).orElseThrow().getRoute();
    }

    @PostMapping(path = "/{tourId}/traffic-lights-data")
    NextTrafficLightDto getNextTrafficLightData(@PathVariable("tourId") long tourId,
            @RequestParam(value = "reachedIdx", defaultValue = "0") int reachedIdx,
            @Valid @RequestBody ValidList<TrackedPositionDto> positions) {

        var tour = trackingService.trackLocations(tourId,
                positions.stream().map(positionDtoMapper::toTrackedPosition).toList());

        var lastPosition = positions.get(positions.size() - 1);

        return routeSnapper.calculateSpeedRecommendation(tour.getRoute().getName(), lastPosition.getLat(),
                lastPosition.getLng(), reachedIdx, Optional.ofNullable(lastPosition.getSpeed()));
    }

    @PostMapping(path = "")
    @ResponseBody
    StartedTripDto startNewTrip(@RequestBody @Valid TimeDto tripStartData) {
        var tripId = service.startNewTrip(tripStartData.getTime(), tripStartData.getRouteName());
        return new StartedTripDto("" + tripId);
    }

    @PostMapping(path = "/{tourId}/end")
    void endTour(@PathVariable("tourId") long tourId, @RequestParam(name = "time") OffsetDateTime time) {
        trackingService.endTrip(tourId, time);
    }

    @GetMapping(path = "/{tourId}")
    TripWithPositionsDto getTripDetails(@PathVariable("tourId") long tourId) {

        var tripWithLocations = tourQueryService.getTripWithLocations(tourId);

        return tripDtoMapper.toTripWithPositionsDto(tripWithLocations);
    }

    @GetMapping(path = "")
    List<TrackedTripAllInfoDto> getTrips() {
        return getTours(Optional.empty());
    }

    @GetMapping(path = "/last-n")
    List<TrackedTripAllInfoDto> getLastTours() {
        Optional<Integer> maxN = Optional.of(25);
        return getTours(maxN);
    }

    private List<TrackedTripAllInfoDto> getTours(Optional<Integer> maxN) {
        var byCreationDateDesc = Comparator.comparing(TrackedTripAllInfoDto::getTripStartTime).reversed();
        Stream<TrackedTripAllInfoDto> stream = tripRepository.findAll().stream().map(tripInfoDtoMapper::toDto)
                .sorted(byCreationDateDesc);
        if (maxN.isPresent()) {
            stream = stream.limit(maxN.get());
        }
        List<TrackedTripAllInfoDto> list = stream.toList();
        return list;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TrackedTripAllInfoDto {
        private Long id;
        private OffsetDateTime tripStartTime;
        private String routeName;
    }

    @Mapper(componentModel = "spring")
    public interface TrackedTripAllInfoDtoMapper {

        @Mapping(source = "route.name", target = "routeName")
        TrackedTripAllInfoDto toDto(TrackedTrip dto);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TimeDto {

        @NotNull
        private OffsetDateTime time;

        @NotNull
        private String routeName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class StartedTripDto {

        private String id;
    }

    @Data
    static class ValidList<E> implements List<E> {
        // https://stackoverflow.com/a/55154919
        @Valid
        @NotEmpty
        @Delegate
        private List<E> list = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TrackedPositionDto {

        @Min(-90)
        @Max(90)
        @NotNull
        private Double lat;

        @Min(-180)
        @Max(180)
        @NotNull
        private Double lng;

        @PositiveOrZero
        private Double accuracy;

        private Double alt;

        private Double altAccuracy;

        private Double heading;

        @PositiveOrZero
        private Double speed;

        @NotNull
        private OffsetDateTime measurementTime;
    }

    @Mapper(componentModel = "spring")
    interface TrackedPositionDtoMapper {
        TrackedPosition toTrackedPosition(TrackedPositionDto dto);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TripWithPositionsDto {
        private long id;
        private OffsetDateTime tripStartTime;
        private OffsetDateTime tripEndTime;
        private List<TrackedPositionDto> locations;
    }

    @Mapper(componentModel = "spring")
    interface TripDtoMapper {

        TripWithPositionsDto toTripWithPositionsDto(TrackedTrip tripWithLocations);

        List<TrackedPositionDto> toTrackedPositionDtos(List<TrackedLocation> locations);

        @Mapping(target = ".", source = "position")
        TrackedPositionDto toTrackedPositionDto(TrackedLocation location);
    }

}
