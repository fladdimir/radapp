package com.example.glosa.calculation;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.glosa.calculation.RouteSnapper.Point;
import com.example.glosa.routing.PlannedRoute;
import com.example.glosa.routing.PlannedRoute.PlannedRouteRepository;
import com.example.glosa.trafficlightdata.TrafficLightRtDataPredictionService;
import com.example.glosa.trafficlightdata.TrafficLightRtDataProvider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpeedRecommender {

    private final RouteLoader routeDataCache;
    private final RouteSnapper snappingService;
    private final TrafficLightRtDataProvider trafficLightDataProvider;
    private final TrafficLightRtDataPredictionService trafficLightPredictionService;

    public double NOTIFICATION_DISTANCE_M = 10_000; // more than 1km does probably not make much sense

    public NextTrafficLightDto calculateSpeedRecommendation(String routeName, double lat, double lon, int reachedIdx,
            Optional<Double> currentSpeed) {

        var snapped = snap(routeName, lat, lon, reachedIdx);

        var route = routeDataCache.getRoute(routeName);

        var nextTlIdx = IntStream.range(
                snapped.getReachedIdx() + 1, route.getPoints().size())
                .filter(i -> route.getPoints().get(i).getTrafficLightId() != null).findFirst();

        if (!nextTlIdx.isPresent()) { // no traffic-light ahead
            return new NextTrafficLightDto(null, null, snapped, null, null, null);
        }

        var routePoints = routeDataCache.getRoutePoints(routeName);
        var relevantPart = new ArrayList<>(routePoints.subList(snapped.getReachedIdx(), nextTlIdx.getAsInt() + 1));
        relevantPart.set(0, Point.ofLatLon(snapped.getLat(), snapped.getLon()));
        var distanceM = snappingService.getLengthHaversineM(relevantPart);

        var trafficLight = route.getPoints().get(nextTlIdx.getAsInt());
        if (distanceM > NOTIFICATION_DISTANCE_M) {
            return new NextTrafficLightDto(trafficLight.getTrafficLightId(), distanceM, snapped, null, null, null);
        }

        var trafficLightData = trafficLightDataProvider.getObservations(trafficLight.getDatastreamId());

        var predictions = trafficLightPredictionService.getPrediction(trafficLight.getTrafficLightId(),
                trafficLightData);

        Function<TrafficLightRtDataDto, OffsetDateTime> dateExtractor = dto -> dto.getPhenomenonTime();
        Comparator<TrafficLightRtDataDto> byTime = Comparator.comparing(dateExtractor);
        var tldWithPredictions = Stream.concat(trafficLightData.stream(), predictions.stream())
                .sorted(byTime.reversed())
                .toList();

        Optional<RecommendationData> necessarySpeed = calcNecessarySpeed(distanceM, tldWithPredictions);
        Optional<Recommendation> recommendation = Optional.empty();
        if (necessarySpeed.isPresent() && currentSpeed.isPresent() && currentSpeed.get() > 0) {
            recommendation = Optional.of(getRecommendation(currentSpeed.get(), necessarySpeed.get()));
        }

        return new NextTrafficLightDto(trafficLight.getTrafficLightId(), distanceM, snapped, tldWithPredictions,
                necessarySpeed.map(rd -> rd.getNecessarySpeed()).orElse(null), recommendation.orElse(null));
    }

    private static final double MAX_SPEED_MS = 28 / 3.6; /* km/h */

    private Optional<RecommendationData> calcNecessarySpeed(double distanceM,
            List<TrafficLightRtDataDto> tldWithPredictions) {
        OffsetDateTime now = OffsetDateTime.now();
        var usefulStates = Set.of(TrafficLightSignalState.GREEN, TrafficLightSignalState.RED);
        Function<TrafficLightRtDataDto, OffsetDateTime> dateExtractor = dto -> dto.getPhenomenonTime();
        Comparator<TrafficLightRtDataDto> byTimeAsc = Comparator.comparing(dateExtractor);
        tldWithPredictions = tldWithPredictions.stream().sorted(byTimeAsc).toList();
        Stream<Optional<RecommendationData>> rds = tldWithPredictions.stream()
                .filter(rtd -> usefulStates.contains(rtd.getResult()))
                .map(rtd -> {

                    OffsetDateTime time = rtd.getPhenomenonTime();
                    long timeUntil = ChronoUnit.SECONDS.between(now, time);

                    if (timeUntil <= 0)
                        return Optional.empty();

                    double necessarySpeed = distanceM / timeUntil;

                    if (necessarySpeed > MAX_SPEED_MS)
                        return Optional.empty();

                    return Optional.of(new RecommendationData(necessarySpeed, rtd.getResult()));
                });
        return rds.filter(Optional::isPresent).map(Optional::get).findFirst();
    }

    @Data
    @AllArgsConstructor
    static class RecommendationData {
        private final double necessarySpeed;
        private final TrafficLightSignalState nextState;
    }

    private Recommendation getRecommendation(double currentSpeed,
            RecommendationData rec) {

        double necessarySpeed = rec.getNecessarySpeed();

        double diff = necessarySpeed - currentSpeed;

        if (rec.getNextState().equals(TrafficLightSignalState.GREEN)) {
            // next phase: green --> at most necessarySpeed
            if (diff <= 0) // faster than necessary
                return Recommendation.SLOWER; // avoid waiting
            if (diff < 1) // almost at necessary speed to be there right when it changes
                // tbd: should actually depend on distance, speed, and green-phase length
                return Recommendation.KEEP;
            // else:
            return Recommendation.QUICKER; // better to accelerate

        } else if (rec.getNextState().equals(TrafficLightSignalState.RED)) {
            // next phase: red --> at least necessarySpeed
            if (diff <= -1) // a lot faster than necessary
                return Recommendation.KEEP; // keep catching the green
            // else:
            return Recommendation.QUICKER;

        } else {
            throw new IllegalStateException();
        }
    }

    private SnappedToDto snap(String name, double lat, double lon, int reachedIdx) {
        var route = routeDataCache.getRoutePoints(name);
        var snapped = snappingService.calcReachedWaypointIdx(
                route, Point.ofLatLon(lat, lon),
                Math.max(reachedIdx - 5, 0)); // limited backwards search
        return new SnappedToDto(
                snapped.getNearestPointOnSegment().getLat(),
                snapped.getNearestPointOnSegment().getLon(),
                snapped.getReachedWaypointIdx());
    }

    @Service
    @RequiredArgsConstructor
    static class RouteLoader {

        private final PlannedRouteRepository repository;

        @Cacheable("routePoints")
        List<Point> getRoutePoints(String name) {

            var dbData = repository.findByName(name).orElseThrow().getPoints();
            List<Point> mapped = dbData.stream().map(prp -> Point.ofLatLon(prp.getLat(), prp.getLon())).toList();

            // calc distance to next point (cached)
            var nextPoints = mapped.subList(1, mapped.size()).iterator();
            mapped.subList(0, mapped.size() - 1).stream()
                    .forEach(p -> p.setDistanceToNext(RouteSnapper.haversineDistanceM(p, nextPoints.next())));

            return Collections.unmodifiableList(mapped);
        }

        @Cacheable("route")
        PlannedRoute getRoute(String name) {
            return repository.findByName(name).orElseThrow();
        }
    }

    @Data
    @RequiredArgsConstructor
    public static class NextTrafficLightDto {
        private final String trafficLightId;
        private final Double distanceM;
        private final SnappedToDto snappedTo;
        private final List<TrafficLightRtDataDto> data;
        private final Double necessarySpeed;
        private final Recommendation recommendation;
    }

    enum Recommendation {
        SLOWER, KEEP, QUICKER
    }

    @Data
    @RequiredArgsConstructor
    static class SnappedToDto {
        private final double lat;
        private final double lon;
        private final int reachedIdx;
    }

    @Data
    @RequiredArgsConstructor
    public static class TrafficLightRtDataDto {
        private final OffsetDateTime phenomenonTime;
        private final TrafficLightSignalState result;
    }

    public enum TrafficLightSignalState {
        // https://tld.iot.hamburg.de/v1.1/Datastreams(50850)
        // 0=dark,1=red,2=amber,3=green,4=red-amber,5=amber-flashing,6=green-flashing,9=unknown
        DARK, RED, AMBER, GREEN, RED_AMBER, AMBER_FLASHING, GREEN_FLASHING, UNKNOWN;

        public static TrafficLightSignalState get(int value) {
            if (value == 9)
                return UNKNOWN;
            return TrafficLightSignalState.values()[value];
        }

        public int get() {
            if (this.equals(UNKNOWN))
                return 9;
            return this.ordinal();
        }
    }

}
