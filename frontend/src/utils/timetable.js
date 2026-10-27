export const DAYS = [
    ['MON', 'MONDAY'], ['TUE', 'TUESDAY'], ['WED', 'WEDNESDAY'], ['THU', 'THURSDAY'], ['FRI', 'FRIDAY'],
]

// 08:00, 08:30 ... 19:00
export const TIME_OPTIONS = (() => {
    const out = []
    for (let m = 480; m <= 1140; m += 30) {
        out.push(`${String(Math.floor(m / 60)).padStart(2, '0')}:${m % 60 === 0 ? '00' : '30'}`)
    }
    return out
})()

export const inTwoWeeks = () => new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10)

export const minutes = (t) => {
    const [h, m] = t.split(':').map(Number)
    return h * 60 + m
}

export const prettyTime = (t) => {
    const [h, m] = t.split(':').map(Number)
    const hour = h > 12 ? h - 12 : h
    return m ? `${hour}:${String(m).padStart(2, '0')}` : `${hour}`
}