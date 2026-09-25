import { useEffect, useState } from 'react'
import { getRequiredUnits, setRequiredUnits } from '../services/registrationService'
import api from '../services/api'

async function getAllProgrammes() {
    const response = await api.get('/programmes')
    return response.data
}

async function getAllLevels() {
    const response = await api.get('/levels')
    return response.data
}

function SetUnitRequirements() {
    const [programmes, setProgrammes] = useState([])
    const [levels, setLevels] = useState([])

    const [programmeId, setProgrammeId] = useState('')
    const [levelId, setLevelId] = useState('')
    const [semester, setSemester] = useState('FIRST')
    const [requiredUnits, setUnits] = useState('')

    const [current, setCurrent] = useState(null)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState(null)
    const [success, setSuccess] = useState(null)

    useEffect(() => {
        async function loadData() {
            setLoading(true)
            setError(null)
            try {
                const [programmesData, levelsData] = await Promise.all([getAllProgrammes(), getAllLevels()])
                setProgrammes(programmesData || [])
                setLevels(levelsData || [])
            } catch (err) {
                setError(err.response?.data?.message || err.message || 'Failed to load programmes and levels.')
            } finally {
                setLoading(false)
            }
        }
        loadData()
    }, [])

    async function handleLookup() {
        if (!programmeId || !levelId || !semester) return
        setError(null)
        setSuccess(null)
        setCurrent(null)
        try {
            const data = await getRequiredUnits(programmeId, levelId, semester)
            if (data && data.requiredUnits !== undefined) {
                setCurrent(data.requiredUnits)
                setUnits(String(data.requiredUnits))
            } else {
                setCurrent(null)
                setUnits('')
                setError('No required-unit value has been configured for this selection.')
            }
        } catch (err) {
            const status = err.response?.status
            if (status === 404) {
                setCurrent(null)
                setUnits('')
                setError('No required units have been configured for this programme, level and semester.')
            } else {
                setError(err.response?.data?.message || err.message || 'Failed to retrieve the current required units.')
            }
        }
    }

    async function handleSave(e) {
        e.preventDefault()
        setError(null)
        setSuccess(null)

        if (!programmeId) { setError('Please select a programme.'); return }
        if (!levelId) { setError('Please select a level.'); return }
        if (!semester) { setError('Please select a semester.'); return }

        const units = Number(requiredUnits)
        if (!Number.isFinite(units) || units <= 0) {
            setError('Required units must be a number greater than zero.')
            return
        }

        setSaving(true)
        try {
            await setRequiredUnits(programmeId, levelId, semester, units)
            setCurrent(units)
            setUnits(String(units))
            setSuccess('Required units updated successfully.')
        } catch (err) {
            setError(err.response?.data?.message || err.message || 'Failed to update required units.')
        } finally {
            setSaving(false)
        }
    }

    function handleProgrammeChange(e) { setProgrammeId(e.target.value); setCurrent(null); setSuccess(null); setError(null) }
    function handleLevelChange(e) { setLevelId(e.target.value); setCurrent(null); setSuccess(null); setError(null) }
    function handleSemesterChange(e) { setSemester(e.target.value); setCurrent(null); setSuccess(null); setError(null) }

    if (loading) {
        return <div className="max-w-lg"><p className="text-slate-500">Loading programmes and levels…</p></div>
    }

    return (
        <div className="max-w-lg">
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Set Required Units</h2>
            <p className="text-sm text-slate-500 mb-6">
                Set the exact total credit units a student must register for a given
                programme, level and semester. Students must match this total exactly.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">{error}</div>}
            {success && <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">{success}</div>}

            <div className="bg-white rounded-lg shadow p-6 space-y-4">
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">Programme</label>
                    <select value={programmeId} onChange={handleProgrammeChange} className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm">
                        <option value="">Select programme…</option>
                        {programmes.map((p) => (
                            <option key={p.id} value={p.id}>{p.name} ({p.code})</option>
                        ))}
                    </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Level</label>
                        <select value={levelId} onChange={handleLevelChange} className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm">
                            <option value="">Select level…</option>
                            {levels.map((l) => (
                                <option key={l.id} value={l.id}>{l.name}</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Semester</label>
                        <select value={semester} onChange={handleSemesterChange} className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm">
                            <option value="FIRST">First</option>
                            <option value="SECOND">Second</option>
                        </select>
                    </div>
                </div>

                <button type="button" onClick={handleLookup} disabled={!programmeId || !levelId} className="text-sm text-blue-600 underline disabled:opacity-50">
                    Check current value
                </button>

                {current !== null && (
                    <div className="px-4 py-3 rounded-md bg-slate-50">
                        <p className="text-sm text-slate-600">Current required units: <span className="font-semibold ml-1">{current}</span></p>
                    </div>
                )}

                <form onSubmit={handleSave} className="flex gap-3 items-end">
                    <div className="flex-1">
                        <label className="block text-sm font-medium text-slate-700 mb-1">Required Units</label>
                        <input type="number" min="1" step="1" value={requiredUnits} onChange={(e) => setUnits(e.target.value)} required
                               className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm" placeholder="e.g. 18" />
                    </div>
                    <button type="submit" disabled={saving || !programmeId || !levelId || !requiredUnits}
                            className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
                        {saving ? 'Saving…' : 'Save'}
                    </button>
                </form>
            </div>
        </div>
    )
}

export default SetUnitRequirements