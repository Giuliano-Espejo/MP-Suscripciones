import { useState } from 'react'
import { api } from '../api/client'
import type { Promotion, PromotionInput } from '../api/types'
import { Empty, Modal } from '../components/ui'
import { formatDate } from '../lib/format'
import { useRun } from '../lib/notify'
import { useData } from '../lib/useData'

export function Promotions({ today, onChange }: { today: string | undefined; onChange: () => void }) {
  const { data, error, reload } = useData(() => api.promotions(), [])
  const [editing, setEditing] = useState<{ promo: Promotion | null; input: PromotionInput } | null>(null)
  const run = useRun()

  if (error) return <p className="error-box">{error}</p>
  if (!data) return <p className="muted">Cargando…</p>

  const isCurrent = (p: Promotion) => !!today && p.active && p.validFrom <= today && today <= p.validTo

  return (
    <div className="stack">
      <div className="row-between">
        <p className="muted">
          Las promociones aplican un descuento a las altas dentro de su ventana de fechas, durante los meses indicados.
          Al vencer, el sistema actualiza el monto en Mercado Pago al precio de lista.
        </p>
        <button className="btn primary" onClick={() => setEditing({
          promo: null,
          input: { name: '', validFrom: today ?? '', validTo: today ?? '', discountPercent: 10, durationMonths: 3, active: true },
        })}>+ Nueva promoción</button>
      </div>
      {data.length === 0 ? <Empty>No hay promociones.</Empty> : (
        <div className="promo-list">
          {data.map((p) => (
            <article key={p.id} className={`card promo-card ${isCurrent(p) ? 'current' : ''}`}>
              <div>
                <div className="row-gap">
                  <h3>{p.name}</h3>
                  {isCurrent(p) && <span className="badge status-active">Vigente</span>}
                  {!p.active && <span className="badge status-cancelled">Desactivada</span>}
                </div>
                <p className="muted">
                  Altas del {formatDate(p.validFrom)} al {formatDate(p.validTo)} · {p.discountPercent}% off por {p.durationMonths} meses
                </p>
              </div>
              <button className="btn sm" onClick={() => setEditing({ promo: p, input: { ...p } })}>Editar</button>
            </article>
          ))}
        </div>
      )}
      {editing && (
        <PromotionForm initial={editing.input} onClose={() => setEditing(null)} onSave={async (input) => {
          const saved = await run(() => editing.promo ? api.updatePromotion(editing.promo.id, input) : api.createPromotion(input), 'Promoción guardada')
          if (saved) { setEditing(null); void reload(); onChange() }
        }} />
      )}
    </div>
  )
}

function PromotionForm({ initial, onClose, onSave }: { initial: PromotionInput; onClose: () => void; onSave: (i: PromotionInput) => void }) {
  const [form, setForm] = useState(initial)
  const set = <K extends keyof PromotionInput>(k: K, v: PromotionInput[K]) => setForm((f) => ({ ...f, [k]: v }))
  return (
    <Modal title="Promoción" onClose={onClose}>
      <form className="form" onSubmit={(e) => { e.preventDefault(); onSave(form) }}>
        <label>Nombre<input required value={form.name} onChange={(e) => set('name', e.target.value)} /></label>
        <div className="form-row">
          <label>Altas desde<input type="date" required value={form.validFrom} onChange={(e) => set('validFrom', e.target.value)} /></label>
          <label>Altas hasta<input type="date" required min={form.validFrom} value={form.validTo} onChange={(e) => set('validTo', e.target.value)} /></label>
        </div>
        <div className="form-row">
          <label>Descuento (%)<input type="number" min={1} max={100} required value={form.discountPercent} onChange={(e) => set('discountPercent', Number(e.target.value))} /></label>
          <label>Duración (meses)<input type="number" min={1} max={24} required value={form.durationMonths} onChange={(e) => set('durationMonths', Number(e.target.value))} /></label>
        </div>
        <label className="check"><input type="checkbox" checked={form.active} onChange={(e) => set('active', e.target.checked)} /> Activa</label>
        <footer className="form-actions">
          <button type="button" className="btn ghost" onClick={onClose}>Cancelar</button>
          <button type="submit" className="btn primary">Guardar</button>
        </footer>
      </form>
    </Modal>
  )
}
