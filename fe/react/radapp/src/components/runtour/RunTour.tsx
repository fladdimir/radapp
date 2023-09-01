import { Button, CircularProgress, Container, FormControlLabel, FormGroup, Switch } from "@mui/material";
import { LatLng } from "leaflet";
import React, { useState } from "react";
import { Navigate, useParams } from "react-router-dom";
import { ApiResponse, Recommendation, RunTourApiService } from "./RunTourApiService";
import RunTourMap, { PhaseValues, Route, RunTourData, TrafficLightData } from "./RunTourMap";

// execute a tour by ID

// next traffic-light (name, position),
// next red/green-times, recommendation (range-slider for min/max speeds -> optional query params)
// -> display current recommendation (red yellow green + arrow)
// + display current connection status (latency / location accuracy)
// + end tour -> tour-ended (-> link to show-tour)

interface RouteResponse {
    points: [{
        lat: number
        lon: number
        trafficLightId?: string
    }]
}

async function getRoute(tourId: string): Promise<Route> {
    const response = await fetch(window.origin + "/api/tour/" + tourId + "/route");
    if (!response.ok) throw new Error("" + response.status);
    const routeResponse: RouteResponse = await response.json();
    return {
        points: routeResponse.points.map(rrP => {
            return { point: new LatLng(rrP.lat, rrP.lon), trafficLightId: rrP.trafficLightId }
        })
    }
}

function requestGeolocationAccess(onUpdate: PositionCallback, onError: PositionErrorCallback): number {
    return navigator.geolocation.watchPosition(
        (position) => onUpdate(position),
        onError,
        { enableHighAccuracy: true }
    );
}

function cancelGeolocationAccess(geolocationWatchId: number): void {
    navigator.geolocation.clearWatch(geolocationWatchId);
}

interface Props {
    tourId: string;
}

interface State {
    isLoading: boolean;
    route?: Route;
    currentPosition?: GeolocationPosition;
    snappedPosition?: LatLng;
    trafficLightsData?: TrafficLightData[];
    apiLatency?: number;
    isGpsEnabled: boolean;
    isWakeLockEnabled: boolean;
    navigateToEndedTourId?: string;
    lastRequestFailed: boolean;
    maxRequestRetriesReached: boolean;
    timeOfFirstRequestFailure?: Date;
}

function ShowRecommendation(props: { recommendation?: Recommendation, currentSpeed?: number | null, necessarySpeed?: number, nextPhaseChangeTsp?: number, nextPhase?: PhaseValues }): React.JSX.Element {
    let color;
    const recommendation = props.recommendation;
    if (!recommendation) color = "grey";
    if (Recommendation.QUICKER === recommendation) color = "DarkGreen";
    if (Recommendation.KEEP === recommendation) color = "lime";
    if (Recommendation.SLOWER === recommendation) color = "red";

    const secToPhaseChange = props.nextPhaseChangeTsp ? ((props.nextPhaseChangeTsp - Date.now()) / 1000) : undefined;
    const [rerender, setRerender] = useState(false);
    setTimeout(() => setRerender(!rerender), 1000);

    let nextPhase = "next phase";
    if (props.nextPhase !== undefined) {
        nextPhase = props.nextPhase === PhaseValues.GREEN ? "green" : "red";

    }

    return <div>
        <div style={{
            height: "200px",
            width: "200px",
            backgroundColor: color,
            borderRadius: "50%",
            display: "inline-block",
        }}>
            <div style={{ position: "relative", top: "30%" }}>
                {recommendation ?? " - "}
                <br />
                {props.currentSpeed ? props.currentSpeed.toFixed(1) : "-"} / {props.necessarySpeed ? props.necessarySpeed.toFixed(1) : "-"} m/s
                <br />
                {secToPhaseChange ? `${nextPhase} in: ${secToPhaseChange.toFixed(0)} s` : ""}
            </div>
        </div>
    </div>;
}

class RunTour extends React.Component<Props, State> {

    geolocationWatchId?: number;

    apiRequestService = new RunTourApiService(this.props.tourId, (response) => this.onApiResponse(response), () => this.onApiRequestFailure(), () => this.onMaxRequestRetriesReached(), 1000);
    lastApiResponse?: ApiResponse;

    state: State = { isLoading: true, isGpsEnabled: true, isWakeLockEnabled: false, lastRequestFailed: false, maxRequestRetriesReached: false };

    componentDidMount(): void {
        this.fetchRoute();
        this.geolocationWatchId = requestGeolocationAccess(pos => this.onGeolocationUpdate(pos), this.onGeolocationError);
    }

    componentWillUnmount(): void {
        if (this.geolocationWatchId) cancelGeolocationAccess(this.geolocationWatchId);
        this.apiRequestService.cancel();
    }

    onGeolocationUpdate(position: GeolocationPosition): void {
        this.apiRequestService.newPosition(position);
        this.setState({ currentPosition: position, });
    }

    onGeolocationError(positionError: GeolocationPositionError): void {
        console.log(positionError);
    }

    switchOffGps(): void {
        if (this.geolocationWatchId) cancelGeolocationAccess(this.geolocationWatchId);
        this.setState({ isGpsEnabled: false });
    }

    async fetchRoute(): Promise<void> {
        if (this.state.route?.points.length) return;
        this.setState({ isLoading: true });
        const route: Route = await getRoute(this.props.tourId);
        this.setState({ isLoading: false, route });
    }

