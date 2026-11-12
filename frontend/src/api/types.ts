// Tipos espejo de la API del backend (Spring Boot).

export type Tier = 'BASICO' | 'PLUS' | 'PREMIUM'
export type SubscriptionStatus = 'PENDING' | 'ACTIVE' | 'PAUSED' | 'CANCELLED'
export type GatewayMode = 'demo' | 'mercadopago'

export const TIERS: Tier[] = ['BASICO', 'PLUS', 'PREMIUM']

export interface Plan {
  id: number
  name: string
  description: string | null
  tier: Tier
  tierLevel: number
  price: number
  currency: string
  monthlyHours: number
  minPermanenceMonths: number
  benefits: string[]
  active: boolean
  gatewayPlanId: string | null
  gatewayInitPoint: string | null
  activeSubscribers: number
}

export interface PlanInput {
  name: string
  description: string
  tier: Tier
  price: number
  monthlyHours: number
  minPermanenceMonths: number
  benefits: string[]
}

export interface Subscription {
  id: number
  memberId: number
  memberName: string
  memberEmail: string
  planId: number
  planName: string
  tier: Tier
  tierLevel: number
  status: SubscriptionStatus
  amount: number
  basePrice: number
  promoActive: boolean
  promotionName: string | null
  promoEndsOn: string | null
  startDate: string | null
  minEndDate: string | null
  permanenceFulfilled: boolean
  scheduledCancelDate: string | null
  cancelledAt: string | null
  gatewayId: string | null
  initPoint: string | null
  hoursIncluded: number
  hoursUsed: number
  hoursAvailable: number
  createdAt: string
}

export interface CancelResult {
  immediate: boolean
  effectiveDate: string
  subscription: Subscription
}

export interface ChangePlanResult {
  change: 'UPGRADE' | 'DOWNGRADE' | 'SAME'
  subscription: Subscription
}

export interface ConsumptionItem {
  id: number
  date: string
  hours: number
  description: string | null
}

export interface ConsumptionSummary {
  subscriptionId: number
  periodFrom: string | null
  periodTo: string | null
  hoursIncluded: number
  hoursUsed: number
  hoursAvailable: number
  benefits: string[]
  items: ConsumptionItem[]
}

export interface Promotion {
  id: number
  name: string
  validFrom: string
  validTo: string
  discountPercent: number
  durationMonths: number
  active: boolean
}

export type PromotionInput = Omit<Promotion, 'id'>

export interface DashboardSummary {
  activeMembers: number
  pending: number
  paused: number
  cancelled: number
  scheduledCancellations: number
  onPromotion: number
  monthlyRevenue: number
  activeByTier: Record<Tier, number>
  currentPromotion: Promotion | null
  gatewayMode: GatewayMode
  today: string
}
