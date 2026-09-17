import { useEffect, useState } from 'react'
import { getAllPropgrammes } from '../services/programmeService'
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
        Promise.all([getAllProgrammes(), getAllLevels()])
            .then(([p, l]) => {
                setProgrammes(p)
                setLevels(l)
            })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    }, [])

    async function handleLookup() {
        if (!programmeId || !levelId) return
        setError(null)
        setSuccess(null)
        try {
            const data = await getRequiredUnits(programmeId, levelId, semester)
            setCurrent(data.requiredUnits)
            setUnits(String(data.requiredUnits))
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        }
    }

    async function handleSave(e) {
        e.preventDefault()
        setError(null)
        setSuccess(null)
        setSaving(true)

        try {
            await setRequiredUnits(programmeId, levelId, semester, Number(requiredUnits))
            setCurrent(Number(requiredUnits))
            setSuccess('Required units updated.')
        } catch (err) {
            setError(err.response?.data?.message || err.message)
        } finally {
            setSaving(false)
        }
    }

    if (loading) return <p className="text-slate-500">Loading…</p>

    return (
        <div className="max-w-lg">
            <h2 className="text-2xl font-bold text-slate-800 mb-2">Set Required Units</h2>
            <p className="text-sm text-slate-500 mb-6">
                Set the exact total credit units a student must register for a given
                programme, level and semester. Students must match this total exactly.
            </p>

            {error && (
                <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 text-sm">
                    {error}
                </div>
            )}
            {success && (
                <div className="mb-4 px-4 py-3 rounded-md bg-green-50 text-green-700 text-sm">
                    {success}
                </div>
            )}

            <div className="bg-white rounded-lg shadow p-6 space-y-4">
                <div>
                    <label className="block text-sm font-medium text-slate-700 mb-1">Programme</label>
                    <select
                        value={programmeId}
                        onChange={(e) => { setProgrammeId(e.target.value); setCurrent(null) }}
                        className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                    >
                        <option value="">Select…</option>
                        {programmes.map((p) => (
                            <option key={p.id} value={p.id}>{p.name} ({p.code})</option>
                        ))}
                    </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Level</label>
                        <select
                            value={levelId}
                            onChange={(e) => { setLevelId(e.target.value); setCurrent(null) }}
                            className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                        >
                            <option value="">Select…</option>
                            {levels.map((l) => (
                                <option key={l.id} value={l.id}>{l.name}</option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-slate-700 mb-1">Semester</label>
                        <select
                            value={semester}
                            onChange={(e) => { setSemester(e.target.value); setCurrent(null) }}
                            className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                        >
                            <option value="FIRST">First</option>
                            <option value="SECOND">Second</option>
                        </select>
                    </div>
                </div>

                <button
                    onClick={handleLookup}
                    disabled={!programmeId || !levelId}
                    className="text-sm text-blue-600 underline disabled:opacity-50"
                >
                    Check current value
                </button>

                {current !== null && (
                    <p className="text-sm text-slate-600">
                        Current required units: <span className="font-semibold">{current}</span>
                    </p>
                )}

                <form onSubmit={handleSave} className="flex gap-3 items-end">
                    <div className="flex-1">
                        <label className="block text-sm font-medium text-slate-700 mb-1">
                            Required Units
                        </label>
                        <input
                            type="number"
                            min="1"
                            value={requiredUnits}
                            onChange={(e) => setUnits(e.target.value)}
                            required
                            className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm"
                        />
                    </div>
                    <button
                        type="submit"
                        disabled={saving || !programmeId || !levelId}
                        className="bg-blue-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                    >
                        {saving ? 'Saving…' : 'Save'}
                    </button>
                </form>
            </div>
        </div>
    )
}

export default SetUnitRequirements