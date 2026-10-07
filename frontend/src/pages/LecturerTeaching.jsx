import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'
import {
    listCourseMaterials, addLinkMaterial, addFileMaterial, deleteMaterial, downloadMaterial, messageFrom,
} from '../services/materialService'
import {
    createOnlineClass, listCourseClasses, startOnlineClass, endOnlineClass,
    joinOnlineClass, getClassParticipants,
} from '../services/onlineClassService'

const STATUS_STYLE = {
    SCHEDULED: 'bg-amber-100 text-amber-700',
    LIVE: 'bg-green-100 text-green-700',
    ENDED: 'bg-slate-200 text-slate-600',
}

function formatDate(value) {
    return value ? new Date(value).toLocaleString() : '—'
}

function formatSize(bytes) {
    if (!bytes) return ''
    return bytes > 1048576 ? `${(bytes / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`
}

// Open the tab immediately (so popup blockers allow it), then point it at the room.
function openBlankTab() {
    const tab = window.open('', '_blank')
    if (tab) tab.opener = null
    return tab
}

function LecturerTeaching() {
    const { user } = useAuth()
    const isHod = user.role === 'HOD' || user.role === 'ADMIN'

    const [allocations, setAllocations] = useState([])
    const [selected, setSelected] = useState(null)
    const [tab, setTab] = useState('materials')
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null)
    const [message, setMessage] = useState(null)

    const [materials, setMaterials] = useState([])
    const [mode, setMode] = useState('FILE')
    const [title, setTitle] = useState('')
    const [description, setDescription] = useState('')
    const [linkUrl, setLinkUrl] = useState('')
    const [file, setFile] = useState(null)
    const [fileKey, setFileKey] = useState(0)
    const [saving, setSaving] = useState(false)

    const [classes, setClasses] = useState([])
    const [classTitle, setClassTitle] = useState('')
    const [scheduledAt, setScheduledAt] = useState('')
    const [busyClassId, setBusyClassId] = useState(null)
    const [participants, setParticipants] = useState({})

    useEffect(() => {
        const request = isHod ? api.get('/allocations') : api.get(`/allocations/lecturer/${user.id}`)
        request
            .then((res) => setAllocations(res.data))
            .catch((e) => setError(e.response?.data?.message || e.message))
            .finally(() => setLoading(false))
    }, [user.id, isHod])

    async function loadAll(a) {
        const [mats, cls] = await Promise.all([
            listCourseMaterials(a.courseId, a.academicSessionId, a.semester),
            listCourseClasses(a.courseId, a.academicSessionId, a.semester),
        ])
        setMaterials(mats)
        setClasses(cls)
    }

    async function handleSelect(a) {
        setError(null); setMessage(null); setParticipants({})
        setSelected(a)
        try {
            await loadAll(a)
        } catch (err) {
            setError(await messageFrom(err))
        }
    }

    async function handleAddMaterial(e) {
        e.preventDefault()
        setError(null); setMessage(null)
        setSaving(true)
        try {
            const base = {
                courseId: selected.courseId,
                academicSessionId: selected.academicSessionId,
                semester: selected.semester,
                title,
                description,
            }
            if (mode === 'FILE') {
                if (!file) { setError('Please choose a file to attach.'); setSaving(false); return }
                await addFileMaterial({ ...base, file })
            } else {
                await addLinkMaterial({ ...base, linkUrl })
            }
            setTitle(''); setDescription(''); setLinkUrl(''); setFile(null); setFileKey((k) => k + 1)
            setMessage('Material attached. Registered students can now see it.')
            await loadAll(selected)
        } catch (err) {
            setError(await messageFrom(err))
        } finally { setSaving(false) }
    }

    async function handleDeleteMaterial(id) {
        if (!window.confirm('Remove this material from the course?')) return
        try {
            await deleteMaterial(id)
            await loadAll(selected)
        } catch (err) {
            setError(await messageFrom(err))
        }
    }

    async function handleDownload(material) {
        try {
            await downloadMaterial(material)
        } catch (err) {
            setError(await messageFrom(err))
        }
    }

    async function handleCreateClass(e) {
        e.preventDefault()
        setError(null); setMessage(null)
        setSaving(true)
        try {
            await createOnlineClass({
                courseId: selected.courseId,
                academicSessionId: selected.academicSessionId,
                semester: selected.semester,
                title: classTitle,
                scheduledAt: scheduledAt || null,
            })
            setClassTitle(''); setScheduledAt('')
            setMessage('Online class created. Click "Start Class" when you are ready to teach.')
            await loadAll(selected)
        } catch (err) {
            setError(await messageFrom(err))
        } finally { setSaving(false) }
    }

    async function handleOpenRoom(action, id) {
        setError(null); setMessage(null)
        setBusyClassId(id)
        const tabRef = openBlankTab()
        try {
            const link = await action(id)
            if (tabRef) {
                tabRef.location.href = link.joinUrl
            } else {
                setError('Your browser blocked the new tab. Allow pop-ups for this site and try again.')
            }
            await loadAll(selected)
        } catch (err) {
            if (tabRef) tabRef.close()
            setError(await messageFrom(err))
        } finally { setBusyClassId(null) }
    }

    async function handleEnd(id) {
        if (!window.confirm('End this class? Students will no longer be able to join.')) return
        try {
            await endOnlineClass(id)
            await loadAll(selected)
        } catch (err) {
            setError(await messageFrom(err))
        }
    }

    async function toggleParticipants(id) {
        if (participants[id]) {
            setParticipants((p) => { const copy = { ...p }; delete copy[id]; return copy })
            return
        }
        try {
            const list = await getClassParticipants(id)
            setParticipants((p) => ({ ...p, [id]: list }))
        } catch (err) {
            setError(await messageFrom(err))
        }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    if (!selected) {
        return (
            <div>
                <h2 className="text-2xl font-bold mb-2">Teaching</h2>
                <p className="theme-text-muted text-sm mb-6">
                    Choose a course to attach class notes or run an online class.
                </p>
                {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
                {allocations.length === 0 ? (
                    <div className="card p-6 theme-text-muted">
                        You have no assigned courses yet. Claim a course first, or ask the H.O.D to allocate one to you.
                    </div>
                ) : (
                    <div className="grid grid-cols-3 gap-4">
                        {allocations.map((a) => (
                            <button key={a.id} onClick={() => handleSelect(a)} className="card card-hover p-4 text-left">
                                <p className="font-semibold">{a.courseCode}</p>
                                <p className="text-sm theme-text-muted">{a.courseTitle}</p>
                                <p className="text-xs theme-text-muted mt-1">{a.academicSessionName} · {a.semester}</p>
                                {isHod && a.lecturerName && <p className="text-xs mt-1" style={{ color: 'var(--accent)' }}>{a.lecturerName}</p>}
                            </button>
                        ))}
                    </div>
                )}
            </div>
        )
    }

    return (
        <div>
            <button onClick={() => setSelected(null)} className="text-sm font-medium mb-3" style={{ color: 'var(--accent)' }}>
                ← Choose a different course
            </button>
            <h2 className="text-2xl font-bold">{selected.courseCode} — {selected.courseTitle}</h2>
            <p className="theme-text-muted text-sm mb-6">{selected.academicSessionName} · {selected.semester} semester</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {message && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{message}</div>}

            <div className="flex mb-6 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                {[['materials', 'Class Materials'], ['classes', 'Online Classes']].map(([key, label]) => (
                    <button key={key} onClick={() => setTab(key)}
                            className={`px-5 py-2 text-sm font-medium rounded-lg btn-press ${
                                tab === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'
                            }`}>
                        {label}
                    </button>
                ))}
            </div>

            {tab === 'materials' && (
                <div className="space-y-6">
                    <form onSubmit={handleAddMaterial} className="card p-5 space-y-3">
                        <div className="flex gap-2">
                            {[['FILE', 'Upload a file'], ['LINK', 'Share a link']].map(([key, label]) => (
                                <button type="button" key={key} onClick={() => setMode(key)}
                                        className={`px-3 py-1.5 text-sm rounded-lg border theme-border btn-press ${
                                            mode === key ? 'accent-gradient text-white border-transparent' : ''
                                        }`}>
                                    {label}
                                </button>
                            ))}
                        </div>
                        <input value={title} onChange={(e) => setTitle(e.target.value)} required placeholder="Title (e.g. Week 3 — Normalisation)"
                               className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                        <input value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Short description (optional)"
                               className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                        {mode === 'FILE' ? (
                            <div>
                                <input key={fileKey} type="file" accept=".pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.png,.jpg,.jpeg,.zip"
                                       onChange={(e) => setFile(e.target.files[0])} className="text-sm" />
                                <p className="text-xs theme-text-muted mt-1">PDF, Word, PowerPoint, Excel, text, images or zip.</p>
                            </div>
                        ) : (
                            <input value={linkUrl} onChange={(e) => setLinkUrl(e.target.value)} required
                                   placeholder="https:// … (Google Classroom, Drive, YouTube, etc.)"
                                   className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                        )}
                        <button type="submit" disabled={saving}
                                className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            {saving ? 'Attaching…' : 'Attach to Course'}
                        </button>
                    </form>

                    <div className="card overflow-hidden">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">
                            Attached Materials ({materials.length})
                        </div>
                        {materials.length === 0 ? (
                            <p className="p-6 text-sm theme-text-muted">Nothing attached yet.</p>
                        ) : (
                            <ul className="divide-y theme-border">
                                {materials.map((m) => (
                                    <li key={m.id} className="px-4 py-3 flex items-center gap-4">
                                        <div className="flex-1 min-w-0">
                                            <p className="font-medium truncate">{m.title}</p>
                                            <p className="text-xs theme-text-muted">
                                                {m.type === 'FILE' ? `${m.originalFilename} ${formatSize(m.sizeBytes)}` : m.linkUrl}
                                                {' · '}{formatDate(m.createdAt)} · {m.uploadedByName}
                                                {m.type === 'FILE' && ` · ${m.downloadCount} student download(s)`}
                                            </p>
                                        </div>
                                        {m.type === 'FILE' ? (
                                            <button onClick={() => handleDownload(m)} className="text-sm font-medium" style={{ color: 'var(--accent)' }}>Download</button>
                                        ) : (
                                            <a href={m.linkUrl} target="_blank" rel="noopener noreferrer" className="text-sm font-medium" style={{ color: 'var(--accent)' }}>Open</a>
                                        )}
                                        <button onClick={() => handleDeleteMaterial(m.id)} className="text-sm text-red-600 hover:underline">Remove</button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>
            )}

            {tab === 'classes' && (
                <div className="space-y-6">
                    <form onSubmit={handleCreateClass} className="card p-5 flex gap-3 items-end flex-wrap">
                        <div className="flex-1 min-w-[220px]">
                            <label className="block text-sm font-medium mb-1">Class title</label>
                            <input value={classTitle} onChange={(e) => setClassTitle(e.target.value)} required placeholder="e.g. Week 4 live lecture"
                                   className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Scheduled for (optional)</label>
                            <input type="datetime-local" value={scheduledAt} onChange={(e) => setScheduledAt(e.target.value)}
                                   className="border theme-border rounded-lg px-3 py-2 text-sm" />
                        </div>
                        <button type="submit" disabled={saving}
                                className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            {saving ? 'Creating…' : 'Create Online Class'}
                        </button>
                    </form>

                    <div className="card overflow-hidden">
                        <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">
                            Online Classes ({classes.length})
                        </div>
                        {classes.length === 0 ? (
                            <p className="p-6 text-sm theme-text-muted">No online classes yet.</p>
                        ) : (
                            <ul className="divide-y theme-border">
                                {classes.map((c) => (
                                    <li key={c.id} className="px-4 py-3">
                                        <div className="flex items-center gap-4 flex-wrap">
                                            <div className="flex-1 min-w-[200px]">
                                                <p className="font-medium">
                                                    {c.title}
                                                    <span className={`ml-2 text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_STYLE[c.status]}`}>{c.status}</span>
                                                </p>
                                                <p className="text-xs theme-text-muted">
                                                    Scheduled: {formatDate(c.scheduledAt)} · Started: {formatDate(c.startedAt)}
                                                    {c.endedAt && ` · Ended: ${formatDate(c.endedAt)}`} · {c.participantCount} student(s) joined
                                                </p>
                                            </div>

                                            {c.status === 'SCHEDULED' && (
                                                <button onClick={() => handleOpenRoom(startOnlineClass, c.id)} disabled={busyClassId === c.id}
                                                        className="accent-gradient text-white rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-50">
                                                    Start Class
                                                </button>
                                            )}
                                            {c.status === 'LIVE' && (
                                                <>
                                                    <button onClick={() => handleOpenRoom(joinOnlineClass, c.id)} disabled={busyClassId === c.id}
                                                            className="accent-gradient text-white rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-50">
                                                        Re-open Room
                                                    </button>
                                                    <button onClick={() => handleEnd(c.id)} className="text-sm text-red-600 hover:underline">End Class</button>
                                                </>
                                            )}
                                            <button onClick={() => toggleParticipants(c.id)} className="text-sm font-medium" style={{ color: 'var(--accent)' }}>
                                                {participants[c.id] ? 'Hide' : 'View'} who joined
                                            </button>
                                        </div>

                                        {participants[c.id] && (
                                            <div className="mt-3 rounded-lg p-3 text-sm" style={{ backgroundColor: 'var(--surface-soft)' }}>
                                                {participants[c.id].length === 0 ? (
                                                    <p className="theme-text-muted">No student has joined through CSDT yet.</p>
                                                ) : (
                                                    <ul className="space-y-1">
                                                        {participants[c.id].map((p, i) => (
                                                            <li key={i} className="flex justify-between">
                                                                <span>{p.matricNumber} — {p.fullName}</span>
                                                                <span className="theme-text-muted">{formatDate(p.joinedAt)}</span>
                                                            </li>
                                                        ))}
                                                    </ul>
                                                )}
                                            </div>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>
            )}
        </div>
    )
}

export default LecturerTeaching