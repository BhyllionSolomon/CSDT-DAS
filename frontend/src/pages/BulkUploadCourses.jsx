import { useEffect, useState } from 'react'
import { extractCourseRoster, saveCourseRoster } from '../services/courseRosterService'
import { getAllProgrammes } from '../services/programmeService'
import api from '../services/api'

async function getAllLevels() {
    const response = await api.get('/levels')
    return response.data
}

const LEVEL_CODES = ['100', '200', '300', '400']

function BulkUploadCourses() {
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [loading, setLoading] = useState(true)

    const [selectedProgramme, setSelectedProgramme] = useState(null)
    const [selectedLevel, setSelectedLevel] = useState(null)
    const [semester, setSemester] = useState('FIRST')

    const [file, setFile] = useState(null)
    const [rows, setRows] = useState(null)
    const [extracting, setExtracting] = useState(false)
    const [saving, setSaving] = useState(false)
    const [saveResult, setSaveResult] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        Promise.all([getAllProgrammes(), getAllLevels()])
            .then(([p, l]) => { setProgrammes(p); setLevels(l) })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    function resetToLevels() { setSelectedLevel(null); setFile(null); setRows(null); setSaveResult(null); setError(null) }
    function resetToProgrammes() { setSelectedProgramme(null); resetToLevels() }

    async function handleExtract() {
        if (!file) { setError('Choose a file first.'); return }
        setError(null); setSaveResult(null); setExtracting(true)
        try {
            const data = await extractCourseRoster(file)
            setRows(data)
            if (data.length === 0) setError('No course codes were found in this file.')
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setExtracting(false) }
    }

    async function handleSave() {
        setError(null); setSaving(true)
        try {
            const result = await saveCourseRoster(selectedProgramme.id, selectedLevel.id, semester, rows)
            setSaveResult(result)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally { setSaving(false) }
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Bulk Upload Courses</h2>
            <p className="text-sm text-slate-500 mb-6">
                Choose a programme, a level, and a semester, then upload a course list (CSV or Excel).
            </p>

            <div className="flex items-center gap-2 text-sm mb-6">
                <button onClick={resetToProgrammes} className="text-blue-600 hover:underline font-medium">Programmes</button>
                {selectedProgramme && (
                    <>
                        <span className="text-slate-400">/</span>
                        <button onClick={resetToLevels} className="text-blue-600 hover:underline font-medium">{selectedProgramme.name}</button>
                    </>
                )}
                {selectedLevel && (
                    <>
                        <span className="text-slate-400">/</span>
                        <span className="font-medium text-slate-700">{selectedLevel.name}</span>
                    </>
                )}
            </div>

            {!selectedProgramme && (
                <div className="grid grid-cols-4 gap-4">
                    {programmes.map((p) => (
                        <button key={p.id} onClick={() => setSelectedProgramme(p)}
                                className="bg-white rounded-lg shadow p-6 text-left hover:shadow-md transition-shadow">
                            <p className="font-semibold text-slate-800">{p.name}</p>
                            <p className="text-sm text-slate-500">{p.code}</p>
                        </button>
                    ))}
                </div>
            )}

            {selectedProgramme && !selectedLevel && (
                <div className="grid grid-cols-4 gap-4">
                    {levels.filter((l) => LEVEL_CODES.includes(l.code)).map((l) => (
                        <button key={l.id} onClick={() => setSelectedLevel(l)}
                                className="bg-white rounded-lg shadow p-6 text-center hover:shadow-md transition-shadow">
                            <p className="text-2xl font-bold text-slate-800">{l.code}</p>
                            <p className="text-sm text-slate-500">Level</p>
                        </button>
                    ))}
                </div>
            )}

            {selectedProgramme && selectedLevel && (
                <div>
                    {error && <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">{error}</div>}
                    {saveResult && (
                        <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">
                            <p>Created: {saveResult.created} · Linked to programme (already existed): {saveResult.linkedToProgramme}</p>
                            {saveResult.errors?.length > 0 && (
                                <ul className="mt-2 list-disc list-inside text-amber-700">{saveResult.errors.map((e, i) => <li key={i}>{e}</li>)}</ul>
                            )}
                        </div>
                    )}

                    <div className="bg-white rounded-lg shadow p-4 mb-6 flex gap-4 items-end flex-wrap">
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">Semester</label>
                            <select value={semester} onChange={(e) => setSemester(e.target.value)}
                                    className="border border-slate-300 rounded-md px-3 py-2 text-sm">
                                <option value="FIRST">First</option>
                                <option value="SECOND">Second</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">File (CSV or Excel)</label>
                            <input type="file" accept=".csv,.xlsx,.xls"
                                   onChange={(e) => { setFile(e.target.files[0]); setRows(null); setSaveResult(null) }}
                                   className="text-sm" />
                        </div>
                        <button onClick={handleExtract} disabled={!file || extracting}
                                className="bg-slate-800 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-slate-900 disabled:opacity-50">
                            {extracting ? 'Extracting…' : 'Extract Course Details'}
                        </button>
                    </div>

                    {rows && rows.length > 0 && (
                        <>
                            <div className="bg-white rounded-lg shadow overflow-hidden mb-4">
                                <div className="bg-slate-800 text-white px-4 py-2 text-sm font-semibold">Preview — {rows.length} course(s)</div>
                                <table className="w-full text-sm text-left">
                                    <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                                    <tr><th className="px-4 py-2">Code</th><th className="px-4 py-2">Title</th><th className="px-4 py-2">Unit</th></tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                    {rows.map((r, i) => (
                                        <tr key={i}>
                                            <td className="px-4 py-2 font-medium">{r.code}</td>
                                            <td className="px-4 py-2">{r.title || <span className="text-slate-400 italic">missing</span>}</td>
                                            <td className="px-4 py-2">{r.creditUnit || '3 (default)'}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                            <button onClick={handleSave} disabled={saving}
                                    className="bg-green-600 text-white rounded-md px-6 py-2 text-sm font-medium hover:bg-green-700 disabled:opacity-50">
                                {saving ? 'Saving…' : `Save ${rows.length} Courses to ${selectedProgramme.code} ${selectedLevel.code}L`}
                            </button>
                        </>
                    )}
                </div>
            )}
        </div>
    )
}
export default BulkUploadCourses