package com.example.glosa.calculation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.glosa.calculation.SpeedRecommender.RouteLoader;
import com.example.glosa.routing.PlannedRoute;
import com.example.glosa.routing.PlannedRoute.PlannedRouteRepository;
import com.example.glosa.routing.PlannedRoutePoint;
import com.example.glosa.trafficlightdata.TrafficLightRtDataPredictionService;
import com.example.glosa.trafficlightdata.TrafficLightRtDataProvider;

class RouteSnapperTest {

        private PlannedRouteRepository repo = mock(PlannedRouteRepository.class);
        private RouteLoader routeDataCache = new RouteLoader(repo);
        private TrafficLightRtDataProvider tldp = mock(TrafficLightRtDataProvider.class);
        private TrafficLightRtDataPredictionService predictionService = mock(TrafficLightRtDataPredictionService.class);
        private SpeedRecommender routeSnapper = new SpeedRecommender(routeDataCache, new RouteSnapper(), tldp,
                        predictionService);

        @CsvSource({
                        "0, t1, 3",
                        "0.5, t1, 2.5",
                        "1, t1, 2",
                        "1.5, t1, 1.5",
                        "2.9, t1, 0.1",
                        "3.5, t2, 11.5",
                        "6.1, t2, 8.9",
        })
        @ParameterizedTest
        void test_next_trafficLight(double lat, String tl, double tlDistLat) {

                // double distPerDegreeLat = 111_000;

                String routeName = "routeName";
                PlannedRoute route = new PlannedRoute();
                route.setName(routeName);
                route.setPoints(List.of(
                                new PlannedRoutePoint(0, 0),
                                new PlannedRoutePoint(1, 0),
                                new PlannedRoutePoint(3, 0, "t1", "tlds_1"),
                                new PlannedRoutePoint(6, 0),
                                new PlannedRoutePoint(10, 0),
                                new PlannedRoutePoint(15, 0, "t2", "tlds_2")));
                when(repo.findByName(routeName)).thenReturn(Optional.of(route));
                routeSnapper.NOTIFICATION_DISTANCE_M = Double.MAX_VALUE;

                // var result = routeSnapper.distanceToNextTrafficLight(routeName, lat, 0, 0);
                // assertThat(result.getDistanceM()).isCloseTo(tlDistLat * distPerDegreeLat,
                // withinPercentage(1));
                // assertThat(result.getTrafficLightId()).isEqualTo(tl);
        }

}
