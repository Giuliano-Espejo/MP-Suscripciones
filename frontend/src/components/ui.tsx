import { useEffect, type ReactNode } from 'react'
import type { SubscriptionStatus, Tier } from '../api/types'
import { formatHours, statusLabel, tierName } from '../lib/format'

export function StatusBadge({ status }: { status: SubscriptionStatus }) {
  return <span className={`badge status-${status.toLowerCase()}`}>{statusLabel(status)}</span>
}

export function TierBadge({ tier }: { tier: Tier }) {
  const level = tier === 'BASICO' ? 1 : tier === 'PLUS' ? 2 : 3
  return (
    <span className={`badge tier tier-${level}`} title={`Nivel ${level}`}>
      <span className="tier-dots" aria-hidden="true">
        {[1, 2, 3].map((n) => (
          <i key={n} className={n <= level ? 'on' : ''} />
        ))}
      </span>
      {tierName(tier)}
    </span>
  )
}

export function HoursBar({ used, included }: { used: number; included: number }) {
  const pct = included > 0 ? Math.min(100, (used / included) * 100) : 0
  const available = Math.max(0, included - used)
  return (
    <div className="hours">
      <div className="hours-track" role="progressbar" aria-valuemin={0} aria-valuemax={included} aria-valuenow={used}>
        <div className={`hours-fill ${pct >= 90 ? 'warn' : ''}`} style={{ width: `${pct}%` }} />
      </div>
      <span className="hours-label">
        {formatHours(available)} <span className="muted">libres de {formatHours(included)}</span>
      </span>
    </div>
  )
}

export function Modal({ title, onClose, children, wide = false }: {
  title: string
  onClose: () => void
  children: ReactNode
  wide?: boolean
}) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal ${wide ? 'wide' : ''}`} role="dialog" aria-modal="true" aria-label={title}>
        <header className="modal-head">
          <h2>{title}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Cerrar">×</button>
        </header>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  )
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>
}

export function Kpi({ label, value, hint, tone }: { label: string; value: ReactNode; hint?: ReactNode; tone?: string }) {
  return (
    <div className={`kpi ${tone ?? ''}`}>
      <span className="kpi-label">{label}</span>
      <span className="kpi-value">{value}</span>
      {hint && <span className="kpi-hint">{hint}</span>}
    </div>
  )
}
