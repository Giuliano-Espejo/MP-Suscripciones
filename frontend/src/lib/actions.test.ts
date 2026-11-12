import { describe, expect, it } from 'vitest'
import type { Subscription } from '../api/types'
import { availableActions } from './actions'

function sub(overrides: Partial<Subscription>): Subscription {
  return {
    id: 1, memberId: 1, memberName: 'Ana', memberEmail: 'ana@example.com', planId: 1, planName: 'Plus',
    tier: 'PLUS', tierLevel: 2, status: 'ACTIVE', amount: 7500, basePrice: 10000, promoActive: true,
    promotionName: 'Lanzamiento', promoEndsOn: null, startDate: '2026-11-11', minEndDate: '2027-02-11',
    permanenceFulfilled: false, scheduledCancelDate: null, cancelledAt: null, gatewayId: 'pre-1',
    initPoint: 'https://mp/checkout', hoursIncluded: 10, hoursUsed: 0, hoursAvailable: 10,
    createdAt: '2026-11-11T12:00:00Z', ...overrides,
  }
}

describe('availableActions', () => {
  it('pendiente: link de pago, simulacion en demo y baja', () => {
    expect(availableActions(sub({ status: 'PENDING', startDate: null }), 'demo'))
      .toEqual(['copyLink', 'simulate', 'cancel'])
  })

  it('pendiente con Mercado Pago real no ofrece simular', () => {
    expect(availableActions(sub({ status: 'PENDING' }), 'mercadopago')).toEqual(['copyLink', 'cancel'])
  })

  it('activa: consumos, cambio de plan, pausa y baja', () => {
    expect(availableActions(sub({ status: 'ACTIVE' }), 'demo'))
      .toEqual(['consumptions', 'changePlan', 'pause', 'cancel'])
  })

  it('activa con baja programada ofrece deshacerla', () => {
    expect(availableActions(sub({ status: 'ACTIVE', scheduledCancelDate: '2027-02-11' }), 'demo'))
      .toEqual(['consumptions', 'changePlan', 'pause', 'undoCancel'])
  })

  it('pausada: consumos, cambio de plan, reanudar y baja', () => {
    expect(availableActions(sub({ status: 'PAUSED' }), 'demo'))
      .toEqual(['consumptions', 'changePlan', 'resume', 'cancel'])
  })

  it('dada de baja no tiene acciones', () => {
    expect(availableActions(sub({ status: 'CANCELLED' }), 'demo')).toEqual([])
  })
})
