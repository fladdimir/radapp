package com.example.glosa.trafficlightdata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TrafficLightPositionProviderTest {

    @Autowired
    TrafficLightPositionProvider tldp;

    @Test
    void test() {

        TrafficLightData tld = tldp.requestInfo("813_19");

        assertThat(tld.getDatastreamId()).isNotBlank();
        assertThat(tld.getLat()).isNotEqualTo(0);
        assertThat(tld.getLon()).isNotEqualTo(0);
    }

    @Test
    void test_cached() {

        var tld = tldp.toTrafficLightRoutePoint("813_19", true);

        assertThat(tld.getDatastreamId()).isNotBlank();
        assertThat(tld.getLat()).isNotEqualTo(0);
        assertThat(tld.getLon()).isNotEqualTo(0);
    }

}
