import { useState } from 'react'
import { api } from '../api/client'
import { TIERS, type Plan, type PlanInput, type Tier } from '../api/types'
import { Empty, Modal, TierBadge } from '../components/ui'
import { formatHours, formatMoney, tierLabel } from '../lib/format'
import { useRun } from '../lib/notify'
import { useData } from '../lib/useData'

const EMPTY: PlanInput = {
  name: '', description: '', tier: 'BASICO', price: 0, monthlyHours: 0, minPermanenceMonths: 3, benefits: [],
}

export function Plans({ onChange }: { onChange: () => void }) {
  const { data: plans, error, reload } = useData(() => api.plans(), [])
  const [editing, setEditing] = useState<{ plan: Plan | null; input: PlanInput } | null>(null)
  const run = useRun()

  const refresh = () => { void reload(); onChange() }
  const activeTiers = new Set((plans ?? []).filter((p) => p.active).map((p) => p.tier))
  const freeTier = TIERS.find((t) => !activeTiers.has(t))

  const openNew = () => setEditing({ plan: null, input: { ...EMPTY, tier: freeTier ?? 'BASICO' } })
  const openEdit = (p: Plan) => setEditing({
    plan: p,
    input: {
      name: p.name, description: p.description ?? '', tier: p.tier, price: p.price,
      monthlyHours: p.monthlyHours, minPermanenceMonths: p.minPermanenceMonths, benefits: p.benefits,
    },
  })

  if (error) return <p className="error-box">{error}</p>
  if (!plans) return <p className="muted">Cargando…</p>

  const byTier = (t: Tier) => plans.filter((p) => p.tier === t)

  return (
    <div className="stack">
      <div className="row-between">
        <p className="muted">
          Hasta tres niveles de membresía, con un plan activo por nivel. Cada plan se registra en Mercado Pago
          como <code>preapproval_plan</code> y tiene un link de suscripción para compartir.
        </p>
        <div className="row-gap">
          <button className="btn" onClick={async () => {
            const r = await run(() => api.syncPlans(), (x) =>
              `Sincronizado con Mercado Pago: ${x.imported} importados, ${x.updated} actualizados, ${x.deactivated} dados de baja`)
            if (r) refresh()
          }}>
            ⟳ Sincronizar con Mercado Pago
          </button>
          <button className="btn primary" onClick={openNew} disabled={!freeTier} title={freeTier ? '' : 'Los tres niveles ya tienen plan activo'}>
            + Nuevo plan
          </button>
        </div>
      </div>

      <div className="plan-grid">
        {TIERS.map((tier) => {
          const list = byTier(tier)
          const active = list.find((p) => p.active)
          const inactive = list.filter((p) => !p.active)
          return (
            <section key={tier} className="plan-col">
              <h3 className="plan-col-title">{tierLabel(tier)}</h3>
              {active ? (
                <PlanCard plan={active} onEdit={() => openEdit(active)} onToggle={async () => {
                  if (!confirm(`¿Dar de baja el plan "${active.name}"? Deja de aceptar altas; las suscripciones vigentes siguen.`)) return
                  await run(() => api.deactivatePlan(active.id), 'Plan dado de baja')
                  refresh()
                }} />
              ) : (
                <Empty>
                  Sin plan activo.
                  <button className="btn ghost sm" onClick={() => setEditing({ plan: null, input: { ...EMPTY, tier } })}>Crear plan {tierLabel(tier).split('· ')[1]}</button>
                </Empty>
              )}
              {inactive.map((p) => (
                <PlanCard key={p.id} plan={p} onEdit={() => openEdit(p)} onToggle={async () => {
                  if (await run(() => api.activatePlan(p.id), 'Plan reactivado')) refresh()
                }} />
              ))}
            </section>
          )
        })}
      </div>

      {editing && (
        <PlanForm
          initial={editing.input}
          isNew={!editing.plan}
          onClose={() => setEditing(null)}
          onSave={async (input) => {
            const saved = await run(
              () => (editing.plan ? api.updatePlan(editing.plan.id, input) : api.createPlan(input)),
              editing.plan ? 'Plan actualizado' : 'Plan creado y registrado en la pasarela',
            )
            if (saved) { setEditing(null); refresh() }
          }}
        />
      )}
    </div>
  )
}

