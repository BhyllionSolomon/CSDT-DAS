import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const LAB_ROLES = ['LAB_OFFICER', 'TECHNICAL_STAFF']

function ProtectedRoute({ children }) {
    const { user } = useAuth()
    const location = useLocation()

    if (!user) return <Navigate to="/login" replace />

    if (LAB_ROLES.includes(user.role) && location.pathname !== '/') {
        return <Navigate to="/" replace />
    }

    return children
}

export default ProtectedRoute