import { describe, expect, it } from 'vitest'
import { daysBetween, formatDate, formatHours, formatMoney, statusLabel, tierLabel } from './format'

const clean = (s: string) => s.replace(/\s/g, ' ')

describe('formatMoney', () => {
  it('formatea pesos sin decimales cuando son enteros', () => {
    expect(clean(formatMoney(7500))).toBe('$ 7.500')
  })

  it('muestra centavos cuando existen', () => {
    expect(clean(formatMoney(6749.99))).toBe('$ 6.749,99')
  })
})

describe('formatDate', () => {
  it('convierte ISO a dd/mm/aaaa sin corrimiento de zona horaria', () => {
    expect(formatDate('2027-02-01')).toBe('01/02/2027')
  })

  it('devuelve guion para fechas nulas', () => {
    expect(formatDate(null)).toBe('—')
  })
})

describe('daysBetween', () => {
  it('cuenta dias calendario entre dos fechas ISO', () => {
    expect(daysBetween('2026-11-11', '2027-02-11')).toBe(92)
  })

  it('es negativo si la fecha destino ya paso', () => {
    expect(daysBetween('2026-11-11', '2026-11-01')).toBe(-10)
  })
})

describe('formatHours', () => {
  it('omite decimales innecesarios', () => {
    expect(formatHours(6)).toBe('6 h')
    expect(formatHours(6.5)).toBe('6,5 h')
  })
})

describe('labels', () => {
  it('traduce estados', () => {
    expect(statusLabel('PENDING')).toBe('Pendiente de pago')
    expect(statusLabel('ACTIVE')).toBe('Activa')
    expect(statusLabel('PAUSED')).toBe('Pausada')
    expect(statusLabel('CANCELLED')).toBe('Baja')
  })

  it('traduce niveles con su numero', () => {
    expect(tierLabel('BASICO')).toBe('Nivel 1 · Básico')
    expect(tierLabel('PREMIUM')).toBe('Nivel 3 · Premium')
  })
})
