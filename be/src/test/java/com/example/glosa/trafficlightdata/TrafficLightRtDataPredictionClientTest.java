package com.example.glosa.trafficlightdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.glosa.calculation.SpeedRecommender.TrafficLightRtDataDto;
import com.example.glosa.calculation.SpeedRecommender.TrafficLightSignalState;

@SpringBootTest
class TrafficLightRtDataPredictionClientTest {

    @Autowired
    TrafficLightRtDataPredictionService service;

    @Test
    @Disabled
    void test() {
        List<TrafficLightRtDataDto> input = IntStream.range(0, 16)
                .mapToObj(i -> new TrafficLightRtDataDto(OffsetDateTime.now(), TrafficLightSignalState.GREEN)).toList();

        var result = service.getPrediction("50850", input);

        assertThat(result).isNotEmpty().extracting(TrafficLightRtDataDto::getPhenomenonTime)
                .allMatch(OffsetDateTime.MIN::isBefore);
    }
}
