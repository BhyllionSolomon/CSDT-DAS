import {
    LayoutDashboard, GraduationCap, BookOpen, Users, UserCog, ClipboardCheck,
    FileSpreadsheet, Video, FlaskConical, Activity, Award, Bell,
} from 'lucide-react'

export const LAB_ROLES = ['LAB_OFFICER', 'TECHNICAL_STAFF']

const performance = { to: '/analytics/performance', label: 'Performance' }
const notices = { label: 'Notices', items: [{ to: '/notices', label: 'Notice Board' }] }

export const hodGroups = [
    notices,
    { label: 'Department', items: [
            { to: '/programme-overview', label: 'Programme Overview' },
            { to: '/sessions', label: 'Academic Sessions' },
            { to: '/start-session', label: 'Start New Session' },
            { to: '/cohort-guide', label: 'Cohort Guide' },
            { to: '/timetable-admin', label: 'Timetable' },
            { to: '/students-per-session', label: 'Students Per Session' },
            { to: '/home-slides', label: 'Home Page Images' },
            { to: '/security-log', label: 'Security Log' },
            { to: '/system-check', label: 'System Check' },
        ] },
    { label: 'Reports', items: [
            { to: '/reports', label: 'Broadsheet & Lists' },
            { to: '/student-report', label: 'Student Result' },
        ] },
    { label: 'Analytics', items: [
            performance,
            { to: '/lecturer-performance', label: 'Lecturer Performance' },
            { to: '/feedback-admin', label: 'Lecturer Feedback' },
            { to: '/attendance', label: 'Attendance' },
            { to: '/continuous-assessment', label: 'Continuous Assessment' },
            { to: '/carryover-monitor', label: 'Carry-Over Monitor' },
        ] },
    { label: 'Students', items: [
            { to: '/students', label: 'All Students' },
            { to: '/graduates', label: 'Graduates' },
            { to: '/bulk-upload-students', label: 'Bulk Upload Students' },
            { to: '/registration-approvals', label: 'Registration Approvals' },
            { to: '/register-courses', label: 'Register in Person' },
            { to: '/registration-status', label: 'Registration Status' },
            { to: '/activation-codes', label: 'Activation Codes' },
        ] },
    { label: 'Courses', items: [
            { to: '/curriculum', label: 'Curriculum by Level' },
            { to: '/courses', label: 'All Courses' },
            { to: '/create-course', label: 'Create Course' },
            { to: '/bulk-upload-courses', label: 'Bulk Upload Courses' },
            { to: '/set-unit-requirements', label: 'Set Required Units' },
        ] },
    { label: 'Lecturers', items: [
            { to: '/course-allocations', label: 'Course Allocations' },
            { to: '/bulk-upload-allocation', label: 'Bulk Upload Allocation' },
            { to: '/users', label: 'Manage Lecturers' },
            { to: '/teaching', label: 'Teaching Activity' },
        ] },
    { label: 'Level Advisers', items: [
            { to: '/advisers', label: 'Manage Advisers' },
        ] },
    { label: 'Final-Year Projects', items: [
            { to: '/final-year-projects', label: 'Assign Supervisors' },
            { to: '/supervisor-export', label: 'Allocation to Word' },
            { to: '/project-cases', label: 'All Project Cases' },
            { to: '/project-alerts', label: 'Project Alerts' },
            { to: '/project-survey-admin', label: 'Integrity Form Results' },
        ] },
    { label: 'Results', items: [
            { to: '/enter-result', label: 'Enter Result' },
            { to: '/bulk-enter-results', label: 'Bulk Enter Results' },
            { to: '/upload-results-csv', label: 'Upload Results' },
        ] },
    { label: 'Exams', items: [
            { to: '/verification', label: 'Result Verification' },
            { to: '/examination-management', label: 'Exam Papers & Vetting' },
            { to: '/exam-operations', label: 'Timetable, Dispatch & Marking' },
        ] },
    { label: 'Laboratory', items: [
            { to: '/lab-staff', label: 'Manage Lab Staff' },
            { to: '/lab-manual-workflow', label: 'Manual Approval & Dispatch' },
            { to: '/lab-assets', label: 'Equipment & Assets' },
            { to: '/lab-manuals', label: 'Manual Sales & Stock' },
            { to: '/lab-finance', label: 'Manual Finance' },
        ] },
]

export const lecturerGroups = [
    notices,
    { label: 'Courses', items: [
            { to: '/', label: 'My Courses' },
            { to: '/claim-courses', label: 'Claim Course' },
        ] },
    { label: 'Teaching', items: [
            { to: '/teaching', label: 'Materials & Online Classes' },
            { to: '/lab-manual-workflow', label: 'Upload Lab Manual' },
            { to: '/my-timetable', label: 'My Timetable Choices' },
            { to: '/timetable', label: 'Department Timetable' },
            { to: '/project-cases', label: 'My Project Students' },
            { to: '/project-alerts', label: 'Project Alerts' },
            { to: '/supervisor-export', label: 'Project Allocation (Word)' },
            { to: '/my-feedback', label: 'My Feedback' },
            { to: '/lecturer-performance', label: 'My Performance' },
        ] },
    { label: 'Students', items: [
            { to: '/lecturer-students', label: 'My Students' },
        ] },
    { label: 'Attendance', items: [
            { to: '/attendance', label: 'Take Attendance' },
        ] },
    { label: 'Continuous Assessment', items: [
            { to: '/continuous-assessment', label: 'Assignments, Submissions & C.A.' },
        ] },
    { label: 'Exam Scoresheet', items: [
            { to: '/exam-scoresheet', label: 'Enter Exam Scores' },
            { to: '/examination-management', label: 'Question Paper Submission' },
            { to: '/exam-operations', label: 'My Script Collection & Return' },
        ] },
]

