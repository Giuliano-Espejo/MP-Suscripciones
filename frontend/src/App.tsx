import { useEffect, useState } from 'react'
import { api } from './api/client'
import { NotifyProvider } from './lib/notify'
import { useData } from './lib/useData'
import { ActiveMembers } from './pages/ActiveMembers'
import { Overview } from './pages/Overview'
import { Plans } from './pages/Plans'
import { Promotions } from './pages/Promotions'
import { Subscriptions } from './pages/Subscriptions'

type Section = 'overview' | 'plans' | 'subscriptions' | 'members' | 'promotions'

const NAV: { id: Section; hash: string; label: string; title: string }[] = [
  { id: 'overview', hash: 'resumen', label: 'Resumen', title: 'Resumen' },
  { id: 'plans', hash: 'planes', label: 'Planes', title: 'Planes y niveles' },
  { id: 'subscriptions', hash: 'suscripciones', label: 'Suscripciones', title: 'Suscripciones' },
  { id: 'members', hash: 'socios', label: 'Socios activos', title: 'Socios activos' },
  { id: 'promotions', hash: 'promociones', label: 'Promociones', title: 'Promociones' },
]

const sectionFromHash = (): Section =>
  NAV.find((n) => n.hash === window.location.hash.slice(1))?.id ?? 'overview'

export default function App() {
  const [section, setSectionState] = useState<Section>(sectionFromHash)
  const setSection = (s: Section) => { window.location.hash = NAV.find((n) => n.id === s)!.hash }
  useEffect(() => {
    const onHash = () => setSectionState(sectionFromHash())
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  }, [])
  const [refreshKey, setRefreshKey] = useState(0)
  const { data: summary, error } = useData(() => api.summary(), [refreshKey])
  const bump = () => setRefreshKey((k) => k + 1)
  const mode = summary?.gatewayMode ?? 'demo'
  const current = NAV.find((n) => n.id === section)!

  return (
    <NotifyProvider>
      <div className="shell">
        <aside className="sidebar">
          <div className="brand">
            <span className="brand-mark" aria-hidden="true">◆</span>
            <span>Membresías</span>
          </div>
          <nav>
            {NAV.map((n) => (
              <button key={n.id} className={`nav-item ${section === n.id ? 'on' : ''}`} onClick={() => setSection(n.id)}>
                {n.label}
                {n.id === 'subscriptions' && !!summary?.pending && <span className="nav-count">{summary.pending}</span>}
              </button>
            ))}
          </nav>
          <div className={`mode ${mode}`}>
            <span className="dot" />
            {mode === 'demo' ? 'Pasarela en modo demo' : 'Mercado Pago conectado'}
          </div>
        </aside>

        <main className="main">
          <header className="page-head">
            <h1>{current.title}</h1>
          </header>
          {error && <p className="error-box">No se pudo conectar con el backend ({error}). ¿Está corriendo en el puerto 8080?</p>}
          {section === 'overview' && <Overview refreshKey={refreshKey} />}
          {section === 'plans' && <Plans onChange={bump} />}
          {section === 'subscriptions' && <Subscriptions mode={mode} onChange={bump} />}
          {section === 'members' && <ActiveMembers onChange={bump} />}
          {section === 'promotions' && <Promotions today={summary?.today} onChange={bump} />}
        </main>
      </div>
    </NotifyProvider>
  )
}
