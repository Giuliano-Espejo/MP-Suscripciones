import { useState } from 'react'
import { api } from '../api/client'
import type { Subscription } from '../api/types'
import { formatDate, formatHours } from '../lib/format'
import { useRun } from '../lib/notify'
import { useData } from '../lib/useData'
import { Empty, HoursBar, Modal, TierBadge } from './ui'

/** Beneficios del plan, horas disponibles del periodo y registro de consumos. */
export function ConsumptionsModal({ sub, onClose }: { sub: Subscription; onClose: () => void }) {
  const { data, reload } = useData(() => api.consumptions(sub.id), [sub.id])
  const [hours, setHours] = useState(1)
  const [description, setDescription] = useState('')
  const run = useRun()

  return (
    <Modal title={`Beneficios y consumos · ${sub.memberName}`} onClose={onClose} wide>
      {!data ? <p className="muted">Cargando…</p> : (
        <div className="consumption-grid">
          <section className="stack">
            <div><TierBadge tier={sub.tier} /> <strong>{sub.planName}</strong></div>
            <div>
              <h4>Horas del período</h4>
              <p className="muted small">{formatDate(data.periodFrom)} al {formatDate(data.periodTo)} (se renuevan cada mes)</p>
              <HoursBar used={data.hoursUsed} included={data.hoursIncluded} />
            </div>
            <div>
              <h4>Beneficios incluidos</h4>
              {data.benefits.length ? (
                <ul className="benefits">{data.benefits.map((b) => <li key={b}>{b}</li>)}</ul>
              ) : <p className="muted">El plan no tiene beneficios cargados.</p>}
            </div>
          </section>

          <section className="stack">
            {sub.status === 'ACTIVE' ? (
              <form className="form inline-form" onSubmit={async (e) => {
                e.preventDefault()
                const r = await run(() => api.addConsumption(sub.id, { hours, description }), `Consumo de ${formatHours(hours)} registrado`)
                if (r) { setDescription(''); void reload() }
              }}>
                <label>Horas<input type="number" min={0.25} step={0.25} max={data.hoursAvailable} required value={hours} onChange={(e) => setHours(Number(e.target.value))} /></label>
                <label className="grow">Detalle<input maxLength={200} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Sala de reuniones, clase, etc." /></label>
                <button className="btn primary" disabled={data.hoursAvailable <= 0}>Registrar</button>
              </form>
            ) : <p className="muted small">Solo las suscripciones activas pueden registrar consumos.</p>}

            <h4>Consumos del período</h4>
            {data.items.length === 0 ? <Empty>Sin consumos en este período.</Empty> : (
              <table className="table compact">
                <thead><tr><th>Fecha</th><th>Detalle</th><th className="num">Horas</th></tr></thead>
                <tbody>
                  {data.items.map((c) => (
                    <tr key={c.id}><td>{formatDate(c.date)}</td><td>{c.description || <span className="muted">—</span>}</td><td className="num">{formatHours(c.hours)}</td></tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </div>
      )}
    </Modal>
  )
}