    async endTour(): Promise<void> {
        // tbd: confirmation modal
        const response = await fetch(window.origin + "/api/tour/" + this.props.tourId + "/end"
            + "?time=" + encodeURIComponent(new Date().toISOString()), { method: "POST" });
        if (!response.ok) { throw new Error("" + response.status); }
        this.setState({ navigateToEndedTourId: this.props.tourId, });
    }

    onApiResponse(response: ApiResponse): void {
        this.setState({ lastRequestFailed: false, });
        this.lastApiResponse = response;
        this.evaluateLastApiResponse();
    }

    onApiRequestFailure(): void {
        this.setState({ lastRequestFailed: true, timeOfFirstRequestFailure: new Date(), });
    }

    onMaxRequestRetriesReached(): void {
        this.switchOffGps();
        this.setState({ maxRequestRetriesReached: true, });
    }

    evaluateLastApiResponse(): void {
        const response = this.lastApiResponse;
        if (!response) {
            this.setState({ apiLatency: undefined, snappedPosition: undefined, trafficLightsData: undefined, });
            return;
        }
        const tld = response.trafficLightsData;
        let processedTld: TrafficLightData | undefined = undefined;
        if (tld?.length) {
            const phases = tld[0].phases;
            processedTld = { id: tld[0].id, currentPhase: phases[0], nextPhases: phases.slice(1) };
        }
        this.setState({ apiLatency: response.latency, snappedPosition: response.snappedPosition, trafficLightsData: processedTld ? [processedTld] : undefined, });
    }

    onCurrentPositionMarkerDrop(latLng: LatLng) {
        this.onGeolocationUpdate({
            coords: {
                latitude: latLng.lat, longitude: latLng.lng,
                accuracy: 10, altitude: null, altitudeAccuracy: null, heading: null, speed: 20 / 3.6,
            },
            timestamp: Date.now(),
        });
    }

    getRunTourData(): RunTourData | undefined {
        if (!this.state.currentPosition) return undefined;
        const coords = this.state.currentPosition.coords;
        const currentPosition = new LatLng(coords.latitude, coords.longitude, coords.altitude ?? undefined);
        return {
            currentPosition,
            accuracyM: coords.accuracy,
            snappedPosition: this.state.snappedPosition,
            trafficLightInfo: this.state.trafficLightsData,
        };
    }

    async enableWakeLock() {
        try {
            await navigator.wakeLock.request('screen');
        } catch (error) {
            alert(`wake-lock error: ${error}`);
            return;
        }
        this.setState({ isWakeLockEnabled: true });
    }

    render(): React.ReactNode {
        if (this.state.isLoading) return <CircularProgress />;
        if (this.state.navigateToEndedTourId) return <Navigate to={`/tour-ended/${this.state.navigateToEndedTourId}`} />;
        if (this.state.maxRequestRetriesReached) return <div style={{ color: "red" }}>API REQUEST FAILURE - RETRY NOT SUCCESSFULL ({this.state.timeOfFirstRequestFailure?.toISOString()})</div>;
        const route = this.state.route;
        const runTourData: RunTourData | undefined = this.getRunTourData();
        return (
            <>
                <Container style={{ textAlign: "center" }}>

                    <ShowRecommendation recommendation={this.lastApiResponse?.recommendation}
                        currentSpeed={this.state.currentPosition?.coords.speed} necessarySpeed={this.lastApiResponse?.necessarySpeed}
                        nextPhaseChangeTsp={this.state.trafficLightsData ? this.state.trafficLightsData[0]?.nextPhases[0]?.tsp : undefined}
                        nextPhase={this.state.trafficLightsData ? this.state.trafficLightsData[0]?.nextPhases[0]?.value : undefined}
                    />

                    <div style={{ display: "flex", justifyContent: "center" }}>
                        <FormGroup style={{ paddingRight: "50px" }}>
                            <FormControlLabel control={<Switch defaultChecked value={this.state.isGpsEnabled}
                                onClick={() => this.switchOffGps()} disabled={!this.state.isGpsEnabled} />} label="GPS" />
                        </FormGroup>
                        <FormGroup>
                            <FormControlLabel control={<Switch checked={this.state.isWakeLockEnabled}
                                onClick={() => this.enableWakeLock()} disabled={!navigator.wakeLock || this.state.isWakeLockEnabled} />} label="Wake-Lock" />
                        </FormGroup>
                    </div>

                    {route ? <RunTourMap route={route} rtData={runTourData}
                        onCurrentPositionMarkerDrop={(latLng) => this.onCurrentPositionMarkerDrop(latLng)} /> : <></>}

                    <br />
                    <div>tour_id: {this.props.tourId} | latency: {this.state.apiLatency}ms | accuracy: {this.state.currentPosition?.coords.accuracy.toFixed(1)}m</div>
                    {this.state.lastRequestFailed ? <div style={{ color: "black" }}>LAST REQUEST FAILED</div> : <></>}
                    <Button onClick={() => this.endTour()}>End tour</Button>

                </Container>
            </>
        );
    }
}

function RunTourRouted() {
    const params = useParams();
    const tourId = params["tourId"];
    if (!tourId) throw new Error();
    return <RunTour tourId={tourId} />;
}

export default RunTourRouted;
