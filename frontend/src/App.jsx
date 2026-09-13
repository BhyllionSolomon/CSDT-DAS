import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Students from './pages/Students'
import Courses from './pages/Courses'
import Sessions from './pages/Sessions'
import StudentResults from './pages/StudentResults'
import EnterResult from './pages/EnterResult'
import ResultVerification from './pages/ResultVerification'

function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Layout />}>
                    <Route index element={<Dashboard />} />
                    <Route path="students" element={<Students />} />
                    <Route path="students/:studentId/results" element={<StudentResults />} />
                    <Route path="courses" element={<Courses />} />
                    <Route path="sessions" element={<Sessions />} />
                    <Route path="enter-result" element={<EnterResult />} />
                    <Route path="verification" element={<ResultVerification />} />
                </Route>
            </Routes>
        </BrowserRouter>
    )
}

export default App