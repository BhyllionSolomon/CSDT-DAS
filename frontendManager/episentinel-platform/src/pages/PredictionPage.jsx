import { useState } from "react";

const API = "http://localhost:8081/api/predictions";

export default function PredictionPage() {

    const [form, setForm] = useState({

        disease: "Cholera",
        country: "Nigeria",
        state: "Lagos",
        lga: "Eti-Osa",

        year: 2025,
        epiWeek: 20,

        forecastHorizon: 1,

        suspectedCases: 120,
        confirmedCases: 35,
        deaths: 2,

        rainfall: 120.5,
        temperature: 28.4,
        humidity: 85

    });

    const [loading, setLoading] = useState(false);

    const [prediction, setPrediction] = useState(null);

    const [error, setError] = useState("");

    const handleChange = (e) => {

        setForm({

            ...form,

            [e.target.name]: e.target.value

        });

    };

    const predict = async () => {

        setLoading(true);

        setPrediction(null);

        setError("");

        try {

            const response = await fetch(API, {

                method: "POST",

                headers: {

                    "Content-Type": "application/json"

                },

                body: JSON.stringify({

                    ...form,

                    year: Number(form.year),
                    epiWeek: Number(form.epiWeek),
                    forecastHorizon: Number(form.forecastHorizon),

                    suspectedCases: Number(form.suspectedCases),
                    confirmedCases: Number(form.confirmedCases),
                    deaths: Number(form.deaths),

                    rainfall: Number(form.rainfall),
                    temperature: Number(form.temperature),
                    humidity: Number(form.humidity)

                })

            });

            if (!response.ok) {

                throw new Error("Prediction service unavailable");

            }

            const data = await response.json();

            setPrediction(data);

        }
        catch (err) {

            setError(err.message);

        }

        setLoading(false);

    };

    return (

        <div style={{padding:"40px"}}>

            <h2>Disease Forecast</h2>

            <hr/>

            {

                Object.keys(form).map(key => (

                    <div
                        key={key}
                        style={{marginBottom:"10px"}}
                    >

                        <label>

                            {key}

                        </label>

                        <br/>

                        <input

                            style={{
                                width:"400px",
                                padding:"8px"
                            }}

                            name={key}

                            value={form[key]}

                            onChange={handleChange}

                        />

                    </div>

                ))

            }

            <button

                onClick={predict}

                disabled={loading}

            >

                {loading ? "Predicting..." : "Predict"}

            </button>

            <br/>
            <br/>

            {

                error && (

                    <div
                        style={{
                            color:"red",
                            fontWeight:"bold"
                        }}
                    >

                        {error}

                    </div>

                )

            }

            {

                prediction && (

                    <div>

                        <h3>Prediction Result</h3>

                        <p>

                            Predicted Cases:
                            {" "}
                            {prediction.predictedCases}

                        </p>

                        <p>

                            Confidence:
                            {" "}
                            {prediction.confidenceScore}

                        </p>

                        <p>

                            Risk:
                            {" "}
                            {prediction.riskLevel}

                        </p>

                        <p>

                            Recommendation:
                            {" "}
                            {prediction.recommendation}

                        </p>

                        <p>

                            Model:
                            {" "}
                            {prediction.modelVersion}

                        </p>

                    </div>

                )

            }

        </div>

    );

}