import { Button, CircularProgress } from "@mui/material";
import React from "react";
import { Navigate, useParams } from "react-router-dom";
import SpeedDiagram from "./SpeedDiagram";
import SpeedMap from "./SpeedMap";

interface TrackedPosition {
    lat: number,
    lng: number,
    accuracy: number,
    alt: number,
    altAccuracy: number,
    heading: number,
    speed: number,
    measurementTime: string,
    tsp: number,
}

export interface TourDetails {
    locations: TrackedPosition[],
}

interface Props {
    tourId: string,
}

interface State {
    tourDetails?: TourDetails,
    navigateBackToStart?: boolean,
}

export class TspHoverChangeObserver {
    hoveredTsp: number | undefined;
    listener: ((tsp: number | undefined) => void) | undefined;

    onTspHover(tsp?: number): void {
        if (this.hoveredTsp === tsp) return;
        this.hoveredTsp = tsp;
        if (this.listener) this.listener(tsp);
    }
}

// https://localhost:5000/show-tour/8271

class ShowTour extends React.Component<Props, State> {

    state: State = {}

    tspHoverChangeObserver = new TspHoverChangeObserver();

    componentDidMount(): void {
        this.fetchTourDetails();
    }

    async fetchTourDetails() {
        const response = await fetch("/api/tour/" + this.props.tourId, { method: "GET" });
        if (!response.ok) throw new Error("" + response.status);
        const body: TourDetails = await response.json();
        body.locations.forEach(l => { l.tsp = new Date(l.measurementTime).getTime() / 1000; });
        this.setState({ tourDetails: body });
    }

    render(): React.ReactNode {
        return <div style={{
            display: "flex", flexDirection: "column", alignItems: "center"
        }}>
            <div >
                Showing tour {this.props.tourId}:</div>
            <Button onClick={() => this.setState({ navigateBackToStart: true })}>Go back</Button>
            {!this.state.tourDetails ? <CircularProgress /> :
                <>
                    <div style={{ padding: "20px", }}>
                        <SpeedDiagram tourDetails={this.state.tourDetails}
                            onTspHover={(tsp) => this.tspHoverChangeObserver.onTspHover(tsp)} />
                    </div>

                    <SpeedMap tourDetails={this.state.tourDetails} tspHoverChangeObserver={this.tspHoverChangeObserver} />

                </>
            }
            {this.state.navigateBackToStart ? <Navigate to={"/"} /> : <></>}
        </div >
    }
}

export default function ShowTourRouted() {
    const params = useParams();
    const tourId = params["tourId"];
    if (!tourId) throw new Error();
    return <ShowTour tourId={tourId} />;
}
