import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import TopBar from './TopBar'

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <div className="flex min-h-screen bg-bg">
      <Sidebar collapsed={collapsed} setCollapsed={setCollapsed} />

      {mobileOpen && (
        <div className="fixed inset-0 z-40 md:hidden">
          <div className="absolute inset-0 bg-black/50" onClick={() => setMobileOpen(false)} />
          <div className="absolute left-0 top-0 h-full w-[268px]">
            <Sidebar collapsed={false} setCollapsed={() => {}} />
          </div>
        </div>
      )}

      <div className="flex-1 min-w-0 flex flex-col">
        <TopBar onMenuClick={() => setMobileOpen(true)} />
        <main className="flex-1 p-4 md:p-6 lg:p-8 max-w-[1800px] w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
