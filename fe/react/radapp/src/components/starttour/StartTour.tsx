import { Button, CircularProgress, Container, FormControl, InputLabel, MenuItem, Select, SelectChangeEvent } from '@mui/material';
import React from "react";
import { Navigate } from 'react-router-dom';
import SelectTour from './SelectTour';

const getOffsetDateTime = () => new Date().toISOString();

interface TourStartRequestData {
    time: string;
    routeName: string;
}

interface TourStartResponse {
    id: string
}

async function post(url: string, data: TourStartRequestData): Promise<TourStartResponse> {
    const response = await fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(data)
    });
    if (!response.ok) {
        const error = "request failure: " + response.status;
        alert(error); throw new Error(error);
    }
    return await response.json();
}

async function createTour(routeName: string): Promise<string> {
    const responseBody = await post(window.origin + "/api/tour", { time: getOffsetDateTime(), routeName })
    return "" + responseBody.id;
}

async function getAllRoutes(): Promise<string[]> {
    const response = await fetch(window.origin + "/api/route-planning/route-names", { method: "GET" });
    if (!response.ok) { alert(response.status); throw new Error("" + response.status); }
    const routes: string[] = await response.json();
    return routes;
}

interface State {
    isLoading: boolean;
    selectedRoute?: string;
    routes: Array<string>;
    navigateToRunTourTourId?: string;
}

class StartTour extends React.Component<unknown, State> {
    state: State = { isLoading: true, selectedRoute: undefined, routes: [] };

    componentDidMount() {
        this.fetchConfiguredRoutes();
    }

    async fetchConfiguredRoutes(): Promise<void> {
        if (this.state.routes.length) return;
        this.setState({ isLoading: true });
        const routes = await getAllRoutes();
        this.setState({ isLoading: false, routes, selectedRoute: routes.find(Boolean) });
    }

    handleSelectedRouteChange(event: SelectChangeEvent): void {
        this.setState({ selectedRoute: event.target.value });
    }

    async startNewTour(selectedRoute: string): Promise<void> {
        const createdTour = await createTour(selectedRoute);
        this.setState({ navigateToRunTourTourId: createdTour }); // -> Navigate
    }

    render(): React.ReactNode {
        if (this.state.isLoading) return <CircularProgress />;
        if (this.state.navigateToRunTourTourId) return <Navigate to={`/run-tour/${this.state.navigateToRunTourTourId}`} />;
        // else:
        return <Container style={{ textAlign: "center", display: "flex", flexDirection: "column" }}>
            <FormControl fullWidth>
                <InputLabel id="route-select-label">Route</InputLabel>
                <Select labelId="route-select-label" label="Route" value={this.state.selectedRoute} onChange={(ev) => this.handleSelectedRouteChange(ev)}>
                    {this.state.routes.map(route => <MenuItem value={route} key={route}>{route}</MenuItem>)}
                </Select>
            </FormControl>
            <Button disabled={this.state.selectedRoute ? false : true}
                onClick={() => { if (this.state.selectedRoute) this.startNewTour(this.state.selectedRoute); }}>Start new tour</Button>

            <SelectTour />
        </Container >;
    }
}

export default StartTour;
