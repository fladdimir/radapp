import { Button, CircularProgress, Container, FormControl, InputLabel, MenuItem, Select, SelectChangeEvent } from "@mui/material";
import React from "react";
import { Navigate } from "react-router-dom";


interface State {
    isLoading: boolean;
    selectedTour?: string;
    tours: ExistingTourInfo[];
    navigateToRunTourId?: boolean;
    navigateToShowTourId?: boolean;
}

interface ExistingTourInfo {
    id: string;
    tripStartTime: string;
    routeName: string;
    tripStartTimeDate: Date
}

class SelectTour extends React.Component<unknown, State> {

    state: State = { isLoading: true, selectedTour: undefined, tours: [] };

    componentDidMount(): void {
        this.fetchExistingTours();
    }

    async fetchExistingTours(): Promise<void> {
        const response = await fetch(window.origin + "/api/tour/last-n", { method: "GET" });
        if (!response.ok) { alert(response.status); throw new Error("" + response.status); }
        const tours: ExistingTourInfo[] = await response.json();
        tours.forEach(t => t.tripStartTimeDate = new Date(t.tripStartTime));
        this.setState({ tours, selectedTour: tours.find(Boolean)?.id, isLoading: false, });
    }

    handleSelectedTourChange(ev: SelectChangeEvent) {
        this.setState({ selectedTour: ev.target.value });
    }

    resumeTour() {
        this.setState({ navigateToRunTourId: true, });
    }

    showTour() {
        this.setState({ navigateToShowTourId: true, });
    }

    render(): React.ReactNode {
        return <>
            {this.state.isLoading ? <CircularProgress /> : <></>}
            {this.state.navigateToRunTourId ? <Navigate to={`/run-tour/${this.state.selectedTour}`} /> : <></>}
            {this.state.navigateToShowTourId ? <Navigate to={`/show-tour/${this.state.selectedTour}`} /> : <></>}
            {this.state.isLoading ? <> </> : <>
                <Container style={{ textAlign: "center", display: "flex", flexDirection: "column" }}>
                    <FormControl fullWidth>
                        <InputLabel id="tour-select-label">Tour</InputLabel>
                        <Select labelId="tour-select-label" label="Tour" value={this.state.selectedTour} onChange={(ev) => this.handleSelectedTourChange(ev)}>
                            {this.state.tours.map(tour => <MenuItem value={tour.id} key={tour.id}>{`${tour.routeName} - ${tour.tripStartTimeDate} (${tour.id})`}</MenuItem>)}
                        </Select>
                    </FormControl>

                    <Button disabled={this.state.selectedTour ? false : true}
                        onClick={() => { if (this.state.selectedTour) this.resumeTour(); }}>Resume tour</Button>
                    <Button disabled={this.state.selectedTour ? false : true}
                        onClick={() => { if (this.state.selectedTour) this.showTour(); }}>Show tour details</Button>
                </Container >
            </>
            }
        </>
    }

}

export default SelectTour;
