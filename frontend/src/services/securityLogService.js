import api from './api'

export async function getSecurityLog() {
    return (await api.get('/security-log')).data
}