// A level adviser is also a lecturer, so every lecturer tool is here as well.
// "My Courses" lives at /my-courses because "/" is the adviser's programme home.
export const adviserGroups = [
    notices,
    { label: 'Programme', items: [{ to: '/', label: 'My Programme' }] },
    { label: 'Reports', items: [
            { to: '/reports', label: 'Broadsheet & Lists' },
            { to: '/student-report', label: 'Student Result' },
        ] },
    { label: 'Analytics', items: [
            performance,
            { to: '/carryover-monitor', label: 'Carry-Over Monitor' },
        ] },
    { label: 'Students', items: [
            { to: '/register-courses', label: 'Register in Person' },
            { to: '/registration-status', label: 'Registration Status' },
            { to: '/lecturer-students', label: 'My Students' },
            { to: '/activation-codes', label: 'Activation Codes' },
        ] },
    { label: 'Results', items: [{ to: '/verification', label: 'Result Verification' }] },
    { label: 'Courses', items: [
            { to: '/my-courses', label: 'My Courses' },
            { to: '/claim-courses', label: 'Claim Course' },
        ] },
    { label: 'Teaching', items: [
            { to: '/teaching', label: 'Materials & Online Classes' },
            { to: '/my-timetable', label: 'My Timetable Choices' },
            { to: '/timetable', label: 'Department Timetable' },
            { to: '/project-cases', label: 'My Project Students' },
            { to: '/project-alerts', label: 'Project Alerts' },
            { to: '/supervisor-export', label: 'Project Allocation (Word)' },
            { to: '/my-feedback', label: 'My Feedback' },
            { to: '/lecturer-performance', label: 'My Performance' },
        ] },
    { label: 'Attendance', items: [
            { to: '/attendance', label: 'Take Attendance' },
        ] },
    { label: 'Continuous Assessment', items: [
            { to: '/continuous-assessment', label: 'Record C.A. Scores' },
        ] },
    { label: 'Exam Scoresheet', items: [
            { to: '/exam-scoresheet', label: 'Enter Exam Scores' },
            { to: '/exam-operations', label: 'My Script Collection & Return' },
        ] },
]

// "gated" items are locked for a student until the H.O.D approves their registration.
export const studentGroups = [
    notices,
    { label: 'Academics', items: [
            { to: '/register-courses', label: 'Course Registration' },
            { to: '/my-registration', label: 'Registration & Payment' },
            { to: '/timetable', label: 'Teaching Timetable' },
            { to: '/my-exam-timetable', label: 'My Examination Timetable' },
            { to: '/my-carryovers', label: 'Carry-Over Courses' },
            { to: '/my-adviser', label: 'My Level Adviser' },
            { to: '/my-project-supervisor', label: 'My Project Supervisor' },
            { to: '/my-project', label: 'My Project' },
            { to: '/project-survey', label: 'Project Integrity Form' },
            { to: '/rate-lecturers', label: 'Rate My Lecturers' },
        ] },
    { label: 'Learning', items: [
            { to: '/my-learning', label: 'Learning & Get Lab Manual', gated: true },
        ] },
    { label: 'Attendance', items: [
            { to: '/my-attendance', label: 'My Attendance' },
        ] },
    { label: 'Continuous Assessment', items: [
            { to: '/my-ca', label: 'Assignments & My C.A. Scores' },
        ] },
    { label: 'My Results', items: [
            { to: '/my-progress', label: 'My Progress', gated: true },
            { to: '/upload-my-results', label: 'Upload & Analyse Results', gated: true },
        ] },
]

export const labGroups = [
    notices,
    { label: 'Laboratory', items: [
            { to: '/', label: 'Lab Home' },
            { to: '/lab-manual-workflow', label: 'Incoming Course Manuals' },
            { to: '/exam-operations', label: 'Assigned Exam Dispatch' },
            { to: '/lab-assets', label: 'Equipment & Assets' },
            { to: '/lab-manuals', label: 'Manual Sales & Stock' },
            { to: '/lab-finance', label: 'Manual Finance' },
        ] },
]

// Technical staff record sales but never see the money or the finance page.
export const technicalGroups = [
    notices,
    { label: 'Laboratory', items: [
            { to: '/', label: 'Lab Home' },
            { to: '/lab-manual-workflow', label: 'Incoming Course Manuals' },
            { to: '/exam-operations', label: 'Assigned Exam Dispatch' },
            { to: '/lab-manuals', label: 'Manual Sales & Stock' },
        ] },
]

export function getGroupsForRole(role) {
    switch (role) {
        case 'HOD':
        case 'ADMIN':
            return hodGroups
        case 'LECTURER':
        case 'ADJUNCT':
            return lecturerGroups
        case 'LEVEL_ADVISER':
            return adviserGroups
        case 'STUDENT':
            return studentGroups
        case 'LAB_OFFICER':
            return labGroups
        case 'TECHNICAL_STAFF':
            return technicalGroups
        default:
            return []
    }
}

export const GROUP_ICONS = {
    'Notices': Bell,
    'Department': LayoutDashboard,
    'Reports': FileSpreadsheet,
    'Analytics': Activity,
    'Students': GraduationCap,
    'Courses': BookOpen,
    'Lecturers': Users,
    'Level Advisers': UserCog,
    'Final-Year Projects': Award,
    'Results': ClipboardCheck,
    'Exams': FileSpreadsheet,
    'Teaching': Video,
    'Learning': Video,
    'Attendance': ClipboardCheck,
    'Continuous Assessment': ClipboardCheck,
    'Exam Scoresheet': FileSpreadsheet,
    'Programme': BookOpen,
    'Academics': GraduationCap,
    'My Results': FileSpreadsheet,
    'Laboratory': FlaskConical,
}