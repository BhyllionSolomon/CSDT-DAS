import {
    LayoutDashboard, GraduationCap, BookOpen, Users, UserCog, ClipboardCheck,
    FileSpreadsheet, Video, FlaskConical, Activity,
} from 'lucide-react'

export const LAB_ROLES = ['LAB_OFFICER', 'TECHNICAL_STAFF']

const performance = { to: '/analytics/performance', label: 'Performance' }

export const hodGroups = [
    { label: 'Department', items: [
            { to: '/programme-overview', label: 'Programme Overview' },
            { to: '/sessions', label: 'Academic Sessions' },
            { to: '/students-per-session', label: 'Students Per Session' },
        ] },
    { label: 'Reports', items: [
            { to: '/reports', label: 'Broadsheet & Lists' },
            { to: '/student-report', label: 'Student Result' },
        ] },
    { label: 'Analytics', items: [performance] },
    { label: 'Students', items: [
            { to: '/students', label: 'All Students' },
            { to: '/bulk-upload-students', label: 'Bulk Upload Students' },
            { to: '/register-courses', label: 'Course Registration' },
            { to: '/registration-status', label: 'Registration Status' },
        ] },
    { label: 'Courses', items: [
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
    { label: 'Results', items: [
            { to: '/enter-result', label: 'Enter Result' },
            { to: '/bulk-enter-results', label: 'Bulk Enter Results' },
            { to: '/upload-results-csv', label: 'Upload Results' },
        ] },
    { label: 'Exams', items: [
            { to: '/verification', label: 'Result Verification' },
        ] },
    { label: 'Laboratory', items: [
            { to: '/lab-staff', label: 'Manage Lab Staff' },
        ] },
]

export const lecturerGroups = [
    { label: 'Courses', items: [
            { to: '/', label: 'My Courses' },
            { to: '/claim-courses', label: 'Claim Course' },
        ] },
    { label: 'Teaching', items: [
            { to: '/teaching', label: 'Materials & Online Classes' },
        ] },
    { label: 'Students', items: [
            { to: '/lecturer-students', label: 'My Students' },
        ] },
    { label: 'Attendance', items: [
            { to: '/attendance', label: 'Take Attendance', disabled: true },
        ] },
    { label: 'Continuous Assessment', items: [
            { to: '/ca/practicals', label: 'Practicals', disabled: true },
            { to: '/ca/assignments', label: 'Assignments', disabled: true },
            { to: '/ca/presentation', label: 'Class Presentation', disabled: true },
            { to: '/ca/test', label: 'Test', disabled: true },
        ] },
    { label: 'Exam Scoresheet', items: [
            { to: '/exam-scoresheet', label: 'Enter Exam Scores' },
        ] },
]

// A level adviser is also a lecturer, so every lecturer tool is here as well.
// "My Courses" lives at /my-courses because "/" is the adviser's programme home.
export const adviserGroups = [
    { label: 'Programme', items: [{ to: '/', label: 'My Programme' }] },
    { label: 'Reports', items: [
            { to: '/reports', label: 'Broadsheet & Lists' },
            { to: '/student-report', label: 'Student Result' },
        ] },
    { label: 'Analytics', items: [performance] },
    { label: 'Students', items: [
            { to: '/register-courses', label: 'Course Registration' },
            { to: '/registration-status', label: 'Registration Status' },
            { to: '/lecturer-students', label: 'My Students' },
        ] },
    { label: 'Results', items: [{ to: '/verification', label: 'Result Verification' }] },
    { label: 'Courses', items: [
            { to: '/my-courses', label: 'My Courses' },
            { to: '/claim-courses', label: 'Claim Course' },
        ] },
    { label: 'Teaching', items: [
            { to: '/teaching', label: 'Materials & Online Classes' },
        ] },
    { label: 'Attendance', items: [
            { to: '/attendance', label: 'Take Attendance', disabled: true },
        ] },
    { label: 'Continuous Assessment', items: [
            { to: '/ca/practicals', label: 'Practicals', disabled: true },
            { to: '/ca/assignments', label: 'Assignments', disabled: true },
            { to: '/ca/presentation', label: 'Class Presentation', disabled: true },
            { to: '/ca/test', label: 'Test', disabled: true },
        ] },
    { label: 'Exam Scoresheet', items: [
            { to: '/exam-scoresheet', label: 'Enter Exam Scores' },
        ] },
]

export const studentGroups = [
    { label: 'Academics', items: [
            { to: '/register-courses', label: 'Course Registration' },
            { to: '/my-carryovers', label: 'Carry-Over Courses' },
            { to: '/my-adviser', label: 'My Level Adviser' },
        ] },
    { label: 'Learning', items: [
            { to: '/my-learning', label: 'Class Materials & Online Classes' },
        ] },
    { label: 'Attendance', items: [
            { to: '/attendance', label: 'My Attendance', disabled: true },
        ] },
    { label: 'Continuous Assessment', items: [
            { to: '/my-ca', label: 'My C.A. Scores', disabled: true },
        ] },
    { label: 'My Results', items: [
            { to: '/upload-my-results', label: 'Upload & Analyse Results' },
        ] },
]

export const labGroups = [
    { label: 'Laboratory', items: [{ to: '/', label: 'Lab Home' }] },
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
        case 'TECHNICAL_STAFF':
            return labGroups
        default:
            return []
    }
}

export const GROUP_ICONS = {
    'Department': LayoutDashboard,
    'Reports': FileSpreadsheet,
    'Analytics': Activity,
    'Students': GraduationCap,
    'Courses': BookOpen,
    'Lecturers': Users,
    'Level Advisers': UserCog,
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