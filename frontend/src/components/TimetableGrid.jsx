import { useMemo } from 'react'
import { DAYS, minutes, prettyTime } from '../utils/timetable'

const BANDS = [
    { key: 'b1', label: '8 – 10', from: 480, to: 600 },
    { key: 'b2', label: '10 – 12', from: 600, to: 720 },
    { key: 'b3', label: '12 – 1', from: 720, to: 780 },
    { key: 'brk', label: '1 – 2', from: 780, to: 840, brk: true },
    { key: 'b4', label: '2 – 3', from: 840, to: 900 },
    { key: 'b5', label: '3 – 4', from: 900, to: 960 },
    { key: 'b6', label: '4 – 6', from: 960, to: 1080 },
    { key: 'b7', label: '6 – 7', from: 1080, to: 1140 },
]

const th = 'border border-black px-1.5 py-1 text-center font-bold'
const td = 'border border-black px-1.5 py-1 align-top'

export function printLandscape() {
    const style = document.createElement('style')
    style.media = 'print'
    style.textContent = '@page { size: A4 landscape; margin: 8mm; }'
    document.head.appendChild(style)
    window.print()
    setTimeout(() => style.remove(), 500)
}

function bandOf(entry) {
    const start = minutes(entry.startTime)
    return BANDS.find((b) => !b.brk && start >= b.from && start < b.to)
}

function label(entry, band) {
    const exact = band.from === minutes(entry.startTime) && band.to === minutes(entry.endTime)
    const time = exact ? '' : ` [${prettyTime(entry.startTime)}–${prettyTime(entry.endTime)}]`
    return `${entry.courseCode}${entry.kind === 'PRACTICAL' ? ' (P)' : ''} (${entry.room})${time}`
}

function TimetableGrid({ entries, sessionName, semester, onSelect, selectedId }) {
    const cells = useMemo(() => {
        const map = new Map()
        for (const e of entries) {
            const band = bandOf(e)
            if (!band) continue
            const key = `${e.day}|${band.key}`
            if (!map.has(key)) map.set(key, [])
            map.get(key).push({ e, band })
        }
        return map
    }, [entries])

    return (
        <div className="bg-white text-black p-4 rounded-lg print-area">
            <div className="text-center mb-3">
                <p className="font-bold text-base">KOLADAISI UNIVERSITY</p>
                <p className="font-semibold text-sm">DEPARTMENT OF COMPUTING SCIENCE &amp; DIGITAL TECHNOLOGY</p>
                <p className="font-semibold text-sm">
                    {semester === 'SECOND' ? 'SECOND' : 'FIRST'} SEMESTER {sessionName} SESSION
                </p>
                <p className="font-semibold text-sm">LECTURE TIME-TABLE</p>
            </div>

            <div className="overflow-x-auto">
                <table className="w-full border-collapse text-[11px]">
                    <thead>
                    <tr>
                        <th className={th}>DAY / TIME</th>
                        {BANDS.map((b) => <th key={b.key} className={th}>{b.label}</th>)}
                    </tr>
                    </thead>
                    <tbody>
                    {DAYS.map(([code, name]) => (
                        <tr key={code}>
                            <td className={`${td} font-bold`}>{name}</td>
                            {BANDS.map((b) => {
                                if (b.brk) {
                                    return <td key={b.key} className={`${td} text-center text-[10px] font-bold bg-gray-200`}>BREAK</td>
                                }
                                const items = cells.get(`${code}|${b.key}`) || []
                                return (
                                    <td key={b.key} className={`${td} min-w-[96px]`}>
                                        {items.map(({ e, band }) => (
                                            onSelect ? (
                                                <button key={e.id} type="button" onClick={() => onSelect(e)}
                                                        className={`block w-full text-left leading-tight py-0.5 hover:underline ${selectedId === e.id ? 'font-bold underline' : ''}`}>
                                                    {label(e, band)}
                                                </button>
                                            ) : (
                                                <p key={e.id} className="leading-tight py-0.5">{label(e, band)}</p>
                                            )
                                        ))}
                                    </td>
                                )
                            })}
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>

            <p className="text-[10px] mt-3">
                Each class shows the course code and its room. (P) marks a practical. A time in brackets appears when a class does not fill the whole band.
            </p>
        </div>
    )
}

export default TimetableGrid