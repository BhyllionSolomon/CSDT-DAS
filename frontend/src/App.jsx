import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import Home from './pages/Home'
import RoleDashboard from './pages/RoleDashboard'
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
import ProgrammeOverview from './pages/ProgrammeOverview'
import CourseAllocations from './pages/CourseAllocations'
import ClaimCourses from './pages/ClaimCourses'
import SetUnitRequirements from './pages/SetUnitRequirements'
import LecturerStudents from './pages/LecturerStudents'
import BulkUploadCourses from './pages/BulkUploadCourses'
import ManageLecturers from './pages/ManageLecturers'
import ManageAdvisers from './pages/ManageAdvisers'
import { ThemeProvider } from './context/ThemeContext'
import ExamScoresheet from './pages/ExamScoresheet'
import StudentCarryOvers from './pages/StudentCarryOvers'
import UploadMyResults from './pages/UploadMyResults'

function App() {
    return (
        <ThemeProvider>
            <AuthProvider>
                <BrowserRouter>
                    <Routes>
                        <Route path="/login" element={<Home />} />

                        <Route
                            path="/"
                            element={
                                <ProtectedRoute>
                                    <Layout />
                                </ProtectedRoute>
                            }
                        >
                            <Route index element={<RoleDashboard />} />
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
                            <Route path="programme-overview" element={<ProgrammeOverview />} />
                            <Route path="course-allocations" element={<CourseAllocations />} />
                            <Route path="claim-courses" element={<ClaimCourses />} />
                            <Route path="set-unit-requirements" element={<SetUnitRequirements />} />
                            <Route path="lecturer-students" element={<LecturerStudents />} />
                            <Route path="bulk-upload-courses" element={<BulkUploadCourses />} />
                            <Route path="users" element={<ManageLecturers />} />
                            <Route path="advisers" element={<ManageAdvisers />} />
                            <Route path="exam-scoresheet" element={<ExamScoresheet />} />
                            <Route path="my-carryovers" element={<StudentCarryOvers />} />
                            <Route path="upload-my-results" element={<UploadMyResults />} />


                        </Route>
                    </Routes>
                </BrowserRouter>
            </AuthProvider>
        </ThemeProvider>
    )
}

export default App