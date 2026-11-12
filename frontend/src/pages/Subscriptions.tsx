import { useState } from 'react'
import { api } from '../api/client'
import type { GatewayMode, Plan, Subscription, SubscriptionStatus } from '../api/types'
import { ConsumptionsModal } from '../components/ConsumptionsModal'
import { Empty, HoursBar, Modal, StatusBadge, TierBadge } from '../components/ui'
import { availableActions, type SubscriptionAction } from '../lib/actions'
import { formatDate, formatMoney, tierLabel } from '../lib/format'
import { useRun } from '../lib/notify'
import { useData } from '../lib/useData'

const FILTERS: { value: SubscriptionStatus | undefined; label: string }[] = [
  { value: undefined, label: 'Todas' },
  { value: 'ACTIVE', label: 'Activas' },
  { value: 'PENDING', label: 'Pendientes' },
  { value: 'PAUSED', label: 'Pausadas' },
  { value: 'CANCELLED', label: 'Bajas' },
]

const ACTION_LABEL: Record<SubscriptionAction, string> = {
  copyLink: 'Copiar link de pago',
  simulate: 'Simular pago',
  consumptions: 'Consumos',
  changePlan: 'Cambiar nivel',
  pause: 'Pausar',
  resume: 'Reanudar',
  cancel: 'Dar de baja',
  undoCancel: 'Deshacer baja',
}

