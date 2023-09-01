package com.example.glosa.routing;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.glosa.routing.PlannedRoute.PlannedRouteRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/route-planning")
@RequiredArgsConstructor
class PlannedRouteController {

    private final RouteCreator routeCreator;
    private final PlannedRouteRepository routeRepository;

    @GetMapping(path = "/route-names")
    List<String> getRouteNames() {
        return routeRepository.getNames();
    }

    @GetMapping(path = "/route/{name}")
    PlannedRoute getOrCreateRoute(@PathVariable("name") String name) {
        return routeCreator.getOrCreate(name, Optional.of(25.));
    }

}
