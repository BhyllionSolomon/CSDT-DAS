import { Routes, Route } from "react-router-dom";

import Layout from "./components/Layout";

import Dashboard from "./pages/Dashboard";
import UploadDataset from "./pages/UploadDataset";
import DatasetHistory from "./pages/DatasetHistory";
import DiseaseRecords from "./pages/DiseaseRecords";
import Prediction from "./pages/Prediction";

function App() {

    return (

        <Routes>

            <Route path="/" element={<Layout />}>

                <Route index element={<Dashboard />} />

                <Route
                    path="upload"
                    element={<UploadDataset />}
                />

                <Route
                    path="history"
                    element={<DatasetHistory />}
                />

                <Route
                    path="records"
                    element={<DiseaseRecords />}
                />

                <Route
                    path="prediction"
                    element={<Prediction />}
                />

            </Route>

        </Routes>

    );

}

export default App;