import { api } from '../api/client'
import { TIERS } from '../api/types'
import { Kpi } from '../components/ui'
import { formatDate, formatMoney, tierLabel } from '../lib/format'
import { useRun } from '../lib/notify'
import { useData } from '../lib/useData'

export function Overview({ refreshKey }: { refreshKey: number }) {
  const { data, error, reload } = useData(() => api.summary(), [refreshKey])
  const run = useRun()

  if (error) return <p className="error-box">{error}</p>
  if (!data) return <p className="muted">Cargando…</p>

  const maxTier = Math.max(1, ...TIERS.map((t) => data.activeByTier[t]))
  const promo = data.currentPromotion

  return (
    <div className="stack">
      {promo && (
        <section className="promo-banner">
          <div>
            <span className="eyebrow">Promoción vigente</span>
            <h3>{promo.name}</h3>
            <p>
              {promo.discountPercent}% off durante {promo.durationMonths} meses para altas del{' '}
              {formatDate(promo.validFrom)} al {formatDate(promo.validTo)}.
            </p>
          </div>
          <span className="promo-pct">-{promo.discountPercent}%</span>
        </section>
      )}

      <section className="kpis">
        <Kpi label="Socios activos" value={data.activeMembers} hint={`${data.onPromotion} con promo`} tone="accent" />
        <Kpi label="Ingreso mensual recurrente" value={formatMoney(data.monthlyRevenue)} hint="Suscripciones activas" />
        <Kpi label="Pendientes de pago" value={data.pending} hint="Esperando checkout" />
        <Kpi label="Bajas programadas" value={data.scheduledCancellations} hint="Por permanencia mínima" tone={data.scheduledCancellations ? 'warn' : ''} />
        <Kpi label="Pausadas" value={data.paused} />
        <Kpi label="Bajas" value={data.cancelled} />
      </section>

      <section className="card">
        <header className="card-head">
          <h3>Socios activos por nivel</h3>
        </header>
        <div className="tier-bars">
          {TIERS.map((t, i) => (
            <div key={t} className="tier-bar">
              <span className="tier-bar-label">{tierLabel(t)}</span>
              <div className="tier-bar-track">
                <div className={`tier-bar-fill tier-${i + 1}`} style={{ width: `${(data.activeByTier[t] / maxTier) * 100}%` }} />
              </div>
              <span className="tier-bar-value">{data.activeByTier[t]}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="card row-between">
        <div>
          <h3>Procesos diarios</h3>
          <p className="muted">
            Corren solos todos los días a las 03:00: ejecutan las bajas programadas que ya cumplieron la permanencia
            y vuelven al precio de lista las promociones vencidas. Hoy: {formatDate(data.today)}.
          </p>
        </div>
        <button
          className="btn"
          onClick={async () => {
            const r = await run(() => api.runJobs(), (x) => `Listo: ${x.cancelled} bajas, ${x.promosExpired} promos finalizadas`)
            if (r) void reload()
          }}
        >
          Ejecutar ahora
        </button>
      </section>
    </div>
  )
}
