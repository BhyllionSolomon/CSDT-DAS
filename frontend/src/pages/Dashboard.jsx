import { useEffect, useState } from "react";
import api from "../services/api";

function Dashboard() {

    const [stats, setStats] = useState({
        datasets: 0,
        records: 0,
        diseases: 0,
        states: 0,
        lgas: 0
    });

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");

    useEffect(() => {

        loadStatistics();

    }, []);

    async function loadStatistics() {

        try {

            const response =
                await api.get("/dashboard/statistics");

            setStats(response.data);

            setLoading(false);

        } catch (err) {

            console.error(err);

            setError("Unable to load dashboard statistics.");

            setLoading(false);

        }

    }

    if (loading) {

        return (
            <div style={{ padding: "30px" }}>
                <h2>Loading dashboard...</h2>
            </div>
        );

    }

    if (error) {

        return (
            <div style={{ padding: "30px", color: "red" }}>
                <h2>{error}</h2>
            </div>
        );

    }

    return (

        <div
            style={{
                padding: "30px"
            }}
        >

            <h1>EpiForecaster Dashboard</h1>

            <p>
                Welcome to the Epidemiological Forecasting System.
            </p>

            <div
                style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(auto-fit,minmax(220px,1fr))",
                    gap: "20px",
                    marginTop: "30px"
                }}
            >

                <DashboardCard
                    title="Datasets Uploaded"
                    value={stats.datasets}
                />

                <DashboardCard
                    title="Disease Records"
                    value={stats.records}
                />

                <DashboardCard
                    title="Diseases"
                    value={stats.diseases}
                />

                <DashboardCard
                    title="States"
                    value={stats.states}
                />

                <DashboardCard
                    title="LGAs"
                    value={stats.lgas}
                />

            </div>

            <div
                style={{
                    marginTop: "40px",
                    padding: "20px",
                    border: "1px solid #ddd",
                    borderRadius: "8px",
                    background: "#fafafa"
                }}
            >

                <h2>System Status</h2>

                <table
                    style={{
                        width: "100%",
                        borderCollapse: "collapse"
                    }}
                >

                    <tbody>

                    <tr>
                        <td><b>Backend</b></td>
                        <td style={{color: "green"}}>Running</td>
                    </tr>

                    <tr>
                        <td><b>Database</b></td>
                        <td style={{color: "green"}}>Connected</td>
                    </tr>

                    <tr>
                        <td><b>CSV Import</b></td>
                        <td style={{color: "green"}}>Operational</td>
                    </tr>

                    <tr>
                        <td><b>Prediction Engine</b></td>
                        <td style={{color: "orange"}}>Coming Soon</td>
                    </tr>

                    </tbody>

                </table>

            </div>

        </div>

    );

}

function DashboardCard({ title, value }) {

    return (

        <div
            style={{
                background: "white",
                borderRadius: "10px",
                padding: "25px",
                boxShadow: "0 2px 8px rgba(0,0,0,.1)"
            }}
        >

            <h3>{title}</h3>

            <h1
                style={{
                    color: "#1976d2",
                    marginTop: "15px"
                }}
            >
                {value}
            </h1>

        </div>

    );

}

export default Dashboard;