package com.example.glosa.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.glosa.calculation.RouteSnapper;
import com.example.glosa.routing.PlannedRoute.PlannedRouteRepository;
import com.example.glosa.trafficlightdata.TrafficLightPositionProvider;

class RouteCreatorUnitTest {

    private TrafficLightPositionProvider tlpp = mock(TrafficLightPositionProvider.class);
    private PlannedRouteRepository routeRepository = mock(PlannedRouteRepository.class);
    private RouteCreator creator = new RouteCreator(new RouteSnapper(), tlpp, routeRepository);

    @BeforeEach
    void beforeEach() {
        when(routeRepository.save(any())).thenAnswer(inv -> inv.getArguments()[0]);
    }

    @Test
    void test() throws Exception {

        var trafficLight1 = new PlannedRoutePoint(53.56350, 9.93163, "813_19", null);
        setup(trafficLight1);

        var trafficLight2 = new PlannedRoutePoint(53.56355, 9.93175, "813_20", null);
        setup(trafficLight2);

        var trafficLight3 = new PlannedRoutePoint(53.56372, 9.93270, "813_21", null);
        setup(trafficLight3);

        var result = creator.getOrCreate("test_1", Optional.empty());

        assertThat(result.getPoints()).hasSize(8);
        assertThat(result.getPoints().get(2)).isEqualTo(trafficLight1);
        assertThat(result.getPoints().get(3)).isEqualTo(trafficLight2);
        assertThat(result.getPoints().get(5)).isEqualTo(trafficLight3);
    }

    @Test
    void test_2() throws Exception {

        var trafficLight1 = new PlannedRoutePoint(51.3, 9.15, "813_19", null);
        setup(trafficLight1);

        var trafficLight2 = new PlannedRoutePoint(51.7, 9.16, "813_20", null);
        setup(trafficLight2);

        var trafficLight3 = new PlannedRoutePoint(52.5, 9.21, "813_21", null);
        setup(trafficLight3);

        var result = creator.getOrCreate("test_2", Optional.empty());

        assertThat(result.getPoints()).hasSize(8);
        assertThat(result.getPoints().get(2)).isEqualTo(trafficLight1);
        assertThat(result.getPoints().get(3)).isEqualTo(trafficLight2);
        assertThat(result.getPoints().get(5)).isEqualTo(trafficLight3);
    }

    private void setup(PlannedRoutePoint trafficLight) {
        when(tlpp.toTrafficLightRoutePoint(Mockito.eq(trafficLight.getTrafficLightId()), Mockito.anyBoolean())).thenReturn(trafficLight);
    }

}
