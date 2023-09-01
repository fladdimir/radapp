import * as L from "leaflet";
import { useRef } from "react";
import { AttributionControl, MapContainer, TileLayer, useMap } from "react-leaflet";
import "uplot/dist/uPlot.min.css";
import { FitBoundsOnce } from "../runtour/RunTourMap";
import { TourDetails, TspHoverChangeObserver } from "./ShowTour";


function Circles(props: {
    tourDetails: TourDetails,
    tspHoverChangeObserver: TspHoverChangeObserver
}) {

    const map = useMap();

    const circlesRef = useRef<Map<number, L.Circle>>(new Map());
    const hoveredCircleRef = useRef<L.Circle | null>(null);

    function hoverCircle(circle: L.Circle) {
        circle.setStyle({ color: "red", weight: 5 });
    }

    function unhoverCircle(circle: L.Circle) {
        circle.setStyle({ color: "blue", weight: 1 });
    }

    function drawCircles() {
        if (!map) throw new Error();
        props.tourDetails.locations.forEach(p => {
            const circle = new L.Circle(L.latLng(p.lat, p.lng), {
                radius: p.accuracy,
                color: "blue",
                weight: 1,
                fillColor: "#f03",
                fillOpacity: 0.01,
            });
            getCircles().set(p.tsp, circle);
            circle.addTo(map);
        });
    }

    function getCircles(): Map<number, L.Circle> {
        return circlesRef.current;
    }

    function getHoveredCircle(): L.Circle | null {
        return hoveredCircleRef.current;
    }

    function onTspHoverChange(tsp?: number) {
        const newlyHoveredCircle = tsp ? getCircles().get(tsp) ?? null : null;
        if (newlyHoveredCircle === getHoveredCircle()) return;
        const prevHoveredCircle = getHoveredCircle();
        if (prevHoveredCircle) {
            unhoverCircle(prevHoveredCircle);
            hoveredCircleRef.current = null;
        }
        if (newlyHoveredCircle) {
            hoverCircle(newlyHoveredCircle);
            hoveredCircleRef.current = newlyHoveredCircle;
        }
    }

    drawCircles();

    props.tspHoverChangeObserver.listener = tsp => onTspHoverChange(tsp);

    return <></>;
}


function SpeedMap(props: {
    tourDetails: TourDetails,
    tspHoverChangeObserver: TspHoverChangeObserver
}) {

    return <MapContainer center={[53, 10]} zoom={10} style={{ height: "500px", width: "100%", }} attributionControl={false} >
        <TileLayer
            attribution='<a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <AttributionControl position="bottomright" prefix={false} />

        <FitBoundsOnce points={props.tourDetails.locations.map(l => new L.LatLng(l.lat, l.lng))} />

        <Circles tourDetails={props.tourDetails} tspHoverChangeObserver={props.tspHoverChangeObserver} />

    </MapContainer>
}

export default SpeedMap;
