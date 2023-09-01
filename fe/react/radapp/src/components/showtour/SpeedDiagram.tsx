import { memo, useEffect } from "react";
import uPlot from "uplot";
import "uplot/dist/uPlot.min.css";
import { TourDetails } from "./ShowTour";

const SPEED_DIAGRAM_ID = "speed-diagram";

function renderSpeedDiagram(tourDetails: TourDetails,
    onTspHover: (tsp?: number) => void): uPlot {

    const tsps = tourDetails.locations.map(l => l.tsp);
    const speeds = tourDetails.locations.map(l => l.speed);
    const uPlotData: uPlot.AlignedData = [tsps, speeds];

    const chartDiv = document.getElementById(SPEED_DIAGRAM_ID);
    if (!chartDiv) throw new Error();
    chartDiv.replaceChildren();

    const opts: uPlot.Options = {
        title: "Speed (m/s)",
        width: Math.min(1000, window.screen.availWidth * 0.9),
        height: 200,
        series: [
            {
                value: (_, tsp) => {
                    if (!tsp) return tsp;
                    return new Date(tsp * 1000).toLocaleTimeString();
                }
            },
            {
                label: "measured",
                stroke: "blue",
                width: 2,
                fill: "rgba(0, 0, 255, 0.1)",
                dash: [5, 5],
                value: (_, v) => v?.toFixed(1),
            },
        ],
        hooks: {
            setCursor: [self => {
                const closestIdx = self.cursor.idx;
                if (closestIdx == null) {
                    onTspHover(undefined);
                    return;
                }
                const tsp = self.data[0][closestIdx];
                onTspHover(tsp);
            }]
        }
    };
    return new uPlot(opts, uPlotData, chartDiv);
}

function SpeedDiagram(props: {
    tourDetails: TourDetails,
    onTspHover: (tsp?: number) => void
}): React.JSX.Element {

    // render into div after mount
    useEffect(() => {
        renderSpeedDiagram(props.tourDetails, (tsp) => props.onTspHover(tsp))
    }, [props]);

    return <div id={SPEED_DIAGRAM_ID} />;
}

const SpeedDiagramMemo = memo(SpeedDiagram, (prevProps, nextProps) => {
    return prevProps.tourDetails !== nextProps.tourDetails;
});

export default SpeedDiagramMemo;
