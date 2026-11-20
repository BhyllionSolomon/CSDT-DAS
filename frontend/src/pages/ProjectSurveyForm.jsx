import { useEffect, useState } from 'react'
import { ShieldCheck } from 'lucide-react'
import { getSurveyState, submitSurvey, errorOf } from '../services/projectSuiteService'

const field = 'border theme-border rounded-lg px-3 py-2 text-sm w-full'
const BLANK = {
    moneyCollected: '', moneyDetails: '', inappropriate: '', inappropriateDetails: '', ghostwritten: '', ghostwrittenDetails: '',
    professional: 0, helpful: 0, available: 0, overall: 0, impact: '', problemSolved: '', acknowledged: '', acknowledgedDetails: '', comment: '',
}

function YesNo({ label, value, onChange, details, onDetails, ask }) {
    return (
        <div className="py-3">
            <p className="text-sm font-medium mb-2">{label}</p>
            <div className="flex gap-2">
                {['YES', 'NO'].map((v) => (
                    <button key={v} type="button" onClick={() => onChange(v)}
                            className={`px-5 py-1.5 rounded-lg text-sm font-semibold border theme-border btn-press ${value === v ? (v === 'YES' ? 'bg-red-600 text-white' : 'accent-gradient text-white') : ''}`}>
                        {v === 'YES' ? 'Yes' : 'No'}
                    </button>
                ))}
            </div>
            {value === 'YES' && <textarea value={details} onChange={(e) => onDetails(e.target.value)} rows={3} maxLength={500} placeholder={ask} className={`${field} mt-2`} />}
        </div>
    )
}

function Stars({ label, value, onChange }) {
    return (
        <div className="py-2">
            <p className="text-sm font-medium mb-1">{label}</p>
            <div className="flex gap-2 items-center">
                {[1, 2, 3, 4, 5].map((n) => (
                    <button key={n} type="button" onClick={() => onChange(n)} className={`w-10 h-10 rounded-lg text-sm font-semibold border theme-border btn-press ${value === n ? 'accent-gradient text-white' : ''}`}>{n}</button>
                ))}
                <span className="text-xs theme-text-muted ml-1">1 = poor, 5 = excellent</span>
            </div>
        </div>
    )
}

