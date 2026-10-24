import { useAuth } from '../context/AuthContext'
import StudentCourseRegistration from './StudentCourseRegistration'
import InPersonRegistration from './InPersonRegistration'

function RegistrationEntry() {
    const { user } = useAuth()
    return user.role === 'STUDENT' ? <StudentCourseRegistration /> : <InPersonRegistration />
}

export default RegistrationEntry