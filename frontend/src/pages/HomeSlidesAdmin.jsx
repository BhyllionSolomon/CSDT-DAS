import { useEffect, useState } from 'react'
import { ArrowUp, ArrowDown, ImagePlus, Trash2 } from 'lucide-react'
import {
    listSlides, createSlide, updateSlide, uploadSlideImage, removeSlideImage, moveSlide, deleteSlide, slideImageUrl,
} from '../services/homeService'
import { compressImage } from '../utils/imageTools'

const msg = (err) => err.response?.data?.message || err.message
const MAX_SLIDES = 8

function SlideCard({ slide, first, last, busy, run }) {
    const [d, setD] = useState(slide)
    useEffect(() => setD(slide), [slide])

    const dirty = ['eyebrow', 'heading', 'body', 'sideHeading', 'sideText', 'active'].some((k) => (d[k] ?? '') !== (slide[k] ?? ''))
    const set = (k) => (e) => setD({ ...d, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

    async function pick(e) {
        const file = e.target.files?.[0]
        e.target.value = ''
        if (!file) return
        run(async () => uploadSlideImage(slide.id, await compressImage(file)), 'Picture saved.')
    }

    const input = 'w-full border theme-border rounded-lg px-3 py-1.5 text-sm'

    return (
        <div className="card p-4 flex gap-4 flex-wrap md:flex-nowrap">
            <div className="w-full md:w-56 shrink-0">
                <div className="aspect-video rounded-lg overflow-hidden flex items-center justify-center text-xs text-center p-2 theme-text-muted" style={{ backgroundColor: 'var(--surface-soft)' }}>
                    {slide.hasImage ? <img src={slideImageUrl(slide)} alt="" className="w-full h-full object-cover" /> : 'No picture yet. A stock photo is shown.'}
                </div>
                <div className="flex gap-2 mt-2 flex-wrap">
                    <label className="flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-lg accent-gradient text-white cursor-pointer btn-press">
                        <ImagePlus size={14} /> {slide.hasImage ? 'Replace' : 'Upload'}
                        <input type="file" accept="image/*" onChange={pick} disabled={busy} className="hidden" />
                    </label>
                    {slide.hasImage && (
                        <button onClick={() => run(() => removeSlideImage(slide.id), 'Picture removed.')} disabled={busy} className="text-xs text-red-600 hover:underline">Remove picture</button>
                    )}
                </div>
            </div>

            <div className="flex-1 min-w-0 space-y-2">
                <div className="grid sm:grid-cols-2 gap-2">
                    <input className={input} value={d.eyebrow || ''} onChange={set('eyebrow')} placeholder="Small heading (e.g. Welcome to CSDT)" maxLength={60} />
                    <input className={input} value={d.heading || ''} onChange={set('heading')} placeholder="Main headline" maxLength={150} />
                </div>
                <textarea className={input} rows={2} value={d.body || ''} onChange={set('body')} placeholder="Short paragraph" maxLength={400} />
                <div className="grid sm:grid-cols-2 gap-2">
                    <input className={input} value={d.sideHeading || ''} onChange={set('sideHeading')} placeholder="Side heading" maxLength={100} />
                    <input className={input} value={d.sideText || ''} onChange={set('sideText')} placeholder="Side text" maxLength={300} />
                </div>

                <div className="flex items-center gap-3 flex-wrap pt-1">
                    <label className="text-sm flex items-center gap-2"><input type="checkbox" checked={!!d.active} onChange={set('active')} /> Show on the home page</label>
                    <button disabled={busy || !dirty} onClick={() => run(() => updateSlide(slide.id, d), 'Slide saved.')}
                            className="accent-gradient text-white rounded-lg px-4 py-1.5 text-sm font-medium btn-press disabled:opacity-40">Save text</button>
                    <div className="flex-1" />
                    <button disabled={busy || first} onClick={() => run(() => moveSlide(slide.id, 'UP'))} className="p-1.5 border theme-border rounded-lg disabled:opacity-30" aria-label="Move up"><ArrowUp size={15} /></button>
                    <button disabled={busy || last} onClick={() => run(() => moveSlide(slide.id, 'DOWN'))} className="p-1.5 border theme-border rounded-lg disabled:opacity-30" aria-label="Move down"><ArrowDown size={15} /></button>
                    <button disabled={busy} onClick={() => { if (window.confirm('Delete this slide and its picture?')) run(() => deleteSlide(slide.id), 'Slide deleted.') }}
                            className="p-1.5 text-red-600 border border-red-200 rounded-lg" aria-label="Delete slide"><Trash2 size={15} /></button>
                </div>
            </div>
        </div>
    )
}

function HomeSlidesAdmin() {
    const [slides, setSlides] = useState([])
    const [loading, setLoading] = useState(true)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState(null)
    const [notice, setNotice] = useState(null)

    useEffect(() => {
        listSlides().then(setSlides).catch((e) => setError(msg(e))).finally(() => setLoading(false))
    }, [])

    async function run(action, success) {
        setError(null); setNotice(null); setBusy(true)
        try {
            await action()
            setSlides(await listSlides())
            if (success) setNotice(success)
        } catch (err) { setError(msg(err)) } finally { setBusy(false) }
    }

    if (loading) return <p className="theme-text-muted">Loading…</p>

    return (
        <div className="max-w-4xl">
            <h2 className="text-2xl font-bold mb-2">Home Page Images</h2>
            <p className="theme-text-muted text-sm mb-6">
                These slides appear on the sign-in page and change every few seconds. Wide pictures work best (1920 × 1080 or larger). Big photos are shrunk
                automatically, and changes show for visitors straight away.
            </p>

            {error && <div className="mb-4 px-4 py-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>}
            {notice && <div className="mb-4 px-4 py-3 rounded-lg bg-green-50 text-green-700 text-sm">{notice}</div>}

            <div className="space-y-4 mb-6">
                {slides.map((s, i) => (
                    <SlideCard key={s.id} slide={s} first={i === 0} last={i === slides.length - 1} busy={busy} run={run} />
                ))}
            </div>

            <button disabled={busy || slides.length >= MAX_SLIDES}
                    onClick={() => run(() => createSlide({ eyebrow: 'New slide', heading: 'Your headline here', active: true }), 'Slide added. Upload its picture and edit the text.')}
                    className="border theme-border rounded-lg px-5 py-2 text-sm font-medium btn-press disabled:opacity-40">
                Add a slide {slides.length >= MAX_SLIDES ? `(maximum ${MAX_SLIDES})` : ''}
            </button>
        </div>
    )
}

export default HomeSlidesAdmin