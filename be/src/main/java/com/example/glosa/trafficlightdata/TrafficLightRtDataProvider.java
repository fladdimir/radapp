package com.example.glosa.trafficlightdata;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.glosa.calculation.SpeedRecommender.TrafficLightRtDataDto;
import com.example.glosa.calculation.SpeedRecommender.TrafficLightSignalState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Service
@RequiredArgsConstructor
public class TrafficLightRtDataProvider {

    private final RestTemplate restTemplate; // tbd: (async) webclient

    private static final int N = 16;

    private static final String BASE_URL = "http://localhost:3000";

    @SneakyThrows
    public List<TrafficLightRtDataDto> getObservations(String datastreamId) {

        String url = BASE_URL + "/tld/" + datastreamId + "?nvalues=" + N;

        var results = get(url).elements();
        Iterable<JsonNode> iterable = () -> results;
        Stream<JsonNode> elements = StreamSupport.stream(iterable.spliterator(), false);
        return elements.map(this::toDto).toList();
    }

    // TODO: use TrafficLightRtDataPredictionDtoMapper
    private TrafficLightRtDataDto toDto(JsonNode node) {
        return new TrafficLightRtDataDto(
                OffsetDateTime.parse(node.get("phenomenonTime").asText()),
                TrafficLightSignalState.get(node.get("result").asInt(-1)));
    }

    @SneakyThrows
    private JsonNode get(String thingRequest) {
        var result = restTemplate.getForEntity(thingRequest, String.class);
        if (!result.getStatusCode().is2xxSuccessful())
            throw new IllegalStateException("" + result.getStatusCode());
        JsonNode body = new ObjectMapper().readTree(result.getBody());
        return Objects.requireNonNull(body);
    }

}
