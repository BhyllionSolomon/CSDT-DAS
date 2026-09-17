import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Students from './pages/Students'
import Courses from './pages/Courses'
import Sessions from './pages/Sessions'
import StudentResults from './pages/StudentResults'
import EnterResult from './pages/EnterResult'
import ResultVerification from './pages/ResultVerification'
import BulkUploadStudents from './pages/BulkUploadStudents'
import CreateCourse from './pages/CreateCourse'
import UploadResultsCsv from './pages/UploadResultsCsv'
import BulkEnterResults from './pages/BulkEnterResults'
import StudentCourseRegistration from './pages/StudentCourseRegistration'

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/login" element={<Login />} />

                    <Route
                        path="/"
                        element={
                            <ProtectedRoute>
                                <Layout />
                            </ProtectedRoute>
                        }
                    >
                        <Route index element={<Dashboard />} />
                        <Route path="students" element={<Students />} />
                        <Route path="students/:studentId/results" element={<StudentResults />} />
                        <Route path="bulk-upload-students" element={<BulkUploadStudents />} />
                        <Route path="courses" element={<Courses />} />
                        <Route path="create-course" element={<CreateCourse />} />
                        <Route path="sessions" element={<Sessions />} />
                        <Route path="enter-result" element={<EnterResult />} />
                        <Route path="bulk-enter-results" element={<BulkEnterResults />} />
                        <Route path="upload-results-csv" element={<UploadResultsCsv />} />
                        <Route path="verification" element={<ResultVerification />} />
                        <Route path="register-courses" element={<StudentCourseRegistration />} />
                    </Route>
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    )
}

export default App