import { useMemo, useState } from 'react'
import { api } from '../api/client'
import type { Subscription } from '../api/types'
import { ConsumptionsModal } from '../components/ConsumptionsModal'
import { Empty, HoursBar, TierBadge } from '../components/ui'
import { formatDate, formatMoney } from '../lib/format'
import { useData } from '../lib/useData'

export function ActiveMembers({ onChange }: { onChange: () => void }) {
  const { data, error, reload } = useData(() => api.activeMembers(), [])
  const [query, setQuery] = useState('')
  const [consuming, setConsuming] = useState<Subscription | null>(null)

  const rows = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (data ?? []).filter((s) => !q || s.memberName.toLowerCase().includes(q) || s.memberEmail.toLowerCase().includes(q))
  }, [data, query])

  if (error) return <p className="error-box">{error}</p>
  if (!data) return <p className="muted">Cargando…</p>

  return (
    <div className="stack">
      <div className="row-between">
        <input className="search" type="search" placeholder="Buscar por nombre o email" value={query} onChange={(e) => setQuery(e.target.value)} />
        <span className="muted">{rows.length} socios activos</span>
      </div>
      {rows.length === 0 ? <Empty>No hay socios activos{query ? ' que coincidan' : ''}.</Empty> : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr><th>Socio</th><th>Nivel</th><th className="num">Cuota</th><th>Socio desde</th><th>Permanencia</th><th>Horas disponibles</th><th aria-label="Acciones" /></tr>
            </thead>
            <tbody>
              {rows.map((s) => (
                <tr key={s.id}>
                  <td><strong>{s.memberName}</strong><div className="muted small">{s.memberEmail}</div></td>
                  <td><TierBadge tier={s.tier} /><div className="muted small">{s.planName}</div></td>
                  <td className="num">{formatMoney(s.amount)}{s.promoActive && <div className="small promo-text">Promo</div>}</td>
                  <td>{formatDate(s.startDate)}</td>
                  <td>
                    {s.permanenceFulfilled ? <span className="ok-text">Cumplida</span> : <>Hasta {formatDate(s.minEndDate)}</>}
                    {s.scheduledCancelDate && <div className="small warn-text">Baja el {formatDate(s.scheduledCancelDate)}</div>}
                  </td>
                  <td><HoursBar used={s.hoursUsed} included={s.hoursIncluded} /></td>
                  <td className="actions"><button className="btn xs ghost" onClick={() => setConsuming(s)}>Consumos</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {consuming && <ConsumptionsModal sub={consuming} onClose={() => { setConsuming(null); void reload(); onChange() }} />}
    </div>
  )
}
