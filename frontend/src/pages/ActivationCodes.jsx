import { useEffect, useState } from 'react'
import { Download, KeyRound, Printer } from 'lucide-react'
import api from '../services/api'
import { getActivationStatus, issueActivationCodes } from '../services/activationService'

const msg = (err) => err.response?.data?.message || err.message
const field = 'border theme-border rounded-lg px-3 py-2 text-sm'

function ActivationCodes() {
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [programmeId, setProgrammeId] = useState('')
    const [levelId, setLevelId] = useState('')
    const [status, setStatus] = useState(null)
    const [result, setResult] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)

    useEffect(() => {
        api.get('/programmes').then((r) => setProgrammes(r.data)).catch((e) => setError(msg(e)))
        api.get('/levels').then((r) => setLevels(r.data)).catch((e) => setError(msg(e)))
    }, [])

    useEffect(() => {
        setStatus(null); setResult(null)
        if (!programmeId || !levelId) return
        getActivationStatus(programmeId, levelId).then(setStatus).catch((e) => setError(msg(e)))
    }, [programmeId, levelId])

    async function issue(regenerate) {
        const question = regenerate
            ? 'Replace every unused code in this group with a new one? Codes already handed out stop working.'
            : 'Generate codes for the students in this group who have none yet?'
        if (!window.confirm(question)) return
        setBusy(true); setError(null)
        try {
            setResult(await issueActivationCodes(Number(programmeId), Number(levelId), regenerate))
            setStatus(await getActivationStatus(programmeId, levelId))
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    function download() {
        const rows = [['Matric number', 'Full name', 'Activation code'], ...result.codes.map((c) => [c.matricNumber, c.fullName, c.code])]
        const csv = rows.map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n')
        const a = document.createElement('a')
        a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
        a.download = 'activation-codes.csv'
        a.click()
    }

    const programme = programmes.find((p) => String(p.id) === programmeId)
    const level = levels.find((l) => String(l.id) === levelId)

    return (
        <div className="max-w-4xl">
            <style>{'@media print { aside, header, nav, .no-print { display: none !important } }'}</style>

            <div className="no-print">
                <h2 className="text-2xl font-bold mb-2 flex items-center gap-2"><KeyRound size={22} /> Activation Codes</h2>
                <p className="theme-text-muted text-sm mb-6">
                    A student needs one of these codes, plus the code emailed to them, to create an account. It stops anyone from signing up with a friend's matric number.
                    Hand each code to its student privately, on a slip or in a direct message, never in a group chat.
                </p>

                {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

                <div className="card p-4 mb-6 flex gap-3 flex-wrap items-end">
                    <div>
                        <label className="block text-sm font-medium mb-1">Programme</label>
                        <select value={programmeId} onChange={(e) => setProgrammeId(e.target.value)} className={field}>
                            <option value="">Choose…</option>
                            {programmes.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium mb-1">Level</label>
                        <select value={levelId} onChange={(e) => setLevelId(e.target.value)} className={field}>
                            <option value="">Choose…</option>
                            {levels.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
                        </select>
                    </div>
                </div>

                {status && (
                    <>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
                            {[['Students', status.students], ['Already have an account', status.withAccount], ['Have an unused code', status.withUnusedCode], ['Need a code', status.withoutCode]].map(([label, n]) => (
                                <div key={label} className="card p-4"><p className="text-2xl font-bold">{n}</p><p className="text-xs theme-text-muted">{label}</p></div>
                            ))}
                        </div>
                        <div className="flex gap-3 flex-wrap mb-6">
                            <button onClick={() => issue(false)} disabled={busy || status.withoutCode === 0}
                                    className="accent-gradient text-white rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                {busy ? 'Working…' : `Generate codes for ${status.withoutCode} student(s)`}
                            </button>
                            <button onClick={() => issue(true)} disabled={busy || status.withUnusedCode === 0}
                                    className="border theme-border rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-50">
                                Replace the unused codes
                            </button>
                        </div>
                    </>
                )}
            </div>

            {result && (
                <div className="card overflow-hidden">
                    <div className="theme-sidebar text-white px-4 py-2 flex justify-between items-center gap-3 flex-wrap text-sm">
                        <span className="font-semibold">{programme?.name} · {level?.name} · {result.issued} new code(s)</span>
                        <span className="no-print flex gap-2">
                            <button onClick={() => window.print()} className="flex items-center gap-1 bg-white/20 rounded-lg px-3 py-1"><Printer size={13} /> Print</button>
                            <button onClick={download} className="flex items-center gap-1 bg-white/20 rounded-lg px-3 py-1"><Download size={13} /> Download</button>
                        </span>
                    </div>
                    <p className="no-print px-4 py-2 text-xs bg-amber-50 text-amber-800">
                        These codes are shown only now. Print or download them before you leave this page. If you lose them, use "Replace the unused codes".
                    </p>
                    {result.issued === 0 ? <p className="p-6 text-sm theme-text-muted">No new codes were needed.</p> : (
                        <table className="w-full text-sm text-left">
                            <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                            <tr><th className="px-4 py-2">Matric number</th><th className="px-4 py-2">Name</th><th className="px-4 py-2">Activation code</th></tr>
                            </thead>
                            <tbody className="divide-y theme-border">
                            {result.codes.map((c) => (
                                <tr key={c.matricNumber}><td className="px-4 py-2">{c.matricNumber}</td><td className="px-4 py-2">{c.fullName}</td><td className="px-4 py-2 font-mono font-bold tracking-wider">{c.code}</td></tr>
                            ))}
                            </tbody>
                        </table>
                    )}
                </div>
            )}
        </div>
    )
}

export default ActivationCodes