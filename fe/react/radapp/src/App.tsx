import {
  createBrowserRouter,
  RouterProvider
} from "react-router-dom";
import EndTourRouted from "./components/endtour/EndTour";
import RunTourRouted from "./components/runtour/RunTour";
import ShowTourRouted from "./components/showtour/ShowTour";
import StartTour from "./components/starttour/StartTour";


const router = createBrowserRouter([
  {
    path: "/",
    element: <StartTour />,
  }
  , {
    path: "/run-tour/:tourId",
    element: <RunTourRouted />,
  },
  {
    path: "/tour-ended/:tourId",
    element: <EndTourRouted />,
  },
  {
    path: "/show-tour/:tourId",
    element: <ShowTourRouted />,
  },
]);

function App() {
  return <RouterProvider router={router} />;
}

export default App
