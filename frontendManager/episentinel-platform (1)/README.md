# EpiSentinel — AI-Driven Epidemiological Surveillance & Disease Forecasting Platform

A production-grade React frontend for a national disease-surveillance and
AI forecasting platform, styled for WHO / NCDC / ministry-of-health use.

## Stack
React 19 · Vite · Tailwind CSS · React Router · Recharts · Lucide Icons

## Getting started

```bash
npm install
npm run dev
```

Open the printed local URL. Sign in with any username/password (the login
screen is a fully designed mock — there is no backend).

## Structure

```
src/
  components/
    layout/     Sidebar, TopBar, Breadcrumbs, AppLayout
    ui/         Card, Badge, Button, KpiCard, RiskGauge, NigeriaMap, Skeleton
    charts/     Recharts wrappers (trend, pie, forecast, bar, stacked area, heatmap)
  context/      ThemeContext (5 themes, persisted), AuthContext (mock session)
  data/         mockData.js — all demo data lives here, swap for live API calls
  pages/        One file per sidebar route
```

## Design notes

- **Themes**: Light, Blue (default), Green, Dark, Neon — switch instantly via
  the palette icon in the top bar or Settings → Theme. Selection persists in
  `localStorage`.
- **Signature motif**: every KPI card carries a thin animated "epidemic curve"
  stroke across its top edge — a nod to the case curves this platform exists
  to forecast.
- **Map**: `NigeriaMap` is a lightweight schematic (SVG silhouette + projected
  state markers), not a GeoJSON choropleth — swap in a real topology (e.g.
  TopoJSON + d3-geo) for production geographic precision.
- **No backend**: all data is in `src/data/mockData.js`. Replace with Axios
  calls to your API; the component props are already shaped for that swap.

## Extending

Each sidebar page is self-contained and reuses the same `Card`, `Badge`,
`Button`, and chart primitives — new pages should follow the same pattern
rather than introducing one-off styles.
