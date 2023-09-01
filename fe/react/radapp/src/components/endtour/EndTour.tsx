import { Button } from "@mui/material";
import { useNavigate, useParams } from "react-router-dom";


function EndTour(props: { tourId: string }): React.JSX.Element {

    const navigate = useNavigate();

    const navigateToDetails = () => navigate("/show-tour/" + props.tourId);

    return <div style={{ textAlign: "center" }}>
        <div>Congrats, tour {props.tourId} successfully finished!</div>
        <Button onClick={() => navigateToDetails()}>Details!</Button>
    </div>
}

function EndTourRouted() {
    const params = useParams();
    const tourId = params["tourId"];
    if (!tourId) throw new Error();
    return <EndTour tourId={tourId} />;
}

export default EndTourRouted;