export function Subscriptions({ mode, onChange }: { mode: GatewayMode; onChange: () => void }) {
  const [filter, setFilter] = useState<SubscriptionStatus | undefined>(undefined)
  const { data: subs, error, reload } = useData(() => api.subscriptions(filter), [filter])
  const { data: plans } = useData(() => api.plans(true), [])
  const [creating, setCreating] = useState(false)
  const [changing, setChanging] = useState<Subscription | null>(null)
  const [consuming, setConsuming] = useState<Subscription | null>(null)
  const run = useRun()

  const refresh = () => { void reload(); onChange() }

  async function act(sub: Subscription, action: SubscriptionAction) {
    switch (action) {
      case 'copyLink':
        await run(() => navigator.clipboard.writeText(sub.initPoint!), 'Link de pago copiado: envialo al socio')
        return
      case 'simulate':
        if (await run(() => api.simulate(sub.id, 'authorized'), 'Pago aprobado (simulado): suscripción activa')) refresh()
        return
      case 'consumptions':
        setConsuming(sub)
        return
      case 'changePlan':
        setChanging(sub)
        return
      case 'pause':
        if (await run(() => api.pause(sub.id), 'Suscripción pausada')) refresh()
        return
      case 'resume':
        if (await run(() => api.resume(sub.id), 'Suscripción reanudada')) refresh()
        return
      case 'undoCancel':
        if (await run(() => api.undoCancel(sub.id), 'Baja programada cancelada')) refresh()
        return
      case 'cancel': {
        const warning = sub.startDate && !sub.permanenceFulfilled
          ? `Permanencia mínima hasta el ${formatDate(sub.minEndDate)}. La baja quedará programada para esa fecha. ¿Continuar?`
          : `¿Dar de baja la suscripción de ${sub.memberName}?`
        if (!confirm(warning)) return
        const r = await run(() => api.cancel(sub.id), (x) =>
          x.immediate ? 'Suscripción dada de baja' : `Baja programada para el ${formatDate(x.effectiveDate)}`)
        if (r) refresh()
      }
    }
  }

  return (
    <div className="stack">
      <div className="row-between">
        <div className="chips" role="tablist">
          {FILTERS.map((f) => (
            <button key={f.label} role="tab" aria-selected={filter === f.value}
              className={`chip ${filter === f.value ? 'on' : ''}`} onClick={() => setFilter(f.value)}>
              {f.label}
            </button>
          ))}
        </div>
        <div className="row-gap">
          <button className="btn" onClick={async () => {
            const r = await run(() => api.syncSubscriptions(), (x) =>
              `Estado consultado en Mercado Pago: ${x.checked} suscripciones, ${x.changed} con cambios`)
            if (r) refresh()
          }}>
            ⟳ Sincronizar estado
          </button>
          <button className="btn primary" onClick={() => setCreating(true)} disabled={!plans?.length}>+ Nueva suscripción</button>
        </div>
      </div>

      {error && <p className="error-box">{error}</p>}
      {subs && subs.length === 0 && <Empty>No hay suscripciones con este filtro.</Empty>}
      {subs && subs.length > 0 && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Socio</th><th>Nivel</th><th>Estado</th><th className="num">Cuota</th>
                <th>Alta</th><th>Permanencia</th><th>Horas disponibles</th><th aria-label="Acciones" />
              </tr>
            </thead>
            <tbody>
              {subs.map((s) => (
                <tr key={s.id}>
                  <td>
                    <strong>{s.memberName}</strong>
                    <div className="muted small">{s.memberEmail}</div>
                  </td>
                  <td><TierBadge tier={s.tier} /><div className="muted small">{s.planName}</div></td>
                  <td>
                    <StatusBadge status={s.status} />
                    {s.scheduledCancelDate && s.status !== 'CANCELLED' && (
                      <div className="small warn-text">Baja el {formatDate(s.scheduledCancelDate)}</div>
                    )}
                    {s.cancelledAt && <div className="muted small">el {formatDate(s.cancelledAt)}</div>}
                  </td>
                  <td className="num">
                    {formatMoney(s.amount)}
                    {s.promoActive && (
                      <div className="small promo-text" title={s.promotionName ?? ''}>
                        Promo{s.promoEndsOn ? ` hasta ${formatDate(s.promoEndsOn)}` : ''}
                        <div className="muted">Lista {formatMoney(s.basePrice)}</div>
                      </div>
                    )}
                  </td>
                  <td>{formatDate(s.startDate)}</td>
                  <td>
                    {s.minEndDate
                      ? s.permanenceFulfilled
                        ? <span className="ok-text">Cumplida</span>
                        : <>Hasta {formatDate(s.minEndDate)}</>
                      : <span className="muted">—</span>}
                  </td>
                  <td>{s.status === 'ACTIVE' || s.status === 'PAUSED'
                    ? <HoursBar used={s.hoursUsed} included={s.hoursIncluded} />
                    : <span className="muted">—</span>}
                  </td>
                  <td className="actions">
                    <div className="actions-inner">
                      {availableActions(s, mode).map((a) => (
                        <button key={a} className={`btn xs ${a === 'cancel' ? 'danger-ghost' : a === 'simulate' ? 'accent' : 'ghost'}`}
                          onClick={() => act(s, a)}>
                          {ACTION_LABEL[a]}
                        </button>
                      ))}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {creating && plans && (
        <NewSubscription plans={plans} onClose={() => setCreating(false)} onCreated={() => { setCreating(false); refresh() }} />
      )}
      {changing && plans && (
        <ChangePlan sub={changing} plans={plans} onClose={() => setChanging(null)} onDone={() => { setChanging(null); refresh() }} />
      )}
      {consuming && (
        <ConsumptionsModal sub={consuming} onClose={() => { setConsuming(null); refresh() }} />
      )}
    </div>
  )
}

function NewSubscription({ plans, onClose, onCreated }: { plans: Plan[]; onClose: () => void; onCreated: () => void }) {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [planId, setPlanId] = useState(plans[0]?.id ?? 0)
  const [created, setCreated] = useState<Subscription | null>(null)
  const run = useRun()

  if (created) {
    return (
      <Modal title="Suscripción creada" onClose={onCreated}>
        <div className="stack">
          <p>
            La suscripción de <strong>{created.memberName}</strong> quedó <strong>pendiente de pago</strong> por{' '}
            {formatMoney(created.amount)}/mes{created.promoActive ? ' (con promoción de lanzamiento)' : ''}.
            Enviá este link para que complete el pago en Mercado Pago:
          </p>
          <code className="link-box">{created.initPoint}</code>
          <footer className="form-actions">
            <button className="btn ghost" onClick={() => run(() => navigator.clipboard.writeText(created.initPoint ?? ''), 'Link copiado')}>Copiar link</button>
            <button className="btn primary" onClick={onCreated}>Listo</button>
          </footer>
        </div>
      </Modal>
    )
  }

  return (
    <Modal title="Nueva suscripción" onClose={onClose}>
      <form className="form" onSubmit={async (e) => {
        e.preventDefault()
        const sub = await run(() => api.createSubscription({ fullName, email, planId }))
        if (sub) setCreated(sub)
      }}>
        <label>Nombre y apellido<input required maxLength={150} value={fullName} onChange={(e) => setFullName(e.target.value)} /></label>
        <label>Email (el mismo de su cuenta de Mercado Pago)<input required type="email" value={email} onChange={(e) => setEmail(e.target.value)} /></label>
        <label>Plan
          <select value={planId} onChange={(e) => setPlanId(Number(e.target.value))}>
            {plans.map((p) => <option key={p.id} value={p.id}>{tierLabel(p.tier)} — {p.name} ({formatMoney(p.price)}/mes)</option>)}
          </select>
        </label>
        <p className="muted small">Se genera un link de pago de Mercado Pago. La suscripción se activa cuando el socio lo completa.</p>
        <footer className="form-actions">
          <button type="button" className="btn ghost" onClick={onClose}>Cancelar</button>
          <button type="submit" className="btn primary">Crear y generar link</button>
        </footer>
      </form>
    </Modal>
  )
}

function ChangePlan({ sub, plans, onClose, onDone }: { sub: Subscription; plans: Plan[]; onClose: () => void; onDone: () => void }) {
  const options = plans.filter((p) => p.id !== sub.planId)
  const [planId, setPlanId] = useState(options[0]?.id ?? 0)
  const run = useRun()
  const target = options.find((p) => p.id === planId)

  return (
    <Modal title={`Cambiar nivel · ${sub.memberName}`} onClose={onClose}>
      {options.length === 0 ? <Empty>No hay otros planes activos.</Empty> : (
        <form className="form" onSubmit={async (e) => {
          e.preventDefault()
          const r = await run(() => api.changePlan(sub.id, planId), (x) =>
            `${x.change === 'UPGRADE' ? 'Upgrade' : 'Downgrade'} a ${x.subscription.planName}: nueva cuota ${formatMoney(x.subscription.amount)}`)
          if (r) onDone()
        }}>
          <p>Nivel actual: <TierBadge tier={sub.tier} /> {sub.planName} · {formatMoney(sub.amount)}/mes</p>
          <label>Nuevo plan
            <select value={planId} onChange={(e) => setPlanId(Number(e.target.value))}>
              {options.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.tierLevel > sub.tierLevel ? '↑' : '↓'} {tierLabel(p.tier)} — {p.name} ({formatMoney(p.price)})
                </option>
              ))}
            </select>
          </label>
          {target && (
            <p className="muted small">
              {sub.promoActive ? 'Se mantiene el descuento promocional sobre el nuevo precio. ' : ''}
              La fecha de alta y la permanencia mínima no se reinician. Mercado Pago cobrará el nuevo monto desde el próximo ciclo.
            </p>
          )}
          <footer className="form-actions">
            <button type="button" className="btn ghost" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn primary">Confirmar cambio</button>
          </footer>
        </form>
      )}
    </Modal>
  )
}
