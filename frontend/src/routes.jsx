import RoleDashboard from './pages/RoleDashboard'
import LecturerDashboard from './pages/LecturerDashboard'
import MyAccount from './pages/MyAccount'
import Students from './pages/Students'
import StudentResults from './pages/StudentResults'
import StudentsPerSession from './pages/StudentsPerSession'
import BulkUploadStudents from './pages/BulkUploadStudents'
import Courses from './pages/Courses'
import CourseCurriculum from './pages/CurriculumPlanner'
import CreateCourse from './pages/CreateCourse'
import BulkUploadCourses from './pages/BulkUploadCourses'
import Sessions from './pages/Sessions'
import StartNewSession from './pages/StartNewSession'
import TimetableAdmin from './pages/TimetableAdmin'
import LecturerTimetable from './pages/LecturerTimetable'
import TimetableView from './pages/TimetableView'
import EnterResult from './pages/EnterResult'
import BulkEnterResults from './pages/BulkEnterResults'
import UploadResultsCsv from './pages/UploadResultsCsv'
import ResultVerification from './pages/ResultVerification'
import RegistrationEntry from './pages/RegistrationEntry'
import MyRegistration from './pages/MyRegistration'
import RegistrationApprovals from './pages/RegistrationApprovals'
import RegistrationStatus from './pages/RegistrationStatus'
import ProgrammeOverview from './pages/ProgrammeOverview'
import CourseAllocations from './pages/CourseAllocations'
import BulkUploadAllocation from './pages/BulkUploadAllocation'
import ClaimCourses from './pages/ClaimCourses'
import SetUnitRequirements from './pages/SetUnitRequirements'
import LecturerStudents from './pages/LecturerStudents'
import ManageLecturers from './pages/ManageLecturers'
import ManageAdvisers from './pages/ManageAdvisers'
import ManageLabStaff from './pages/ManageLabStaff'
import ExamScoresheet from './pages/ExamScoresheet'
import StudentCarryOvers from './pages/StudentCarryOvers'
import UploadMyResults from './pages/UploadMyResults'
import Reports from './pages/Reports'
import StudentReport from './pages/StudentReport'
import LecturerTeaching from './pages/LecturerTeaching'
import StudentLearning from './pages/StudentLearning'
import MyLevelAdviser from './pages/MyLevelAdviser'
import PerformanceAnalytics from './pages/PerformanceAnalytics'

// Every page behind the sign-in is registered here. A new page = one new line.
export const protectedRoutes = [
    { index: true, element: <RoleDashboard /> },
    { path: 'my-courses', element: <LecturerDashboard /> },
    { path: 'my-account', element: <MyAccount /> },
    { path: 'students', element: <Students /> },
    { path: 'students/:studentId/results', element: <StudentResults /> },
    { path: 'students-per-session', element: <StudentsPerSession /> },
    { path: 'bulk-upload-students', element: <BulkUploadStudents /> },
    { path: 'courses', element: <Courses /> },
    { path: 'curriculum', element: <CourseCurriculum /> },
    { path: 'create-course', element: <CreateCourse /> },
    { path: 'bulk-upload-courses', element: <BulkUploadCourses /> },
    { path: 'sessions', element: <Sessions /> },
    { path: 'start-session', element: <StartNewSession /> },
    { path: 'timetable-admin', element: <TimetableAdmin /> },
    { path: 'my-timetable', element: <LecturerTimetable /> },
    { path: 'timetable', element: <TimetableView /> },
    { path: 'enter-result', element: <EnterResult /> },
    { path: 'bulk-enter-results', element: <BulkEnterResults /> },
    { path: 'upload-results-csv', element: <UploadResultsCsv /> },
    { path: 'verification', element: <ResultVerification /> },
    { path: 'register-courses', element: <RegistrationEntry /> },
    { path: 'my-registration', element: <MyRegistration /> },
    { path: 'registration-approvals', element: <RegistrationApprovals /> },
    { path: 'registration-status', element: <RegistrationStatus /> },
    { path: 'programme-overview', element: <ProgrammeOverview /> },
    { path: 'course-allocations', element: <CourseAllocations /> },
    { path: 'bulk-upload-allocation', element: <BulkUploadAllocation /> },
    { path: 'claim-courses', element: <ClaimCourses /> },
    { path: 'set-unit-requirements', element: <SetUnitRequirements /> },
    { path: 'lecturer-students', element: <LecturerStudents /> },
    { path: 'users', element: <ManageLecturers /> },
    { path: 'advisers', element: <ManageAdvisers /> },
    { path: 'lab-staff', element: <ManageLabStaff /> },
    { path: 'exam-scoresheet', element: <ExamScoresheet /> },
    { path: 'my-carryovers', element: <StudentCarryOvers /> },
    { path: 'upload-my-results', element: <UploadMyResults /> },
    { path: 'reports', element: <Reports /> },
    { path: 'student-report', element: <StudentReport /> },
    { path: 'teaching', element: <LecturerTeaching /> },
    { path: 'my-learning', element: <StudentLearning /> },
    { path: 'my-adviser', element: <MyLevelAdviser /> },
    { path: 'analytics/performance', element: <PerformanceAnalytics /> },
]