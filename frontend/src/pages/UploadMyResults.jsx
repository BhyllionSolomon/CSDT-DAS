import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { extractSelfResults, saveSelfResults, getSelfResultAnalytics } from '../services/selfResultService'

function UploadMyResults() {
    const { user } = useAuth()
    const [file, setFile] = useState(null)
    const [rows, setRows] = useState(null)
    const [extracting, setExtracting] = useState(false)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState(null)
    const [saveResult, setSaveResult] = useState(null)

    const [analytics, setAnalytics] = useState(null)
    const [loadingAnalytics, setLoadingAnalytics] = useState(true)

    function loadAnalytics() {
        setLoadingAnalytics(true)
        getSelfResultAnalytics(user.studentId).then(setAnalytics).finally(() => setLoadingAnalytics(false))
    }
    useEffect(loadAnalytics, [user.studentId])

    async function handleExtract() {
        if (!file) { setError('Choose a file first.'); return }
        setError(null); setSaveResult(null); setExtracting(true)
        try {
            const data = await extractSelfResults(file)
            setRows(data)
            if (data.length === 0) setError('No course codes were found in this file.')
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setExtracting(false) }
    }

    async function handleSave() {
        setSaving(true); setError(null)
        try {
            const count = await saveSelfResults(user.studentId, rows)
            setSaveResult(count)
            setRows(null); setFile(null)
            loadAnalytics()
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setSaving(false) }
    }

    return (
        <div>
            <h2 className="text-2xl font-bold mb-2">Upload &amp; Analyse Results</h2>
            <p className="theme-text-muted text-sm mb-6">
                Download your official results from the school-wide portal, then upload the file here
                (CSV or Excel) to get personalised analytics — average score, strong/weak courses, and advice.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {saveResult !== null && (
                <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">
                    {saveResult} result(s) saved successfully.
                </div>
            )}

            <div className="card p-4 mb-6 flex gap-4 items-end flex-wrap">
                <div>
                    <label className="block text-sm font-medium mb-1">File (CSV or Excel)</label>
                    <input type="file" accept=".csv,.xlsx,.xls"
                           onChange={(e) => { setFile(e.target.files[0]); setRows(null) }}
                           className="text-sm" />
                </div>
                <button onClick={handleExtract} disabled={!file || extracting}
                        className="accent-gradient text-white rounded-lg px-4 py-2 text-sm font-medium btn-press disabled:opacity-50">
                    {extracting ? 'Extracting…' : 'Extract Results'}
                </button>
            </div>

            {rows && rows.length > 0 && (
                <div className="card overflow-hidden mb-6">
                    <div className="theme-sidebar text-white px-4 py-2 text-sm font-semibold">Preview — {rows.length} result(s)</div>
                    <table className="w-full text-sm text-left">
                        <thead style={{ backgroundColor: 'var(--surface-soft)' }} className="theme-text-muted uppercase text-xs">
                        <tr><th className="px-4 py-2">Code</th><th className="px-4 py-2">Title</th><th className="px-4 py-2">Score</th><th className="px-4 py-2">Grade</th></tr>
                        </thead>
                        <tbody className="divide-y theme-border">
                        {rows.map((r, i) => (
                            <tr key={i}>
                                <td className="px-4 py-2 font-medium">{r.courseCode}</td>
                                <td className="px-4 py-2">{r.courseTitle || '—'}</td>
                                <td className="px-4 py-2">{r.totalScore || '—'}</td>
                                <td className="px-4 py-2">{r.grade || '—'}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                    <div className="p-4">
                        <button onClick={handleSave} disabled={saving}
                                className="accent-gradient text-white rounded-lg px-6 py-2 text-sm font-medium btn-press disabled:opacity-50">
                            {saving ? 'Saving…' : `Save ${rows.length} Results`}
                        </button>
                    </div>
                </div>
            )}

            <h3 className="text-lg font-bold mb-3">Your Analytics</h3>
            {loadingAnalytics ? (
                <p className="theme-text-muted">Loading…</p>
            ) : (
                <div className="grid grid-cols-3 gap-4 mb-6">
                    <div className="card p-6 text-center">
                        <p className="text-3xl font-bold">{analytics.averageScore}</p>
                        <p className="text-sm theme-text-muted">Average Score</p>
                    </div>
                    <div className="card p-6">
                        <p className="text-sm font-semibold mb-2">Grade Distribution</p>
                        <div className="flex gap-3 text-sm flex-wrap">
                            {Object.entries(analytics.gradeDistribution).map(([g, c]) => (
                                <span key={g}><b>{g}</b>: {c}</span>
                            ))}
                            {Object.keys(analytics.gradeDistribution).length === 0 && <span className="theme-text-muted">No data yet</span>}
                        </div>
                    </div>
                    <div className="card p-6" style={{ backgroundColor: 'var(--surface-soft)' }}>
                        <p className="text-sm font-semibold mb-1">💡 Advice</p>
                        <p className="text-sm">{analytics.advice}</p>
                    </div>
                </div>
            )}

            {analytics && (analytics.weakCourses?.length > 0 || analytics.strongCourses?.length > 0) && (
                <div className="grid grid-cols-2 gap-4">
                    <div className="card overflow-hidden">
                        <div className="bg-red-600 text-white px-4 py-2 text-sm font-semibold">Courses to Focus On</div>
                        <ul className="divide-y theme-border">
                            {analytics.weakCourses.map((c, i) => (
                                <li key={i} className="px-4 py-2 text-sm flex justify-between">
                                    <span>{c.courseCode} — {c.courseTitle}</span>
                                    <span className="font-medium">{c.totalScore}</span>
                                </li>
                            ))}
                            {analytics.weakCourses.length === 0 && <li className="px-4 py-4 text-sm theme-text-muted">None — nice work.</li>}
                        </ul>
                    </div>
                    <div className="card overflow-hidden">
                        <div className="bg-green-600 text-white px-4 py-2 text-sm font-semibold">Strongest Courses</div>
                        <ul className="divide-y theme-border">
                            {analytics.strongCourses.map((c, i) => (
                                <li key={i} className="px-4 py-2 text-sm flex justify-between">
                                    <span>{c.courseCode} — {c.courseTitle}</span>
                                    <span className="font-medium">{c.totalScore}</span>
                                </li>
                            ))}
                            {analytics.strongCourses.length === 0 && <li className="px-4 py-4 text-sm theme-text-muted">Upload results to see this.</li>}
                        </ul>
                    </div>
                </div>
            )}
        </div>
    )
}
export default UploadMyResults