package com.example.glosa.trafficlightdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.glosa.routing.PlannedRoutePoint;
import com.example.glosa.trafficlightdata.TrafficLightData.TrafficLightDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Fetches static traffic-light information from the persistent cache or the API
 * (not via
 * API-proxy-service)
 */
@Service
@RequiredArgsConstructor
public class TrafficLightPositionProvider {

    private final RestTemplate restTemplate; // tbd: (async) webclient

    private final TrafficLightDataRepository trafficLightDataRepository;

    @Bean
    public static RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Transactional(value = TxType.REQUIRES_NEW) // commit to cache independent of caller tx
    public PlannedRoutePoint toTrafficLightRoutePoint(String trafficLightId, boolean useCache) {

        TrafficLightData tld = trafficLightDataRepository.findById(trafficLightId)
                .filter(cached -> useCache)
                .orElseGet(() -> trafficLightDataRepository.save(requestInfo(trafficLightId)));

        return new PlannedRoutePoint(tld.getLat(), tld.getLon(), tld.getTrafficLightId(), tld.getDatastreamId());
    }

    @SneakyThrows
    public TrafficLightData requestInfo(String trafficLightId) {

        final String baseUrl = "https://tld.iot.hamburg.de/v1.0/";

        final String iotId = "@iot.id";

        // 1.: https://tld.iot.hamburg.de/v1.0/Things?$filter=name+eq+'813_19''
        // -> 1 thing: value[0].@iot.id = thingId
        String thingRequest = baseUrl + "Things?$filter=name+eq+'" + trafficLightId + "'";
        JsonNode thingBody = get(thingRequest);
        String thingId = thingBody.elements().next().get(iotId).asText();
        if (thingId == null || thingId.isBlank())
            throw new IllegalStateException(thingBody.toPrettyString());

        // 2.:
        // Things(12382)/Datastreams?$filter=properties/layerName+eq+'primary_signal'
        // -> 1 Datastream: iotid = datastreamId + observedArea/coordinates[] Lon Lat
        String datastreamRequest = baseUrl + "Things(" + thingId
                + ")/Datastreams?$filter=properties/layerName+eq+'primary_signal'";
        JsonNode datastreamBody = get(datastreamRequest).elements().next();
        String datastreamId = datastreamBody.get(iotId).asText();
        if (datastreamId == null || thingId.isBlank())
            throw new IllegalStateException(datastreamBody.toPrettyString());
        var observedArea = datastreamBody.get("observedArea");

        var latlon = getLatLon(observedArea, trafficLightId, datastreamRequest);

        return new TrafficLightData(trafficLightId, latlon.lat, latlon.lon, datastreamId);
    }

    @Data
    @AllArgsConstructor
    static class LatLon {
        private double lat;
        private double lon;
    }

    private LatLon getLatLon(JsonNode observedArea, String trafficLightId, String request) {

        List<JsonNode> coords = new ArrayList<>();
        var type = observedArea.get("type").asText();
        var coordinates = observedArea.get("coordinates");
        if (type.equals("LineString")) {
            coordinates.elements().next().elements().forEachRemaining(coords::add);
        }
        if (type.equals("Point")) {
            coordinates.elements().forEachRemaining(coords::add);
        }
        if (coords.size() != 2)
            throw new IllegalArgumentException(trafficLightId + " coords formatting error: \n" + request);

        double lon = coords.get(0).asDouble();
        double lat = coords.get(1).asDouble();

        if (lon == 0 || lat == 0) {
            throw new IllegalArgumentException(trafficLightId + " coords not set: \n" + request);
        }

        return new LatLon(lat, lon);
    }

    @SneakyThrows
    private JsonNode get(String thingRequest) {
        var result = restTemplate.getForEntity(thingRequest, String.class);
        if (!result.getStatusCode().is2xxSuccessful())
            throw new IllegalStateException("" + result.getStatusCode());
        JsonNode body = new ObjectMapper().readTree(result.getBody());
        return Objects.requireNonNull(body.get("value"));
    }
}
