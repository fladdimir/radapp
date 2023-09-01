import { LatLng } from "leaflet";
import { Phase, PhaseValues } from "./RunTourMap";


interface TrafficLightData {
    id: string;
    phases: Phase[];
}

interface RequestDataPoint {
    lat: number,
    lng: number,
    accuracy: number,
    alt?: number,
    altAccuracy?: number,
    heading?: number,
    speed?: number,
    measurementTime: string,
}

export enum Recommendation {
    SLOWER = "SLOWER", KEEP = "KEEP", QUICKER = "QUICKER"
}

interface Response {
    trafficLightId: string,
    distanceM: number,
    snappedTo: { lat: number, lon: number, reachedIdx: number },
    data: { phenomenonTime: string, result: string }[],
    necessarySpeed?: number,
    recommendation?: Recommendation,
}

export interface ApiResponse {
    latency: number,
    snappedPosition?: LatLng,
    trafficLightsData?: TrafficLightData[],
    necessarySpeed?: number,
    recommendation?: Recommendation,
}

export interface ApiResponseCallback {
    (response: ApiResponse): void;
}

export class RunTourApiService {

    // request new traffic-lights data based on current positions
    // at most every X seconds

    private tourId: string;
    private timeBetweenRequests: number;
    private onApiResponse: ApiResponseCallback;
    private onApiRequestFailure: VoidFunction;
    private onMaxRequestRetriesReached: VoidFunction;
    private newPositions: Array<GeolocationPosition> = [];
    private timeOfLastRequest: number | undefined;
    private isRequestPending = false;
    private requestTimer: number | undefined;
    private reachedIdx = 0;
    private consecutiveRequestFailures = 0;
    private static readonly MAX_CONSECUTIVE_REQUEST_FAILURES = 10;

    private static readonly FETCH_TIMEOUT = 3000;
    private static readonly DEFAULT_TIME_BETWEEN_REQUESTS = 10_000;

    constructor(tourId: string, onApiResponse: ApiResponseCallback, onApiRequestFailure: VoidFunction, onMaxRequestRetriesReached: VoidFunction, timeBetweenRequests = RunTourApiService.DEFAULT_TIME_BETWEEN_REQUESTS) {
        this.tourId = tourId;
        this.onApiResponse = onApiResponse;
        this.timeBetweenRequests = timeBetweenRequests;
        this.onApiRequestFailure = onApiRequestFailure;
        this.onMaxRequestRetriesReached = onMaxRequestRetriesReached;
    }

    newPosition(position: GeolocationPosition): void {
        this.newPositions.push(position);
        this.checkForRequest();
    }

    cancel(): void {
        if (!this.requestTimer) return;
        clearTimeout(this.requestTimer);
        this.requestTimer = undefined;
    }

    private async request(): Promise<void> {
        this.isRequestPending = true;

        let newPositions = this.newPositions.splice(0, this.newPositions.length);
        const map = new Map<number, GeolocationPosition>();
        newPositions.forEach(p => map.set(p.timestamp, p));
        newPositions = [...map.values()];

        const requestStartTime = Date.now();

        const data: RequestDataPoint[] = newPositions.map(p => {
            const mapped: RequestDataPoint = {
                lat: p.coords.latitude,
                lng: p.coords.longitude,
                accuracy: p.coords.accuracy,
                measurementTime: new Date(p.timestamp).toISOString(),
                speed: p.coords.speed ?? undefined,
                alt: p.coords.altitude ?? undefined,
                altAccuracy: p.coords.altitudeAccuracy ?? undefined,
                heading: p.coords.heading ?? undefined,
            }
            return mapped;
        });

        // sometimes there seem to be strange 504 errors
        let response: globalThis.Response;
        try {
            response = await fetch(
                window.origin + "/api/tour/" + this.tourId + "/traffic-lights-data"
                + "?reachedIdx=" + this.reachedIdx, {
                method: "POST",
                headers: { "Content-Type": "application/json", },
                body: JSON.stringify(data),
                signal: AbortSignal.timeout(RunTourApiService.FETCH_TIMEOUT)
            });
            if (!response.ok) {
                const errorText = "api-request error: " + response.status;
                console.log(errorText, response);
                throw new Error(errorText);
            }
        } catch (err) {
            this.timeOfLastRequest = Date.now();
            this.isRequestPending = false;
            this.consecutiveRequestFailures++;
            this.newPositions.unshift(...newPositions); // back to the queue
            this.onApiRequestFailure();

            // try again or finally throw the error
            if (this.consecutiveRequestFailures <= RunTourApiService.MAX_CONSECUTIVE_REQUEST_FAILURES) {
                this.checkForRequest(); // try again
                return;
            }
            this.consecutiveRequestFailures = 0; // reset
            this.onMaxRequestRetriesReached();
            throw err; // report
        }
        this.consecutiveRequestFailures = 0; // reset

        const latency = Date.now() - requestStartTime;

        const responseBody: Response = await response.json();

        this.reachedIdx = responseBody.snappedTo.reachedIdx;

        const toPhaseValue = (v: string) => v === "RED" ? PhaseValues.RED : PhaseValues.GREEN;

        this.onApiResponse({
            latency,
            snappedPosition: new LatLng(responseBody.snappedTo.lat, responseBody.snappedTo.lon),
            trafficLightsData: [{
                id: responseBody.trafficLightId,
                phases: !(responseBody.data) ? [] :
                    responseBody.data.filter(v => v.result === "RED" || v.result === "GREEN").map(v => {
                        const ph: Phase = {
                            tsp: new Date(v.phenomenonTime).valueOf(),
                            value: toPhaseValue(v.result),
                        };
                        return ph;
                    }).sort((p1, p2) => p2.tsp - p1.tsp)
            }],
            necessarySpeed: responseBody.necessarySpeed,
            recommendation: responseBody.recommendation,
        });

        this.timeOfLastRequest = Date.now();
        this.isRequestPending = false;
        this.checkForRequest();
    }

    private checkForRequest() {
        if (this.isRequestPending) return; // no concurrent requests
        if (!this.newPositions.length) return; // nothing new
        if (this.timeOfLastRequest && this.timeSinceLastRequest() < this.timeBetweenRequests) {
            this.setTimerIfNecessary();
            return;
        }
        this.request();
    }

    private timeSinceLastRequest(): number {
        if (!this.timeOfLastRequest) throw new Error();
        return Date.now() - this.timeOfLastRequest;
    }

    private setTimerIfNecessary() {
        if (this.requestTimer) return;
        const timeToNextRequest = this.timeBetweenRequests - this.timeSinceLastRequest();
        this.requestTimer = setTimeout(() => {
            this.requestTimer = undefined;
            this.checkForRequest();
        }, timeToNextRequest);
    }

}
