import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Bell, ChevronRight } from 'lucide-react'
import { getNotices } from '../services/noticeService'

function NoticeStrip() {
    const [board, setBoard] = useState(null)

    useEffect(() => { getNotices().then(setBoard).catch(() => setBoard(null)) }, [])

    const unread = board ? board.notices.filter((n) => !n.read).slice(0, 3) : []
    if (!board || unread.length === 0) return null

    return (
        <section className="mb-8">
            <div className="flex items-center justify-between mb-2">
                <p className="flex items-center gap-2 font-semibold"><Bell size={16} /> New notices <span className="text-xs px-2 py-0.5 rounded-full accent-gradient text-white">{board.unread}</span></p>
                <Link to="/notices" className="text-sm flex items-center gap-1" style={{ color: 'var(--accent)' }}>Notice Board <ChevronRight size={14} /></Link>
            </div>
            <div className="space-y-2">
                {unread.map((n) => (
                    <Link key={n.id} to="/notices" className="card card-hover p-3 block" style={n.priority === 'URGENT' ? { borderColor: '#fca5a5' } : undefined}>
                        <span className="block text-sm font-semibold">{n.priority === 'URGENT' ? 'Urgent: ' : ''}{n.title}</span>
                        <span className="block text-xs theme-text-muted truncate">{n.audienceLabel} · {n.body}</span>
                    </Link>
                ))}
            </div>
        </section>
    )
}

export default NoticeStrip