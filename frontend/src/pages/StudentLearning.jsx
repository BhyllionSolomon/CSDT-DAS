import { useEffect, useState } from 'react'
import { listMyMaterials, downloadMaterial, messageFrom } from '../services/materialService'
import { listMyClasses, joinOnlineClass } from '../services/onlineClassService'
import { listStudentLabManualOptions } from '../services/studentLabManualCatalogueService'

const STATUS_STYLE = {
    SCHEDULED: 'bg-amber-100 text-amber-700',
    LIVE: 'bg-green-100 text-green-700',
    ENDED: 'bg-slate-200 text-slate-600',
}

function formatDate(value) {
    return value ? new Date(value).toLocaleString() : '—'
}

function StudentLearning() {
    const [tab, setTab] = useState('classes')
    const [labManuals, setLabManuals] = useState([])
    const [selectedManual, setSelectedManual] = useState(null)
    const [materials, setMaterials] = useState([])
    const [classes, setClasses] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [busyId, setBusyId] = useState(null)

    function load() {
        return Promise.all([listMyMaterials(), listMyClasses(), listStudentLabManualOptions().catch(() => [])])
            .then(([m, c, manuals]) => { setMaterials(m); setClasses(c); setLabManuals(manuals) })
            .catch(async (err) => setError(await messageFrom(err)))
    }

    useEffect(() => {
        load().finally(() => setLoading(false))
    }, [])

    async function handleJoin(id) {
        setError(null)
        setBusyId(id)
        const tabRef = window.open('', '_blank')
        if (tabRef) tabRef.opener = null
        try {
            const link = await joinOnlineClass(id)
            if (tabRef) tabRef.location.href = link.joinUrl
            else setError('Your browser blocked the new tab. Allow pop-ups for this site and try again.')
        } catch (err) {
            if (tabRef) tabRef.close()
            setError(await messageFrom(err))
        } finally { setBusyId(null) }
    }

    async function handleDownload(material) {
        try {
            await downloadMaterial(material)
            await load()
        } catch (err) {
            setError(await messageFrom(err))
        }
    }

    const byCourse = materials.reduce((acc, m) => {
        const key = `${m.courseCode} — ${m.courseTitle}`
        ;(acc[key] = acc[key] || []).push(m)
        return acc
    }, {})

    if (loading) return <p className="theme-text-muted">Loading…</p>

    const liveNow = classes.filter((c) => c.status === 'LIVE')
    const upcoming = classes.filter((c) => c.status === 'SCHEDULED')
    const past = classes.filter((c) => c.status === 'ENDED')

    function ClassRow({ c }) {
        return (
            <li className="px-4 py-3 flex items-center gap-4">
                <div className="flex-1">
                    <p className="font-medium">
                        {c.courseCode} — {c.title}
                        <span className={`ml-2 text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_STYLE[c.status]}`}>{c.status}</span>
                    </p>
                    <p className="text-xs theme-text-muted">
                        {c.lecturerName} · {c.status === 'SCHEDULED' ? `Scheduled: ${formatDate(c.scheduledAt)}` : `Started: ${formatDate(c.startedAt)}`}
                    </p>
                </div>
                {c.status === 'LIVE' && (
                    <button onClick={() => handleJoin(c.id)} disabled={busyId === c.id}
                            className="accent-gradient text-white rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-50">
                        Join Class
                    </button>
                )}
                {c.status === 'SCHEDULED' && <span className="text-xs theme-text-muted">Waiting for lecturer</span>}
            </li>
        )
    }

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Learning Resources</h2>
            <p className="theme-text-muted text-sm mb-6">
                Notes, slides, live classes and laboratory manuals for the courses you have registered this session.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

            <div className="flex mb-6 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                {[['classes', `Online Classes${liveNow.length ? ` (${liveNow.length} live)` : ''}`], ['materials', 'Class Materials'], ['manuals', 'Get Lab Manual']].map(([key, label]) => (
                    <button key={key} onClick={() => setTab(key)}
                            className={`px-5 py-2 text-sm font-medium rounded-lg btn-press ${
                                tab === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'
                            }`}>
                        {label}
                    </button>
                ))}
            </div>

            {tab === 'classes' && (
                <div className="space-y-6">
                    {classes.length === 0 && (
                        <div className="card p-6 theme-text-muted text-sm">
                            No online classes yet. If your lecturers are teaching online, they will appear here
                            once you have registered the course.
                        </div>
                    )}
                    {liveNow.length > 0 && (
                        <div className="card overflow-hidden">
                            <div className="bg-green-600 text-white px-4 py-2 text-sm font-semibold">Live Now</div>
                            <ul className="divide-y theme-border">{liveNow.map((c) => <ClassRow key={c.id} c={c} />)}</ul>
                        </div>
                    )}
                    {upcoming.length > 0 && (
                        <div className="card overflow-hidden">
                            <div className="bg-amber-600 text-white px-4 py-2 text-sm font-semibold">Upcoming</div>
                            <ul className="divide-y theme-border">{upcoming.map((c) => <ClassRow key={c.id} c={c} />)}</ul>
                        </div>
                    )}
                    {past.length > 0 && (
                        <div className="card overflow-hidden">
                            <div className="bg-slate-500 text-white px-4 py-2 text-sm font-semibold">Past Classes</div>
                            <ul className="divide-y theme-border">{past.map((c) => <ClassRow key={c.id} c={c} />)}</ul>
                        </div>
                    )}
                </div>
            )}

            {tab === 'manuals' && (
                <div className="space-y-4">
                    <div className="card p-4">
                        <div className="flex flex-wrap items-center justify-between gap-2">
                            <div>
                                <h3 className="font-semibold">Lab Manual Payment</h3>
                                <p className="text-sm theme-text-muted mt-1">Select your course manual, review its price and see how payment and collection will work.</p>
                            </div>
                            <span className="text-xs font-semibold rounded-full px-3 py-1" style={{ backgroundColor: 'var(--surface-soft)' }}>Payment preview</span>
                        </div>
                        <div className="mt-3 rounded-lg border p-3 text-sm" style={{ borderColor: 'var(--border)', backgroundColor: 'var(--surface-soft)' }}>
                            <b>Online payment is not active yet.</b>
                            <p className="theme-text-muted mt-1">This is a preview of the student payment feature. You can view available manuals and prices, but no payment will be initiated and no money will be deducted.</p>
                        </div>
                    </div>
                    {labManuals.length === 0 ?
                        <div className="card p-6 text-sm theme-text-muted">There are no active lab manuals available at the moment. Please check again later or contact the laboratory office.</div> :
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {labManuals.map(manual => <div key={manual.id} className="card p-4 flex flex-col gap-3">
                                <div className="flex-1">
                                    <p className="text-xs theme-text-muted">{manual.courseCode || 'Department manual'}</p>
                                    <h4 className="font-semibold mt-1">{manual.title}</h4>
                                    {manual.courseTitle && <p className="text-sm theme-text-muted">{manual.courseTitle}</p>}
                                    <p className="text-lg font-bold mt-3">₦{Number(manual.price || 0).toLocaleString('en-NG', { minimumFractionDigits: 2 })}</p>
                                </div>
                                <button onClick={() => setSelectedManual(manual)} className="accent-gradient text-white rounded-lg px-4 py-2 text-sm">View payment details</button>
                            </div>)}
                        </div>
                    }
                    {selectedManual && <div className="card p-5 space-y-4" role="dialog" aria-modal="true" aria-labelledby="student-manual-title">
                        <div className="flex items-start justify-between gap-3">
                            <div>
                                <p className="text-xs theme-text-muted">{selectedManual.courseCode || 'Department manual'}</p>
                                <h3 id="student-manual-title" className="font-bold text-lg">{selectedManual.title}</h3>
                                {selectedManual.courseTitle && <p className="text-sm theme-text-muted">{selectedManual.courseTitle}</p>}
                            </div>
                            <button onClick={() => setSelectedManual(null)} className="text-sm underline" aria-label="Close details">Close</button>
                        </div>
                        <div className="rounded-lg p-4" style={{ backgroundColor: 'var(--surface-soft)' }}>
                            <p className="text-xs theme-text-muted">Manual price</p>
                            <p className="text-2xl font-bold">₦{Number(selectedManual.price || 0).toLocaleString('en-NG', { minimumFractionDigits: 2 })}</p>
                        </div>
                        <h4 className="font-semibold">How the payment process will work</h4>
                        <ol className="list-decimal pl-5 space-y-2 text-sm">
                            <li>Select the correct course manual and confirm the displayed price.</li>
                            <li>When online payment is enabled, select Pay with Paystack.</li>
                            <li>Complete payment on Paystack's secure checkout page.</li>
                            <li>Return to CSDT-DAS to view and print your verified receipt.</li>
                            <li>Present the receipt at the laboratory and collect your manual.</li>
                        </ol>
                        <div className="rounded-lg p-3 text-sm" style={{ backgroundColor: 'var(--surface-soft)' }}>
                            <b>Preview only — payment unavailable</b>
                            <p className="theme-text-muted mt-1">The payment button will become available after the department completes testing. No transaction can be started from this screen.</p>
                        </div>
                        <button disabled className="w-full rounded-lg px-4 py-2 text-sm font-semibold opacity-60" style={{ backgroundColor: 'var(--surface-soft)' }}>Paystack payment coming soon</button>
                    </div>}
                </div>
            )}

            {tab === 'materials' && (
                Object.keys(byCourse).length === 0 ? (
                    <div className="card p-6 theme-text-muted text-sm">
                        No materials yet. Your lecturers' notes will appear here for each course you have registered.
                    </div>
                ) : (
                    <div className="space-y-6">
                        {Object.entries(byCourse).map(([course, items]) => (
                            <div key={course} className="card overflow-hidden">
                                <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">{course}</div>
                                <ul className="divide-y theme-border">
                                    {items.map((m) => (
                                        <li key={m.id} className="px-4 py-3 flex items-center gap-4">
                                            <div className="flex-1 min-w-0">
                                                <p className="font-medium truncate">{m.title}</p>
                                                {m.description && <p className="text-sm theme-text-muted">{m.description}</p>}
                                                <p className="text-xs theme-text-muted">{m.uploadedByName} · {formatDate(m.createdAt)}</p>
                                            </div>
                                            {m.type === 'FILE' ? (
                                                <button onClick={() => handleDownload(m)} className="text-sm font-medium" style={{ color: 'var(--accent)' }}>Download</button>
                                            ) : (
                                                <a href={m.linkUrl} target="_blank" rel="noopener noreferrer" className="text-sm font-medium" style={{ color: 'var(--accent)' }}>Open link</a>
                                            )}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        ))}
                    </div>
                )
            )}
        </div>
    )
}

export default StudentLearning