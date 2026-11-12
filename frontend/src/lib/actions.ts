import type { GatewayMode, Subscription } from '../api/types'

export type SubscriptionAction =
  | 'copyLink'
  | 'simulate'
  | 'consumptions'
  | 'changePlan'
  | 'pause'
  | 'resume'
  | 'cancel'
  | 'undoCancel'

/** Acciones que el tablero ofrece para una suscripcion segun su estado. */
export function availableActions(sub: Subscription, mode: GatewayMode): SubscriptionAction[] {
  const cancelAction: SubscriptionAction = sub.scheduledCancelDate ? 'undoCancel' : 'cancel'
  switch (sub.status) {
    case 'PENDING': {
      const actions: SubscriptionAction[] = []
      if (sub.initPoint) actions.push('copyLink')
      if (mode === 'demo') actions.push('simulate')
      actions.push('cancel')
      return actions
    }
    case 'ACTIVE':
      return ['consumptions', 'changePlan', 'pause', cancelAction]
    case 'PAUSED':
      return ['consumptions', 'changePlan', 'resume', cancelAction]
    case 'CANCELLED':
      return []
  }
}