function ProjectSurveyForm() {
    const [state, setState] = useState(null)
    const [f, setF] = useState(BLANK)
    const [busy, setBusy] = useState(false)
    const [done, setDone] = useState(false)
    const [error, setError] = useState(null)

    useEffect(() => { getSurveyState().then(setState).catch((e) => setError(errorOf(e))) }, [])

    const set = (k) => (v) => setF({ ...f, [k]: v })

    async function submit(e) {
        e.preventDefault()
        if (!window.confirm('Submit your answers anonymously? You cannot change them afterwards.')) return
        setBusy(true); setError(null)
        try { await submitSurvey(f); setDone(true) } catch (err) { setError(errorOf(err)) } finally { setBusy(false) }
    }

    if (error && !state) return <div className="px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm max-w-xl">{error}</div>
    if (!state) return <p className="theme-text-muted">Loading…</p>

    return (
        <div className="max-w-2xl">
            <h2 className="text-2xl font-bold mb-2">Project Integrity Form</h2>

            <div className="mb-6 px-4 py-4 rounded-lg bg-green-50 text-green-900 text-sm">
                <p className="flex items-center gap-2 font-semibold mb-1"><ShieldCheck size={16} /> This form is anonymous</p>
                <ul className="space-y-1 list-disc pl-5">
                    <li>Your name, matric number and the time you submit are <b>not stored</b> with your answers.</li>
                    <li>Your supervisor never sees your answers. Only the H.O.D. does.</li>
                    <li>Ratings are shown only when at least 5 students have answered.</li>
                    <li>Serious concerns go to the H.O.D. so they can be acted on. If very few students supervised by one person answer, the H.O.D. may be able to guess who wrote something, so only write what you are comfortable with.</li>
                </ul>
            </div>

            {done && <div className="card p-6 text-sm text-green-700">Thank you. Your answers were saved anonymously.</div>}
            {!done && !state.assigned && <div className="card p-6 text-sm theme-text-muted">You have not been assigned a project supervisor yet.</div>}
            {!done && state.assigned && state.submitted && <div className="card p-6 text-sm text-green-700">You have already submitted this form. Thank you.</div>}
            {!done && state.assigned && !state.submitted && !state.open && <div className="card p-6 text-sm theme-text-muted">This form is not open yet. The H.O.D. opens it near the end of the project year.</div>}

            {!done && state.assigned && !state.submitted && state.open && (
                <form onSubmit={submit} className="card p-5 divide-y theme-border">
                    {error && <div className="mb-3 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}

                    <YesNo label="Did any member of staff collect money from you in connection with your project?" value={f.moneyCollected} onChange={set('moneyCollected')} details={f.moneyDetails} onDetails={set('moneyDetails')} ask="Who collected money, how much, and what for?" />
                    <YesNo label="Were there any inappropriate or unprofessional moments with any member of staff?" value={f.inappropriate} onChange={set('inappropriate')} details={f.inappropriateDetails} onDetails={set('inappropriateDetails')} ask="What happened?" />
                    <YesNo label="Did anyone write your report for you, or pressure you to submit work that was not your own?" value={f.ghostwritten} onChange={set('ghostwritten')} details={f.ghostwrittenDetails} onDetails={set('ghostwrittenDetails')} ask="What happened?" />

                    <div className="py-3">
                        <p className="font-semibold mb-1">Your supervisor: {state.supervisor}</p>
                        <Stars label="Was your supervisor professional?" value={f.professional} onChange={set('professional')} />
                        <Stars label="Was your supervisor helpful?" value={f.helpful} onChange={set('helpful')} />
                        <Stars label="Was your supervisor available when you needed them?" value={f.available} onChange={set('available')} />
                        <Stars label="Overall, how do you rate your supervisor?" value={f.overall} onChange={set('overall')} />
                    </div>

                    <div className="py-3 space-y-3">
                        <div><p className="text-sm font-medium mb-1">What problem did your project solve?</p><textarea value={f.problemSolved} onChange={(e) => set('problemSolved')(e.target.value)} rows={3} maxLength={1000} className={field} /></div>
                        <div><p className="text-sm font-medium mb-1">What impact did the project have on you?</p><textarea value={f.impact} onChange={(e) => set('impact')(e.target.value)} rows={3} maxLength={1000} className={field} /></div>
                    </div>

                    <div className="py-3">
                        <p className="text-sm font-medium mb-2">Were all contributors and correspondents properly acknowledged?</p>
                        <div className="flex gap-2">
                            {[['YES', 'Yes'], ['NO', 'No'], ['N/A', 'Not applicable']].map(([v, label]) => (
                                <button key={v} type="button" onClick={() => set('acknowledged')(v)} className={`px-4 py-1.5 rounded-lg text-sm font-semibold border theme-border btn-press ${f.acknowledged === v ? 'accent-gradient text-white' : ''}`}>{label}</button>
                            ))}
                        </div>
                        <textarea value={f.acknowledgedDetails} onChange={(e) => set('acknowledgedDetails')(e.target.value)} rows={2} maxLength={500} placeholder="Anything to add (optional)" className={`${field} mt-2`} />
                    </div>

                    <div className="py-3"><p className="text-sm font-medium mb-1">Anything else the department should know? (optional)</p><textarea value={f.comment} onChange={(e) => set('comment')(e.target.value)} rows={3} maxLength={1000} className={field} /></div>

                    <div className="pt-4">
                        <button type="submit" disabled={busy} className="accent-gradient text-white rounded-lg px-6 py-2.5 text-sm font-semibold btn-press disabled:opacity-50">{busy ? 'Submitting…' : 'Submit anonymously'}</button>
                    </div>
                </form>
            )}
        </div>
    )
}

export default ProjectSurveyForm