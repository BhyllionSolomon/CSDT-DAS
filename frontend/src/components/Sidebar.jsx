import { Link } from "react-router-dom";

function Sidebar() {

    return (

        <div className="sidebar">

            <h2>EpiForecaster</h2>

            <Link to="/">Dashboard</Link>

            <Link to="/upload">
                Upload Dataset
            </Link>

            <Link to="/history">
                Dataset History
            </Link>

            <Link to="/records">
                Disease Records
            </Link>

            <Link to="/prediction">
                Prediction
            </Link>

        </div>

    );

}

export default Sidebar;