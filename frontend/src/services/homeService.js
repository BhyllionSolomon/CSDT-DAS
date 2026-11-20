import api, { API_URL } from './api'

export async function getPublicSlides() { return (await api.get('/auth/home/slides')).data }
export async function listSlides() { return (await api.get('/home-slides')).data }
export async function createSlide(payload) { return (await api.post('/home-slides', payload)).data }
export async function updateSlide(id, payload) { return (await api.put(`/home-slides/${id}`, payload)).data }
export async function uploadSlideImage(id, blob) {
    return (await api.put(`/home-slides/${id}/image`, blob, { headers: { 'Content-Type': 'application/octet-stream' } })).data
}
export async function removeSlideImage(id) { return (await api.delete(`/home-slides/${id}/image`)).data }
export async function moveSlide(id, direction) { return (await api.post(`/home-slides/${id}/move`, { direction })).data }
export async function deleteSlide(id) { return (await api.delete(`/home-slides/${id}`)).data }

// The version changes whenever the picture does, so browsers never show an old one.
export const slideImageUrl = (s) => `${API_URL || '/api'}/auth/home/slides/${s.id}/image?v=${s.version}`