package com.example.glosa.trafficlightdata;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.mapstruct.Mapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.glosa.calculation.SpeedRecommender;
import com.example.glosa.calculation.SpeedRecommender.TrafficLightRtDataDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Service
@RequiredArgsConstructor
public class TrafficLightRtDataPredictionService {

    private final RestTemplate restTemplate; // tbd: (async) webclient

    private static final String BASE_URL = "http://localhost:5001";

    private final TrafficLightRtDataPredictionDtoMapper mapper;

    public List<TrafficLightRtDataDto> getPrediction(String trafficLightId, List<TrafficLightRtDataDto> input) {

        var result = post(BASE_URL + "/tlp-prediction/" + trafficLightId, mapper.toDto(input));

        return mapper.fromDto(result);
    }

    @SneakyThrows
    private List<TrafficLightRtDataPredictionDto> post(String url, List<TrafficLightRtDataPredictionDto> input) {
        var result = restTemplate.postForEntity(url, input, String.class);
        if (!result.getStatusCode().is2xxSuccessful())
            throw new IllegalStateException("" + result.getStatusCode());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        var resultBody = objectMapper.readValue(result.getBody(), TrafficLightRtDataPredictionDto[].class);
        return Stream.of(resultBody).toList();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TrafficLightRtDataPredictionDto {
        OffsetDateTime phenomenonTime;
        int result;
    }

    @Mapper(componentModel = "spring")
    interface TrafficLightRtDataPredictionDtoMapper {

        List<TrafficLightRtDataPredictionDto> toDto(List<TrafficLightRtDataDto> fromList);

        List<TrafficLightRtDataDto> fromDto(List<TrafficLightRtDataPredictionDto> fromList);

        TrafficLightRtDataPredictionDto map(TrafficLightRtDataDto fromDto);

        TrafficLightRtDataDto map(TrafficLightRtDataPredictionDto fromDto);

        default int map(SpeedRecommender.TrafficLightSignalState value) {
            return value.get();
        }

        default SpeedRecommender.TrafficLightSignalState map(int value) {
            return SpeedRecommender.TrafficLightSignalState.get(value);
        }
    }

}
