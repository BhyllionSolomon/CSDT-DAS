import { useEffect, useState } from 'react'
import { getAllSessions } from '../services/sessionService'
import { extractRoster, saveRoster } from '../services/studentRosterService'
import api from '../services/api'

async function getAllProgrammes() {
    const response = await api.get('/programmes')
    return response.data
}
async function getAllLevels() {
    const response = await api.get('/levels')
    return response.data
}

const LEVEL_CODES = ['100', '200', '300', '400']

function BulkUploadStudents() {
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])
    const [sessions, setSessions] = useState([])
    const [loading, setLoading] = useState(true)

    const [selectedProgramme, setSelectedProgramme] = useState(null)
    const [selectedLevel, setSelectedLevel] = useState(null)
    const [academicSessionId, setAcademicSessionId] = useState('')

    const [file, setFile] = useState(null)
    const [rows, setRows] = useState(null)
    const [extracting, setExtracting] = useState(false)
    const [saving, setSaving] = useState(false)
    const [saveResult, setSaveResult] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        Promise.all([getAllProgrammes(), getAllLevels(), getAllSessions()])
            .then(([p, l, s]) => { setProgrammes(p); setLevels(l); setSessions(s) })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    function resetToLevels() {
        setSelectedLevel(null)
        setFile(null)
        setRows(null)
        setSaveResult(null)
        setError(null)
    }

    function resetToProgrammes() {
        setSelectedProgramme(null)
        resetToLevels()
    }

    async function handleExtract() {
        if (!file) { setError('Choose a file first.'); return }
        setError(null)
        setSaveResult(null)
        setExtracting(true)
        try {
            const data = await extractRoster(file)
            setRows(data)
            if (data.length === 0) {
                setError('No matric numbers were found in this file. Check the file and try again.')
            }
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setExtracting(false)
        }
    }

    async function handleSave() {
        if (!academicSessionId) { setError('Select an academic session first.'); return }
        setError(null)
        setSaving(true)
        try {
            const result = await saveRoster(selectedProgramme.id, selectedLevel.id, academicSessionId, rows)
            setSaveResult(result)
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setSaving(false)
        }
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div>
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Bulk Upload Students</h2>
            <p className="text-sm text-slate-500 mb-6">
                Choose a programme, then a level, then upload a roster file (PDF, Word, CSV or Excel).
            </p>

            {/* Breadcrumb */}
            <div className="flex items-center gap-2 text-sm mb-6">
                <button onClick={resetToProgrammes} className="text-blue-600 hover:underline font-medium">
                    Programmes
                </button>
                {selectedProgramme && (
                    <>
                        <span className="text-slate-400">/</span>
                        <button onClick={resetToLevels} className="text-blue-600 hover:underline font-medium">
                            {selectedProgramme.name}
                        </button>
                    </>
                )}
                {selectedLevel && (
                    <>
                        <span className="text-slate-400">/</span>
                        <span className="font-medium text-slate-700">{selectedLevel.name}</span>
                    </>
                )}
            </div>

            {/* Step 1: pick programme */}
            {!selectedProgramme && (
                <div className="grid grid-cols-4 gap-4">
                    {programmes.map((p) => (
                        <button
                            key={p.id}
                            onClick={() => setSelectedProgramme(p)}
                            className="bg-white rounded-lg shadow p-6 text-left hover:shadow-md transition-shadow"
                        >
                            <p className="font-semibold text-slate-800">{p.name}</p>
                            <p className="text-sm text-slate-500">{p.code}</p>
                        </button>
                    ))}
                </div>
            )}

            {/* Step 2: pick level */}
            {selectedProgramme && !selectedLevel && (
                <div className="grid grid-cols-4 gap-4">
                    {levels
                        .filter((l) => LEVEL_CODES.includes(l.code))
                        .map((l) => (
                            <button
                                key={l.id}
                                onClick={() => setSelectedLevel(l)}
                                className="bg-white rounded-lg shadow p-6 text-center hover:shadow-md transition-shadow"
                            >
                                <p className="text-2xl font-bold text-slate-800">{l.code}</p>
                                <p className="text-sm text-slate-500">Level</p>
                            </button>
                        ))}
                </div>
            )}

            {/* Step 3: upload for that programme + level */}
            {selectedProgramme && selectedLevel && (
                <div>
                    {error && (
                        <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">{error}</div>
                    )}

                    {saveResult && (
                        <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">
                            <p>
                                Students created: {saveResult.studentsCreated} · Already existed: {saveResult.studentsSkipped}
                                {' '}· CGPA records saved: {saveResult.cgpaRecorded}
                            </p>
                            {saveResult.errors?.length > 0 && (
                                <ul className="mt-2 list-disc list-inside text-amber-700">
                                    {saveResult.errors.map((e, i) => <li key={i}>{e}</li>)}
                                </ul>
                            )}
                        </div>
                    )}

                    <div className="bg-white rounded-lg shadow p-4 mb-6 flex gap-4 items-end flex-wrap">
                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">Academic Session</label>
                            <select
                                value={academicSessionId}
                                onChange={(e) => setAcademicSessionId(e.target.value)}
                                className="border border-slate-300 rounded-md px-3 py-2 text-sm"
                            >
                                <option value="">Select…</option>
                                {sessions.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-700 mb-1">
                                File (PDF, Word, CSV or Excel)
                            </label>
                            <input
                                type="file"
                                accept=".pdf,.docx,.csv,.xlsx,.xls"
                                onChange={(e) => { setFile(e.target.files[0]); setRows(null); setSaveResult(null) }}
                                className="text-sm"
                            />
                        </div>

                        <button
                            onClick={handleExtract}
                            disabled={!file || extracting}
                            className="bg-slate-800 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-slate-900 disabled:opacity-50"
                        >
                            {extracting ? 'Extracting…' : 'Extract Student Details'}
                        </button>
                    </div>

                    {rows && rows.length > 0 && (
                        <>
                            <div className="bg-white rounded-lg shadow overflow-hidden mb-4">
                                <div className="bg-slate-800 text-white px-4 py-2 text-sm font-semibold">
                                    Preview — {rows.length} row(s) found
                                </div>
                                <table className="w-full text-sm text-left">
                                    <thead className="bg-slate-50 text-slate-600 uppercase text-xs">
                                    <tr>
                                        <th className="px-4 py-2">Matric No.</th>
                                        <th className="px-4 py-2">Name</th>
                                        <th className="px-4 py-2">CGPA</th>
                                    </tr>
                                    </thead>
                                    <tbody className="divide-y divide-slate-100">
                                    {rows.map((r, i) => (
                                        <tr key={i}>
                                            <td className="px-4 py-2 font-medium">{r.matricNumber}</td>
                                            <td className="px-4 py-2">{r.fullName || <span className="text-slate-400 italic">missing</span>}</td>
                                            <td className="px-4 py-2">{r.cgpa || '—'}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>

                            <button
                                onClick={handleSave}
                                disabled={saving || !academicSessionId}
                                className="bg-green-600 text-white rounded-md px-6 py-2 text-sm font-medium hover:bg-green-700 disabled:opacity-50"
                            >
                                {saving ? 'Saving…' : `Save ${rows.length} Students to ${selectedProgramme.code} ${selectedLevel.code}L`}
                            </button>
                        </>
                    )}
                </div>
            )}
        </div>
    )
}

export default BulkUploadStudents