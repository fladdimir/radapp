import { Icon, LatLng, LatLngTuple } from 'leaflet';
import 'leaflet/dist/leaflet.css';
import React, { Fragment, useEffect } from 'react';
import { AttributionControl, Circle, MapContainer, Marker, Polyline, Popup, TileLayer, useMap } from 'react-leaflet';

export interface RoutePoint {
    point: LatLng,
    trafficLightId?: string,
}

export interface Route {
    points: RoutePoint[],
}

export enum PhaseValues {
    GREEN, RED,
}

export interface Phase {
    value: PhaseValues,
    tsp: number
}

export interface TrafficLightData {
    id: string,
    currentPhase: Phase,
    nextPhases: Phase[],
}

export interface RunTourData {

    currentPosition: LatLng,
    accuracyM: number,

    snappedPosition?: LatLng,
    trafficLightInfo?: TrafficLightData[];
}

interface Props {
    route: Route,
    rtData?: RunTourData,
    onCurrentPositionMarkerDrop?: (latLng: LatLng) => void,
}

function Route(props: { positions: LatLng[] }): React.JSX.Element {
    return <Polyline positions={props.positions} />
}

export function FitBoundsOnce(props: { points: LatLng[] }): React.JSX.Element {
    const map = useMap();
    useEffect(() => { // just on initial display
        const bounds: LatLngTuple[] = props.points.map(p => [p.lat, p.lng]);
        if (!bounds.length) return;
        map.fitBounds(bounds)
    }, []);
    return <></>
}

function RunTourMap(props: Props): React.JSX.Element {

    const routeLatLngs = props.route.points.map(rp => rp.point);
    const tlRoutePoints: Map<string, RoutePoint> = new Map(props.route.points.filter(p => p.trafficLightId).map(p => [p.trafficLightId!, p]));

    const rtTlData = props.rtData?.trafficLightInfo ?? [];
    const rtTlDataMap: Map<string, TrafficLightData> = new Map(rtTlData.filter(rtTLD => rtTLD.id).map(rtTlD => [rtTlD.id, rtTlD]));

    const otherTrafficLightRoutePoints: RoutePoint[] = Array.from(tlRoutePoints.values()).filter(
        p => p.trafficLightId && !(p.trafficLightId in rtTlDataMap));

    const boundsPoints: LatLng[] = [...routeLatLngs];
    if (props.rtData?.currentPosition) boundsPoints.push(props.rtData.currentPosition);

    const markerIcon = new Icon({ iconUrl: "/marker-icon.png", iconSize: [25, 41], iconAnchor: [12, 41] });

    return (
        <MapContainer center={[53, 10]} zoom={10} style={{ height: "500px", width: "100%", }} attributionControl={false}>
            <TileLayer
                attribution='<a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <AttributionControl position="bottomright" prefix={false} />

            <Route positions={routeLatLngs} />

            {otherTrafficLightRoutePoints.map(tl => (
                <Marker icon={markerIcon} position={tl.point} key={JSON.stringify(tl)}>
                    <Popup> {tl.trafficLightId} </Popup>
                </Marker>))}

            {Array.from(rtTlDataMap.values()).map((tld) => (
                <Fragment key={tld.id}>
                    <Marker icon={markerIcon} position={tlRoutePoints?.get(tld.id)!.point} key={"marker_" + tld.id} >
                        <Popup key={"popup_" + tld.id}>{tld.id} - {tld.currentPhase?.value === PhaseValues.RED ? "red" : "green"}</Popup>
                    </Marker>
                    <Circle center={tlRoutePoints.get(tld.id)!.point} radius={200} key={"circle_" + tld.id}
                        pathOptions={{ color: tld.currentPhase?.value === PhaseValues.GREEN ? "green" : "red" }} opacity={0.1} />
                </ Fragment>
            ))}

            {props.rtData?.snappedPosition ?
                <Circle center={props.rtData.snappedPosition} radius={5} fillOpacity={1} color='black' weight={3} /> : <></>}
            {
                props.rtData?.currentPosition ?
                    <>
                        <Circle center={props.rtData.currentPosition} radius={props.rtData.accuracyM} fillOpacity={0.1} color='blue' weight={1} />
                        <Marker icon={markerIcon} position={props.rtData.currentPosition} draggable eventHandlers={{
                            dragend: (event) => {
                                if (props.onCurrentPositionMarkerDrop) props.onCurrentPositionMarkerDrop(event.target.getLatLng());
                            },
                        }} />
                    </> : <></>
            }

            <FitBoundsOnce points={boundsPoints} />
        </MapContainer>
    );
}

export default RunTourMap;
