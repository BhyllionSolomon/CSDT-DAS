import { useEffect, useState } from 'react'
import { AlertCircle, Pin, Trash2 } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { getNotices, readNotice, getNoticeTargets, postNotice, deleteNotice } from '../services/noticeService'

const msg = (err) => err.response?.data?.message || err.message
const POSTERS = ['HOD', 'ADMIN', 'LECTURER', 'ADJUNCT', 'LEVEL_ADVISER']
const AUDIENCE = {
    ALL: 'Everyone', STAFF: 'All staff', STUDENTS: 'All students',
    PROGRAMME_LEVEL: 'A programme and level', COURSE: 'The students of a course',
}
const LEVELS = [100, 200, 300, 400]
const BLANK = { title: '', body: '', audience: '', programmeId: '', levelNumber: '', courseId: '', priority: 'NORMAL', pinned: false, expiresOn: '' }

const when = (v) => new Date(v).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })

function NoticeBoard() {
    const { user } = useAuth()
    const canPost = POSTERS.includes(user.role)

    const [board, setBoard] = useState(null)
    const [targets, setTargets] = useState(null)
    const [filter, setFilter] = useState('ALL')
    const [open, setOpen] = useState(null)
    const [showForm, setShowForm] = useState(false)
    const [form, setForm] = useState(BLANK)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    async function load() { setBoard(await getNotices()) }

    useEffect(() => {
        load().catch((e) => setError(msg(e)))
        if (canPost) getNoticeTargets().then(setTargets).catch(() => setTargets(null))
    }, [])

    async function toggle(n) {
        const opening = open !== n.id
        setOpen(opening ? n.id : null)
        if (opening && !n.read) {
            try {
                await readNotice(n.id)
                setBoard((b) => ({ unread: Math.max(0, b.unread - 1), notices: b.notices.map((x) => (x.id === n.id ? { ...x, read: true } : x)) }))
            } catch { /* the notice is still readable */ }
        }
    }

    function chooseAudience(audience) {
        setForm({
            ...form, audience,
            programmeId: targets?.fixedProgrammeId ? String(targets.fixedProgrammeId) : '',
            levelNumber: targets?.fixedLevel ? String(targets.fixedLevel) : '',
            courseId: '',
        })
    }

    async function submit(e) {
        e.preventDefault()
        setBusy(true); setError(null); setNotice(null)
        try {
            await postNotice({
                title: form.title, body: form.body, audience: form.audience,
                programmeId: form.programmeId ? Number(form.programmeId) : null,
                levelNumber: form.levelNumber ? Number(form.levelNumber) : null,
                courseId: form.courseId ? Number(form.courseId) : null,
                priority: form.priority, pinned: form.pinned, expiresOn: form.expiresOn || null,
            })
            setNotice('Notice posted.')
            setForm(BLANK); setShowForm(false)
            await load()
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    async function remove(n) {
        if (!window.confirm(`Delete "${n.title}"? It disappears for everyone.`)) return
        try { await deleteNotice(n.id); await load() } catch (err) { setError(msg(err)) }
    }

    const shown = (board?.notices || []).filter((n) => filter === 'ALL' || (filter === 'UNREAD' && !n.read) || (filter === 'PINNED' && n.pinned))
    const fixed = !!targets?.fixedProgrammeId && form.audience === 'PROGRAMME_LEVEL'

    return (
        <div className="max-w-3xl">
            <div className="flex justify-between items-start gap-3 flex-wrap mb-2">
                <h2 className="text-2xl font-bold">Notice Board{board?.unread > 0 && <span className="ml-2 text-xs align-middle px-2 py-0.5 rounded-full accent-gradient text-white">{board.unread} new</span>}</h2>
                {targets && (
                    <button onClick={() => setShowForm(!showForm)} className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press">
                        {showForm ? 'Close' : 'New notice'}
                    </button>
                )}
            </div>
            <p className="theme-text-muted text-sm mb-6">Notices from the department, your level adviser and your lecturers.</p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            {showForm && targets && (
                <form onSubmit={submit} className="card p-4 mb-6 space-y-3">
                    <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required maxLength={150} placeholder="Title"
                           className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />
                    <textarea value={form.body} onChange={(e) => setForm({ ...form, body: e.target.value })} required rows={5} maxLength={4000} placeholder="Write the notice…"
                              className="w-full border theme-border rounded-lg px-3 py-2 text-sm" />

                    <div className="flex gap-3 flex-wrap">
                        <select value={form.audience} onChange={(e) => chooseAudience(e.target.value)} required className="border theme-border rounded-lg px-3 py-2 text-sm">
                            <option value="">Who is it for?</option>
                            {targets.audiences.map((a) => <option key={a} value={a}>{AUDIENCE[a]}</option>)}
                        </select>

                        {form.audience === 'PROGRAMME_LEVEL' && (
                            <>
                                <select value={form.programmeId} disabled={fixed} onChange={(e) => setForm({ ...form, programmeId: e.target.value })} required className="border theme-border rounded-lg px-3 py-2 text-sm">
                                    <option value="">Programme…</option>
                                    {targets.programmes.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                                </select>
                                <select value={form.levelNumber} disabled={fixed && !!targets.fixedLevel} onChange={(e) => setForm({ ...form, levelNumber: e.target.value })} className="border theme-border rounded-lg px-3 py-2 text-sm">
                                    <option value="">All levels</option>
                                    {LEVELS.map((l) => <option key={l} value={l}>{l} Level</option>)}
                                </select>
                            </>
                        )}

                        {form.audience === 'COURSE' && (
                            <select value={form.courseId} onChange={(e) => setForm({ ...form, courseId: e.target.value })} required className="border theme-border rounded-lg px-3 py-2 text-sm">
                                <option value="">Course…</option>
                                {targets.courses.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                            </select>
                        )}
                    </div>

                    <div className="flex gap-4 flex-wrap items-center">
                        <label className="text-sm flex items-center gap-2"><input type="checkbox" checked={form.priority === 'URGENT'} onChange={(e) => setForm({ ...form, priority: e.target.checked ? 'URGENT' : 'NORMAL' })} /> Urgent</label>
                        {targets.canPin && <label className="text-sm flex items-center gap-2"><input type="checkbox" checked={form.pinned} onChange={(e) => setForm({ ...form, pinned: e.target.checked })} /> Pin to the top</label>}
                        <label className="text-sm flex items-center gap-2">Show until
                            <input type="date" value={form.expiresOn} onChange={(e) => setForm({ ...form, expiresOn: e.target.value })} className="border theme-border rounded-lg px-2 py-1 text-sm" />
                        </label>
                    </div>

                    <button type="submit" disabled={busy} className="accent-gradient text-white rounded-lg px-6 py-2 text-sm font-medium btn-press disabled:opacity-50">{busy ? 'Posting…' : 'Post notice'}</button>
                </form>
            )}

            <div className="flex gap-1 mb-4 p-1 rounded-xl w-fit" style={{ backgroundColor: 'var(--surface-soft)' }}>
                {[['ALL', 'All'], ['UNREAD', 'Unread'], ['PINNED', 'Pinned']].map(([key, label]) => (
                    <button key={key} onClick={() => setFilter(key)} className={`px-4 py-1.5 text-sm font-medium rounded-lg btn-press ${filter === key ? 'accent-gradient text-white shadow-sm' : 'theme-text-muted'}`}>{label}</button>
                ))}
            </div>

            {!board && !error && <p className="theme-text-muted">Loading…</p>}
            {board && shown.length === 0 && <div className="card p-6 text-sm theme-text-muted">{board.notices.length === 0 ? 'There are no notices for you right now.' : 'Nothing matches this filter.'}</div>}

            <div className="space-y-3">
                {shown.map((n) => (
                    <div key={n.id} className={`card overflow-hidden ${n.expired ? 'opacity-60' : ''}`} style={n.priority === 'URGENT' ? { borderColor: '#fca5a5' } : undefined}>
                        <button onClick={() => toggle(n)} className="w-full text-left p-4 flex items-start gap-3">
                            {!n.read && <span className="w-2.5 h-2.5 rounded-full mt-1.5 shrink-0 accent-gradient" aria-label="Unread" />}
                            <span className="flex-1 min-w-0">
                <span className="flex items-center gap-2 flex-wrap">
                  {n.priority === 'URGENT' && <span className="flex items-center gap-1 text-[11px] font-bold uppercase text-red-600"><AlertCircle size={13} /> Urgent</span>}
                    {n.pinned && <Pin size={13} className="theme-text-muted" />}
                    <span className={`text-sm ${n.read ? 'font-medium' : 'font-bold'}`}>{n.title}</span>
                </span>
                <span className="block text-xs theme-text-muted mt-0.5">
                  {n.audienceLabel} · {n.postedBy} · {when(n.postedAt)}{n.expired ? ' · expired' : n.expiresOn ? ` · until ${when(n.expiresOn)}` : ''}
                </span>
                                {open !== n.id && <span className="block text-sm theme-text-muted mt-1 truncate">{n.body}</span>}
              </span>
                        </button>

                        {open === n.id && (
                            <div className="px-4 pb-4">
                                <p className="text-sm whitespace-pre-wrap">{n.body}</p>
                                {(n.mine || user.role === 'HOD' || user.role === 'ADMIN') && (
                                    <button onClick={() => remove(n)} className="mt-3 flex items-center gap-1 text-xs text-red-600 hover:underline"><Trash2 size={13} /> Delete this notice</button>
                                )}
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </div>
    )
}

export default NoticeBoard