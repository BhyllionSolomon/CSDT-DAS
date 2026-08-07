import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ThemeProvider } from './context/ThemeContext'
import { AuthProvider, useAuth } from './context/AuthContext'
import AppLayout from './components/layout/AppLayout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import DailySurveillance from './pages/DailySurveillance'
import DiseaseRecords from './pages/DiseaseRecords'
import LaboratoryResults from './pages/LaboratoryResults'
import CaseInvestigation from './pages/CaseInvestigation'
import Forecasts from './pages/Forecasts'
import Hotspots from './pages/Hotspots'
import MapsPage from './pages/MapsPage'
import Analytics from './pages/Analytics'
import DecisionSupport from './pages/DecisionSupport'
import DatasetHistory from './pages/DatasetHistory'
import ModelManagement from './pages/ModelManagement'
import Settings from './pages/Settings'

function RequireAuth({ children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to="/login" replace />
}

function Shell() {
  const { user } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="surveillance" element={<DailySurveillance />} />
        <Route path="records" element={<DiseaseRecords />} />
        <Route path="laboratory" element={<LaboratoryResults />} />
        <Route path="investigation" element={<CaseInvestigation />} />
        <Route path="forecasts" element={<Forecasts />} />
        <Route path="hotspots" element={<Hotspots />} />
        <Route path="maps" element={<MapsPage />} />
        <Route path="analytics" element={<Analytics />} />
        <Route path="decision-support" element={<DecisionSupport />} />
        <Route path="dataset-history" element={<DatasetHistory />} />
        <Route path="model-management" element={<ModelManagement />} />
        <Route path="settings" element={<Settings />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <Shell />
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  )
}
