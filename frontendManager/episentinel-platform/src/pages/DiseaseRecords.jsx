import { useMemo, useState } from 'react'
import { Search, Filter, Download, ChevronLeft, ChevronRight, ArrowUpDown, SlidersHorizontal } from 'lucide-react'
import Card from '../components/ui/Card'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import { DISEASE_RECORDS, DISEASES, STATES } from '../data/mockData'

const PAGE_SIZE = 8
const STATUS_VARIANT = { Confirmed: 'high', Suspected: 'medium', 'Under Investigation': 'info', 'Ruled Out': 'neutral' }

export default function DiseaseRecords() {
  const [query, setQuery] = useState('')
  const [diseaseFilter, setDiseaseFilter] = useState('All')
  const [stateFilter, setStateFilter] = useState('All')
  const [sortKey, setSortKey] = useState('reportedDate')
  const [sortDir, setSortDir] = useState('desc')
  const [page, setPage] = useState(1)
  const [advanced, setAdvanced] = useState(false)

  const filtered = useMemo(() => {
    let rows = DISEASE_RECORDS.filter((r) =>
      (diseaseFilter === 'All' || r.disease === diseaseFilter) &&
      (stateFilter === 'All' || r.state === stateFilter) &&
      (query === '' || r.id.toLowerCase().includes(query.toLowerCase()) || r.disease.toLowerCase().includes(query.toLowerCase()) || r.state.toLowerCase().includes(query.toLowerCase()))
    )
    rows.sort((a, b) => {
      const av = a[sortKey], bv = b[sortKey]
      const cmp = typeof av === 'number' ? av - bv : String(av).localeCompare(String(bv))
      return sortDir === 'asc' ? cmp : -cmp
    })
    return rows
  }, [query, diseaseFilter, stateFilter, sortKey, sortDir])

  const pages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const pageRows = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  const toggleSort = (key) => {
    if (sortKey === key) setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    else { setSortKey(key); setSortDir('asc') }
  }

  const Th = ({ label, k }) => (
    <th className="px-5 py-3 font-semibold cursor-pointer select-none" onClick={() => toggleSort(k)}>
      <span className="flex items-center gap-1">{label} <ArrowUpDown size={11} className={sortKey === k ? 'text-accent' : 'text-textMuted'} /></span>
    </th>
  )

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-bold text-textPrimary tracking-tight">Disease Records</h1>
          <p className="text-sm text-textMuted mt-1">{filtered.length.toLocaleString()} records matching current filters</p>
        </div>
        <Button variant="secondary" size="sm" icon={Download}>Export</Button>
      </div>

      <Card className="p-4">
        <div className="flex flex-col lg:flex-row gap-3">
          <div className="relative flex-1">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-textMuted" />
            <input
              value={query}
              onChange={(e) => { setQuery(e.target.value); setPage(1) }}
              placeholder="Search by record ID, disease, or state…"
              className="w-full bg-bgSunken border border-borderc rounded-lg pl-9 pr-3 py-2.5 text-sm text-textPrimary placeholder:text-textMuted focus-ring focus:border-accent2/60"
            />
          </div>
          <select value={diseaseFilter} onChange={(e) => { setDiseaseFilter(e.target.value); setPage(1) }} className="bg-bgSunken border border-borderc rounded-lg px-3.5 py-2.5 text-sm text-textPrimary focus-ring">
            <option>All</option>{DISEASES.map((d) => <option key={d}>{d}</option>)}
          </select>
          <select value={stateFilter} onChange={(e) => { setStateFilter(e.target.value); setPage(1) }} className="bg-bgSunken border border-borderc rounded-lg px-3.5 py-2.5 text-sm text-textPrimary focus-ring">
            <option>All</option>{STATES.map((s) => <option key={s.name}>{s.name}</option>)}
          </select>
          <Button variant="secondary" size="md" icon={SlidersHorizontal} onClick={() => setAdvanced((a) => !a)}>Advanced</Button>
        </div>

        {advanced && (
          <div className="grid sm:grid-cols-3 gap-3 mt-4 pt-4 border-t border-borderc animate-riseIn">
            <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Date From</label><input type="date" className="w-full bg-bgSunken border border-borderc rounded-lg px-3.5 py-2 text-sm focus-ring" /></div>
            <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Date To</label><input type="date" className="w-full bg-bgSunken border border-borderc rounded-lg px-3.5 py-2 text-sm focus-ring" /></div>
            <div><label className="block text-xs font-semibold text-textSecondary mb-1.5">Reporting Officer</label><input placeholder="Any officer" className="w-full bg-bgSunken border border-borderc rounded-lg px-3.5 py-2 text-sm focus-ring" /></div>
          </div>
        )}
      </Card>

      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-[11px] uppercase tracking-wide text-textMuted border-y border-borderc">
                <Th label="Record ID" k="id" />
                <Th label="Disease" k="disease" />
                <Th label="State / LGA" k="state" />
                <Th label="Reported" k="reportedDate" />
                <th className="px-5 py-3 font-semibold">Status</th>
                <Th label="Cases" k="cases" />
                <Th label="Deaths" k="deaths" />
                <th className="px-5 py-3 font-semibold">Officer</th>
              </tr>
            </thead>
            <tbody>
              {pageRows.map((r) => (
                <tr key={r.id} className="border-b border-borderc last:border-0 hover:bg-surfaceHover transition-colors">
                  <td className="px-5 py-3 font-mono text-[12.5px] text-accent">{r.id}</td>
                  <td className="px-5 py-3 font-medium text-textPrimary">{r.disease}</td>
                  <td className="px-5 py-3 text-textSecondary">{r.state} <span className="text-textMuted">/ {r.lga}</span></td>
                  <td className="px-5 py-3 font-mono text-textSecondary">{r.reportedDate}</td>
                  <td className="px-5 py-3"><Badge variant={STATUS_VARIANT[r.status]}>{r.status}</Badge></td>
                  <td className="px-5 py-3 font-mono">{r.cases}</td>
                  <td className="px-5 py-3 font-mono">{r.deaths}</td>
                  <td className="px-5 py-3 text-textSecondary">{r.officer}</td>
                </tr>
              ))}
              {pageRows.length === 0 && (
                <tr><td colSpan={8} className="text-center py-10 text-textMuted text-sm">No records match your filters.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="flex items-center justify-between px-5 py-4 border-t border-borderc text-sm">
          <span className="text-textMuted text-xs">Page {page} of {pages}</span>
          <div className="flex gap-2">
            <Button variant="secondary" size="sm" icon={ChevronLeft} onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1}>Prev</Button>
            <Button variant="secondary" size="sm" onClick={() => setPage((p) => Math.min(pages, p + 1))} disabled={page === pages}>Next <ChevronRight size={14} /></Button>
          </div>
        </div>
      </Card>
    </div>
  )
}
