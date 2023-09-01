package com.example.glosa.trafficlightdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.glosa.calculation.SpeedRecommender.TrafficLightRtDataDto;

@SpringBootTest
class TrafficLightRtDataProviderTest {

    @Autowired
    TrafficLightRtDataProvider service;

    @Test
    @Disabled
    void test() {
        var result = service.getObservations("50850");
        assertThat(result).isNotEmpty().extracting(TrafficLightRtDataDto::getPhenomenonTime)
                .allMatch(OffsetDateTime.MIN::isBefore);
    }
}
