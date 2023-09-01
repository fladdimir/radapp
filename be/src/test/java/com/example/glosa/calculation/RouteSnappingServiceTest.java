package com.example.glosa.calculation;

import static com.example.glosa.calculation.RouteSnapper.Point.ofMercator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.withinPercentage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.glosa.calculation.RouteSnapper.Point;
import com.example.glosa.calculation.RouteSnapper.RouteSnappingParameter;

class RouteSnappingServiceTest {

        private RouteSnapper service = new RouteSnapper();

        @CsvSource({
                        "0,1, 0",
                        "2,1, 1",
                        "2,3, 2",
                        "3,4, 2",
                        "-1,-1, 0",
                        "0,0, 0",
                        "1,1, 0",
                        "0.9,0.9, 0",
                        "1.9,1.9, 1",
                        "2,0, 0",
                        "3,1, 1",
        })
        @ParameterizedTest
        void test_origin_line(double x, double y, int expectedResult) {

                var straightLine_1_1 = List.of(
                                ofMercator(0, 0),
                                ofMercator(1, 1),
                                ofMercator(2, 2),
                                ofMercator(3, 3));

                var location = ofMercator(x, y);

                var result = service.calcReachedWaypointIdx(straightLine_1_1, location);

                assertThat(result.getReachedWaypointIdx()).isEqualTo(expectedResult);
        }

        @CsvSource({
                        "-1,0, 0",
                        "0,0, 0",
                        "0.9,1, 0",
                        "1,-1, 0",
                        "1.1,-1, 1",
                        "2.1,0.1, 2",
                        "4,1, 2",
        })
        @ParameterizedTest
        void test_horizontal_line(double x, double y, int expectedResult) {

                var straightLine_1_0 = List.of(
                                ofMercator(0, 0),
                                ofMercator(1, 0),
                                ofMercator(2, 0),
                                ofMercator(3, 0));

                var location = ofMercator(x, y);

                var result = service.calcReachedWaypointIdx(straightLine_1_0, location);

                assertThat(result.getReachedWaypointIdx()).isEqualTo(expectedResult);
        }

        @CsvSource({
                        "0,-1, 0",
                        "0,0.5, 0",
                        "0,1, 0",
                        "0,1.5, 1",
                        "10,2.5, 2",
                        "0,4, 2",
        })
        @ParameterizedTest
        void test_vertical_line(double x, double y, int expectedResult) {

                var straightLine_0_1 = List.of(
                                ofMercator(0, 0),
                                ofMercator(0, 1),
                                ofMercator(0, 2),
                                ofMercator(0, 3));

                var location = ofMercator(x, y);

                var result = service.calcReachedWaypointIdx(straightLine_0_1, location);

                assertThat(result.getReachedWaypointIdx()).isEqualTo(expectedResult);
        }

        @Test
        void test_insert() {

                var straightLine_1_1 = new ArrayList<>(Arrays.asList(
                                ofMercator(0, 0),
                                ofMercator(1, 0),
                                ofMercator(2, 0),
                                ofMercator(3, 0)));

                var location = ofMercator(0.1, 0.1);

                var params = RouteSnappingParameter.builder()
                                .route(straightLine_1_1).location(location);

                var result = service.insert(params.build());

                assertThat(straightLine_1_1.get(1)).isEqualTo(location);
                assertThat(result.getReachedWaypointIdx()).isEqualTo(0);
        }

        @Test
        void test_insert_withHint() {

                var route = Arrays.asList(
                                ofMercator(0, 0),
                                ofMercator(1, 0),
                                ofMercator(2, 0),
                                ofMercator(2, 1),
                                ofMercator(2, 2));

                Point location = ofMercator(1.5, 0.4);
                var params = RouteSnappingParameter.builder()
                                .location(location);

                var route1 = new ArrayList<>(route);
                var result = service.insert(params.route(route1).build());
                assertThat(result.getReachedWaypointIdx()).isEqualTo(1);
                assertThat(route1.get(2)).isEqualTo(location);

                var route2 = new ArrayList<>(route);
                var result2 = service.insert(params.route(route2).previouslyReachedWaypointIdx(2).build());
                assertThat(result2.getReachedWaypointIdx()).isEqualTo(2);
                assertThat(route2.get(3)).isEqualTo(location);
        }

        @Test
        void test_distance_mercator_haversine_1() {

                List<Point> route = List.of(
                                ofMercator(0, 0), ofMercator(1, 0), ofMercator(1, 1), ofMercator(3, 1));

                assertThat(service.getLengthMercatorM(route)).isEqualTo(4);
                // close to center:
                assertThat(service.getLengthHaversineM(route)).isCloseTo(4, within(.01));

                // further north:
                List<Point> route2 = List.of(
                                Point.ofLatLon(53.5623056922887, 9.94879432455777),
                                Point.ofLatLon(53.57036870513437, 9.956098175907377));
                double expected = 1019.452368; // m

                double haversine2 = service.getLengthHaversineM(route2);
                assertThat(haversine2).isCloseTo(expected, within(2.));

                double mercator2 = service.getLengthMercatorM(route2);
                assertThat(mercator2).isNotCloseTo(haversine2, withinPercentage(50));
        }

        @Test
        void test_cached_distance_calculation() {

                List<Point> route = List.of(
                                ofMercator(0, 0),
                                ofMercator(1, 0, 2), // preferred
                                ofMercator(2, 0));

                assertThat(service.getLengthMercatorM(route)).isEqualTo(3);
        }
}
