import api from './api'

export async function getLabAssets() { return (await api.get('/lab/assets')).data }
export async function createLabAsset(payload) { return (await api.post('/lab/assets', payload)).data }
export async function updateLabAsset(id, payload) { return (await api.put('/lab/assets/' + id, payload)).data }
export async function updateLabAssetCondition(id, conditionStatus, notes) {
    return (await api.patch('/lab/assets/' + id + '/condition', { conditionStatus, notes })).data
}
