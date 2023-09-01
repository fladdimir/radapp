package com.example.glosa.routing;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.example.glosa.calculation.RouteSnapper;
import com.example.glosa.calculation.RouteSnapper.Point;
import com.example.glosa.calculation.RouteSnapper.ReachedWaypointResult;
import com.example.glosa.calculation.RouteSnapper.RouteSnappingParameter;
import com.example.glosa.routing.PlannedRoute.PlannedRouteRepository;
import com.example.glosa.trafficlightdata.TrafficLightPositionProvider;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Creates (and persists) a named route from a gpx-file and a list of
 * traffic-light-IDs. The* locations of the traffic-lights are resolved via
 * (cached) API calls and they are inserted as special waypoints into the
 * route.
 */
@Service
@RequiredArgsConstructor
class RouteCreator {

    private final RouteSnapper routeSnappingCalculator;
    private final TrafficLightPositionProvider trafficLightPositionProvider;
    private final PlannedRouteRepository routeRepository;

    @Transactional
    PlannedRoute getOrCreate(String name, Optional<Double> distanceFromRouteThreshold) {

        var existing = routeRepository.findByName(name);
        if (existing.isPresent())
            return existing.get();

        var gpx = RouteFileUtil.read(name + ".route.gpx");
        var trafficLightIds = RouteFileUtil.readLines(name + ".traffic_lights.txt");

        return createRoute(readGpxFile(gpx), trafficLightIds, distanceFromRouteThreshold, name);
    }

    @Transactional
    void deleteByName(String name) {
        routeRepository.delete(routeRepository.findByName(name).get());
    }

    private PlannedRoute createRoute(List<PlannedRoutePoint> plainRoute, List<String> trafficLightIds,
            Optional<Double> distanceFromRouteThreshold, String name) {

        plainRoute = new ArrayList<>(plainRoute);
        var trafficLights = toRoutePoints(trafficLightIds);

        var route = createRouteIntern(plainRoute, trafficLights, distanceFromRouteThreshold);
        route.setName(name);
        return routeRepository.save(route);
    }

    private PlannedRoute createRouteIntern(List<PlannedRoutePoint> plainRoute,
            List<PlannedRoutePoint> trafficLights, Optional<Double> distanceFromRouteThreshold) {
        List<Point> routePoints = new ArrayList<>(
                plainRoute.stream().map(prp -> Point.ofLatLon(prp.getLat(), prp.getLon())).toList());

        // TODO: remove unused points on (almost) linear segments

        var mergedRoute = new ArrayList<>(plainRoute);
        Optional<Integer> previouslyReachedWaypoint = Optional.empty();

        for (var trafficLight : trafficLights) {

            var params = RouteSnappingParameter.builder()
                    .route(routePoints)
                    .location(Point.ofLatLon(trafficLight.getLat(), trafficLight.getLon()))
                    .context(trafficLight.getTrafficLightId());

            distanceFromRouteThreshold.ifPresent(params::distanceFromRouteThresholdM);
            previouslyReachedWaypoint.ifPresent(params::previouslyReachedWaypointIdx);

            ReachedWaypointResult result = routeSnappingCalculator.insert(params.build());
            int insertionIdx = result.getReachedWaypointIdx() + 1;
            mergedRoute.add(insertionIdx, trafficLight);

            // keep read-order, even when traffic-lights belong to the same crossing
            previouslyReachedWaypoint = Optional.of(insertionIdx);
        }

        var route = new PlannedRoute();
        route.getPoints().addAll(mergedRoute);
        mergedRoute.forEach(p -> p.setRoute(route));
        return route;
    }

    private List<PlannedRoutePoint> toRoutePoints(List<String> ids) {
        return ids.stream().map(id -> trafficLightPositionProvider.toTrafficLightRoutePoint(id, true)).toList();
    }

    @SneakyThrows
    static List<PlannedRoutePoint> readGpxFile(String gpxData) {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(new InputSource(new StringReader(gpxData)));
        NodeList trk = document.getElementsByTagName("trk");
        if (trk.getLength() > 1)
            throw new IllegalArgumentException();

        NodeList trkpts = document.getElementsByTagName("trkpt");
        if (trkpts.getLength() == 0)
            throw new IllegalArgumentException();

        return IntStream.range(0, trkpts.getLength()).mapToObj(trkpts::item)
                .map(trkpt -> new PlannedRoutePoint(getDoubleAttribute(trkpt, "lat"), getDoubleAttribute(trkpt, "lon")))
                .distinct()
                .toList();

    }

    private static double getDoubleAttribute(Node node, String attribute) {
        return Double.parseDouble(node.getAttributes().getNamedItem(attribute).getNodeValue());
    }

}
