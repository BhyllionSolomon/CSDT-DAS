import api from './api'

export async function getCarryOverMonitor() {
    return (await api.get('/carryover-monitor')).data
}