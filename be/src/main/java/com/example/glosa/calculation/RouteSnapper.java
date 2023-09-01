package com.example.glosa.calculation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Delegate;

/**
 * Calculates the nearest point on a given route of piece-wise linear
 * segments.
 */
@Service
public class RouteSnapper {

    /**
     * can be created from wsg84 or mercator coordinates
     */
    @Data
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter(value = AccessLevel.PRIVATE)
    public static class Point {
        private final double x;
        private final double y;

        private Double lon;
        private Double lat;
        private Double distanceToNext;

        private Coordinate toJtsCoordinate() {
            return new Coordinate(x, y);
        }

        private org.locationtech.jts.geom.Point toJts() {
            return new GeometryFactory().createPoint(toJtsCoordinate());
        }

        private static Point fromJts(Coordinate c) {
            return new Point(c.x, c.y, null, null, null);
        }

        public static Point ofLatLon(double lat, double lon) {
            return ofLatLon(lat, lon, null);
        }

        public static Point ofLatLon(double lat, double lon, Double distanceToNext) {
            return new Point(lonToMercatorX(lon), latToMercatorY(lat), lon, lat, distanceToNext);
        }

        public static Point ofMercator(double x, double y) {
            return new Point(x, y, null, null, null);
        }

        public static Point ofMercator(double x, double y, double distanceToNext) {
            return new Point(x, y, null, null, distanceToNext);
        }

        double getLon() {
            if (lon == null) {
                lon = xToLon(x);
            }
            return lon;
        }

        double getLat() {
            if (lat == null) {
                lat = yToLat(y);
            }
            return lat;
        }

    }

    /**
     * a segment is defined by 2 points
     */
    @Data
    @AllArgsConstructor
    public static class Segment {
        private final Point start;
        private final Point end;

        private LineString toJts() {
            return new GeometryFactory()
                    .createLineString(new Coordinate[] { start.toJtsCoordinate(), end.toJtsCoordinate() });
        }
    }

    @Data
    @AllArgsConstructor
    public static class NearestSegmentResult {
        private final Segment segment;
        private final Point nearestPointOnSegment;
        private final double distance;
    }

    @Data
    @AllArgsConstructor
    public static class ReachedWaypointResult {
        @Delegate
        private final NearestSegmentResult csr;
        private int reachedWaypointIdx;
    }

    @Data
    @Builder
    public static class RouteSnappingParameter {
        private final List<Point> route;
        private final Point location;
        private final String context;
        @Builder.Default
        private final Double distanceFromRouteThresholdM = null;
        @Builder.Default
        private final Integer previouslyReachedWaypointIdx = null;

        public Optional<Double> getDistanceFromRouteThresholdM() {
            return Optional.ofNullable(distanceFromRouteThresholdM);
        }

        public Optional<Integer> getPreviouslyReachedWaypointIdx() {
            return Optional.ofNullable(previouslyReachedWaypointIdx);
        }
    }

    public double getLengthMercatorM(List<Point> routePoints) {
        return getLength(routePoints, RouteSnapper::euklideanDistance);
    }

    public double getLengthHaversineM(List<Point> routePoints) {
        return getLength(routePoints, RouteSnapper::haversineDistanceM);
    }

    private double getLength(List<Point> routePoints, BiFunction<Point, Point, Double> distanceFunction) {
        return IntStream.range(0, routePoints.size() - 1)
                .mapToDouble(i -> {
                    Point start = routePoints.get(i);
                    return Optional.ofNullable(start.distanceToNext)
                            .orElseGet(() -> distanceFunction.apply(start, routePoints.get(i + 1)));
                })
                .sum();
    }

    private static double euklideanDistance(Point p1, Point p2) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public ReachedWaypointResult insert(RouteSnappingParameter params) {

        var route = params.getRoute();
        var location = params.getLocation();
        var distanceFromRouteThresholdM = params.getDistanceFromRouteThresholdM();
        var previouslyReachedWaypoint = params.getPreviouslyReachedWaypointIdx();

        var startIdx = previouslyReachedWaypoint.orElse(0);

        var reached = calcReachedWaypointIdx(route, location, startIdx);

        if (distanceFromRouteThresholdM.map(max -> max < reached.getDistance()).orElse(false)) {
            throw new IllegalArgumentException(
                    "distance to segment larger than threshold. context: " + params.getContext() + "\nlocation: "
                            + location
                            + "\nthreshold: " + distanceFromRouteThresholdM + "\nsegment: " + reached);
        }

        route.add(reached.reachedWaypointIdx + 1, location);
        return reached;
    }

    ReachedWaypointResult calcReachedWaypointIdx(List<Point> route, Point location, int reachedWaypointIdx) {

        var subList = route.subList(reachedWaypointIdx, route.size());
        var result = calcReachedWaypointIdx(subList, location);
        result.reachedWaypointIdx += reachedWaypointIdx;
        return result;
    }

    public ReachedWaypointResult calcReachedWaypointIdx(List<Point> route, Point location) {

        // calc distance from each linear segment
        NearestSegmentResult nearestSegment = new NearestSegmentResult(null, null, Double.MAX_VALUE);
        int reachedWaypoint = -1;
        for (int i = 0; i < (route.size() - 1); i++) {
            var segment = new Segment(route.get(i), route.get(i + 1));
            var segmentDistance = calcDistance(segment, location);
            if (segmentDistance.distance < nearestSegment.distance) {
                nearestSegment = segmentDistance;
                reachedWaypoint = i;
            } else { // getting further away again
                if (nearestSegment.distance < 25 && segmentDistance.distance > 50) {
                    break; // seems we cant get closer, stop before iterating the entire route
                }
            }
        }
        Objects.requireNonNull(nearestSegment.segment);

        // recalculate actually correct distance
        var correctDistance = haversineDistanceM(location, nearestSegment.nearestPointOnSegment);
        nearestSegment = new NearestSegmentResult(nearestSegment.segment, nearestSegment.nearestPointOnSegment,
                correctDistance);

        return new ReachedWaypointResult(nearestSegment, reachedWaypoint);
    }

    NearestSegmentResult calcDistance(Segment s, Point p) {

        var dop = new DistanceOp(s.toJts(), p.toJts());

        var distance = dop.distance();
        var nearestPoints = dop.nearestPoints();

        return new NearestSegmentResult(s, Point.fromJts(nearestPoints[0]), distance);
    }

    private static double lonToMercatorX(double lon) {
        return lon * 20037508.34 / 180;
    }

    private static double latToMercatorY(double lat) {
        var y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
        y = y * 20037508.34 / 180;
        return y;
    }

    private static double xToLon(double x) {
        return x / 20037508.34 * 180;
    }

    private static double yToLat(double y) {
        var lat = (y / 20037508.34) * 180;
        lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180)) - Math.PI /
                2);
        return lat;
    }

    static double haversineDistanceM(Point p1, Point p2) {
        return haversineDistanceM(p1.getLat(), p1.getLon(), p2.getLat(), p2.getLon());
    }

    private static double haversineDistanceM(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        final int R = 6371 * 1000;
        return R * c;
    }

}
