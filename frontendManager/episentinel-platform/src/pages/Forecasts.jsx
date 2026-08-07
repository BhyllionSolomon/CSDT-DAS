import { useState } from "react";
import axios from "axios";
import {
  Sparkles,
  TrendingUp,
  ShieldAlert,
  Syringe
} from "lucide-react";

import Card, { CardHeader } from "../components/ui/Card";
import Badge from "../components/ui/Badge";
import Button from "../components/ui/Button";
import ForecastChart from "../components/charts/ForecastChart";
import RiskGauge from "../components/ui/RiskGauge";
import { generateForecast } from "../services/predictionService";

import {
  DISEASES,
  STATES,
  LGAS_BY_STATE,
  FORECAST_CURVE
} from "../data/mockData";

const inputCls =
  "w-full bg-bgSunken border border-borderc rounded-lg px-3.5 py-2.5 text-sm text-textPrimary focus-ring focus:border-accent2/60";

export default function Forecasts() {

  const [disease, setDisease] = useState("Cholera");
  const [state, setState] = useState("Kano");
  const [lga, setLga] = useState(LGAS_BY_STATE["Kano"][0]);
  const [horizon, setHorizon] = useState("4 weeks");

  const [generated, setGenerated] = useState(false);
  const [loading, setLoading] = useState(false);

  const [prediction, setPrediction] = useState(null);

  const generateForecast = async () => {

    setLoading(true);

    try {

      const response = await axios.post(
        "http://localhost:8081/api/predictions",
        {

          disease: disease,
          country: "Nigeria",
          state: state,
          lga: lga,

          year: 2025,
          epiWeek: 20,

          forecastHorizon: parseInt(horizon.split(" ")[0]),

          suspectedCases: 120,
          confirmedCases: 35,
          deaths: 2,

          rainfall: 120.5,
          temperature: 28.4,
          humidity: 85

        }
      );

      setPrediction(response.data);

      setGenerated(true);

    }
    catch (error) {

      console.error(error);

      alert("Prediction service is unavailable.");

    }
    finally {

      setLoading(false);

    }

  };

  return (

    <div className="space-y-5">

      <CardHeader
        title="Forecast Panel"
        subtitle="Heterogeneous parallel-branch model — TCN + Bi-LSTM + Transformer"
      />

      <Card className="p-5">

        <div className="grid sm:grid-cols-2 lg:grid-cols-5 gap-4 items-end">

          <div>

            <label className="block text-xs font-semibold text-textSecondary mb-1.5">
              Disease
            </label>

            <select
              value={disease}
              onChange={(e) => setDisease(e.target.value)}
              className={inputCls}
            >

              {DISEASES.map((d) => (
                <option key={d}>{d}</option>
              ))}

            </select>

          </div>

          <div>

            <label className="block text-xs font-semibold text-textSecondary mb-1.5">
              State
            </label>

            <select
              value={state}
              onChange={(e) => {

                setState(e.target.value);

                setLga(LGAS_BY_STATE[e.target.value][0]);

              }}
              className={inputCls}
            >

              {STATES.map((s) => (

                <option key={s.name}>
                  {s.name}
                </option>

              ))}

            </select>

          </div>

          <div>

            <label className="block text-xs font-semibold text-textSecondary mb-1.5">
              LGA
            </label>

            <select
              value={lga}
              onChange={(e) => setLga(e.target.value)}
              className={inputCls}
            >

              {(LGAS_BY_STATE[state] || []).map((x) => (

                <option key={x}>
                  {x}
                </option>

              ))}

            </select>

          </div>

          <div>

            <label className="block text-xs font-semibold text-textSecondary mb-1.5">
              Forecast Horizon
            </label>

            <select
              value={horizon}
              onChange={(e) => setHorizon(e.target.value)}
              className={inputCls}
            >

              {[
                "2 weeks",
                "4 weeks",
                "8 weeks",
                "12 weeks"
              ].map((x) => (

                <option key={x}>
                  {x}
                </option>

              ))}

            </select>

          </div>

          <Button
            variant="primary"
            icon={Sparkles}
            disabled={loading}
            onClick={generateForecast}
          >

            {loading
              ? "Generating..."
              : "Generate Forecast"}

          </Button>

        </div>

      </Card>

      {generated && prediction && (

        <div className="grid grid-cols-1 xl:grid-cols-3 gap-5 animate-riseIn">

          <Card className="xl:col-span-2">

            <CardHeader
              title={`${disease} — ${state}`}
              subtitle={`${horizon} Forecast`}
              right={
                <Badge variant="accent" dot>

                  {prediction.modelVersion}

                </Badge>
              }
            />

            <div className="px-3 pb-4 pt-2">

              <ForecastChart
                data={FORECAST_CURVE}
              />

            </div>

          </Card>

          <div className="space-y-5">

            <Card className="p-5 flex flex-col items-center">

              <CardHeader title="Outbreak Risk" />

              <div className="mt-3">

                <RiskGauge

                  value={
                    prediction.riskLevel === "HIGH"
                      ? 90
                      : prediction.riskLevel === "MEDIUM"
                      ? 60
                      : 25
                  }

                  label={prediction.riskLevel}

                />

              </div>

            </Card>

            <Card className="p-5 space-y-3">

              <div className="flex justify-between">

                <span>

                  Predicted Cases

                </span>

                <span className="font-bold">

                  {prediction.predictedCases}

                </span>

              </div>

              <div className="flex justify-between">

                <span>

                  Confidence

                </span>

                <span className="font-bold text-green-600">

                  {(prediction.confidenceScore * 100).toFixed(1)}%

                </span>

              </div>

              <div className="flex justify-between">

                <span>

                  Risk Level

                </span>

                <span className="font-bold">

                  {prediction.riskLevel}

                </span>

              </div>

            </Card>

            <Card className="p-5">

              <div className="flex items-center gap-2 mb-3">

                <ShieldAlert
                  size={16}
                />

                <span className="font-semibold">

                  Recommendation

                </span>

              </div>

              <div className="flex items-start gap-2 text-sm">

                <Syringe
                  size={15}
                />

                {prediction.recommendation}

              </div>

            </Card>

          </div>

        </div>

      )}

    </div>

  );

}