function PlanCard({ plan, onEdit, onToggle }: { plan: Plan; onEdit: () => void; onToggle: () => void }) {
  const run = useRun()
  return (
    <article className={`plan-card ${plan.active ? '' : 'inactive'}`}>
      <header>
        <TierBadge tier={plan.tier} />
        {!plan.active && <span className="badge status-cancelled">Inactivo</span>}
      </header>
      <h4>{plan.name}</h4>
      {plan.description && <p className="muted">{plan.description}</p>}
      <p className="plan-price">{formatMoney(plan.price)}<span>/mes</span></p>
      <dl className="plan-facts">
        <div><dt>Horas incluidas</dt><dd>{formatHours(plan.monthlyHours)} / mes</dd></div>
        <div><dt>Permanencia mínima</dt><dd>{plan.minPermanenceMonths} meses</dd></div>
        <div><dt>Socios activos</dt><dd>{plan.activeSubscribers}</dd></div>
        <div><dt>ID en Mercado Pago</dt><dd className="mono" title={plan.gatewayPlanId ?? ''}>{plan.gatewayPlanId ? `…${plan.gatewayPlanId.slice(-10)}` : '—'}</dd></div>
      </dl>
      {plan.benefits.length > 0 && (
        <ul className="benefits">
          {plan.benefits.map((b) => <li key={b}>{b}</li>)}
        </ul>
      )}
      <footer className="plan-actions">
        <button className="btn sm" onClick={onEdit}>Editar</button>
        {plan.active && plan.gatewayInitPoint && (
          <button className="btn sm ghost" onClick={() => run(() => navigator.clipboard.writeText(plan.gatewayInitPoint!), 'Link del plan copiado')}>
            Copiar link
          </button>
        )}
        <button className={`btn sm ${plan.active ? 'danger-ghost' : 'ghost'}`} onClick={onToggle}>
          {plan.active ? 'Dar de baja' : 'Reactivar'}
        </button>
      </footer>
    </article>
  )
}

function PlanForm({ initial, isNew, onClose, onSave }: {
  initial: PlanInput
  isNew: boolean
  onClose: () => void
  onSave: (input: PlanInput) => void
}) {
  const [form, setForm] = useState(initial)
  const [benefits, setBenefits] = useState(initial.benefits.join('\n'))
  const set = <K extends keyof PlanInput>(k: K, v: PlanInput[K]) => setForm((f) => ({ ...f, [k]: v }))

  return (
    <Modal title={isNew ? 'Nuevo plan' : 'Editar plan'} onClose={onClose}>
      <form
        className="form"
        onSubmit={(e) => {
          e.preventDefault()
          onSave({ ...form, benefits: benefits.split('\n').map((b) => b.trim()).filter(Boolean) })
        }}
      >
        <label>Nombre<input required maxLength={120} value={form.name} onChange={(e) => set('name', e.target.value)} /></label>
        <label>Descripción<input maxLength={500} value={form.description} onChange={(e) => set('description', e.target.value)} /></label>
        <div className="form-row">
          <label>Nivel
            <select value={form.tier} onChange={(e) => set('tier', e.target.value as Tier)}>
              {TIERS.map((t) => <option key={t} value={t}>{tierLabel(t)}</option>)}
            </select>
          </label>
          <label>Precio mensual (ARS)
            <input type="number" required min={1} step="0.01" value={form.price || ''} onChange={(e) => set('price', Number(e.target.value))} />
          </label>
        </div>
        <div className="form-row">
          <label>Horas incluidas por mes
            <input type="number" required min={0} max={744} step="1" value={form.monthlyHours} onChange={(e) => set('monthlyHours', Number(e.target.value))} />
          </label>
          <label>Permanencia mínima (meses)
            <input type="number" required min={3} max={36} value={form.minPermanenceMonths} onChange={(e) => set('minPermanenceMonths', Number(e.target.value))} />
          </label>
        </div>
        <label>Beneficios <span className="muted">(uno por línea)</span>
          <textarea rows={4} value={benefits} onChange={(e) => setBenefits(e.target.value)} placeholder={'Acceso libre\nSala de reuniones\nCafé'} />
        </label>
        <footer className="form-actions">
          <button type="button" className="btn ghost" onClick={onClose}>Cancelar</button>
          <button type="submit" className="btn primary">{isNew ? 'Crear plan' : 'Guardar cambios'}</button>
        </footer>
      </form>
    </Modal>
  )
}
