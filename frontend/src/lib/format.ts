import type { SubscriptionStatus, Tier } from '../api/types'

const money = new Intl.NumberFormat('es-AR', {
  style: 'currency',
  currency: 'ARS',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
})

const decimal = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2 })

export function formatMoney(value: number): string {
  return money.format(value)
}

export function formatHours(value: number): string {
  return `${decimal.format(value)} h`
}

/** "2027-02-01" -> "01/02/2027", sin pasar por Date para evitar corrimientos de zona horaria. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const [y, m, d] = iso.slice(0, 10).split('-')
  return `${d}/${m}/${y}`
}

export function daysBetween(fromIso: string, toIso: string): number {
  const toUtc = (iso: string) => {
    const [y, m, d] = iso.slice(0, 10).split('-').map(Number)
    return Date.UTC(y, m - 1, d)
  }
  return Math.round((toUtc(toIso) - toUtc(fromIso)) / 86_400_000)
}

const STATUS_LABELS: Record<SubscriptionStatus, string> = {
  PENDING: 'Pendiente de pago',
  ACTIVE: 'Activa',
  PAUSED: 'Pausada',
  CANCELLED: 'Baja',
}

export function statusLabel(status: SubscriptionStatus): string {
  return STATUS_LABELS[status]
}

const TIER_INFO: Record<Tier, { level: number; name: string }> = {
  BASICO: { level: 1, name: 'Básico' },
  PLUS: { level: 2, name: 'Plus' },
  PREMIUM: { level: 3, name: 'Premium' },
}

export function tierLabel(tier: Tier): string {
  const info = TIER_INFO[tier]
  return `Nivel ${info.level} · ${info.name}`
}

export function tierName(tier: Tier): string {
  return TIER_INFO[tier].name
}
