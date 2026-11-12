import type {
  CancelResult,
  ChangePlanResult,
  ConsumptionSummary,
  DashboardSummary,
  Plan,
  PlanInput,
  Promotion,
  PromotionInput,
  Subscription,
  SubscriptionStatus,
} from './types'

export class ApiError extends Error {
  readonly status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(method: string, url: string, body?: unknown): Promise<T> {
  const res = await fetch(url, {
    method,
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    let message = `Error ${res.status}`
    try {
      const problem = await res.json()
      message = problem.detail ?? problem.title ?? message
    } catch {
      // respuesta sin cuerpo JSON
    }
    throw new ApiError(res.status, message)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const api = {
  summary: () => request<DashboardSummary>('GET', '/api/dashboard/summary'),
  runJobs: () => request<{ cancelled: number; promosExpired: number }>('POST', '/api/admin/jobs/run'),

  plans: (activeOnly = false) => request<Plan[]>('GET', `/api/plans${activeOnly ? '?activeOnly=true' : ''}`),
  createPlan: (input: PlanInput) => request<Plan>('POST', '/api/plans', input),
  updatePlan: (id: number, input: PlanInput) => request<Plan>('PUT', `/api/plans/${id}`, input),
  deactivatePlan: (id: number) => request<void>('DELETE', `/api/plans/${id}`),
  activatePlan: (id: number) => request<Plan>('POST', `/api/plans/${id}/activate`),
  syncPlans: () => request<{ imported: number; updated: number; deactivated: number }>('POST', '/api/plans/sync'),
  syncSubscriptions: () => request<{ checked: number; changed: number }>('POST', '/api/subscriptions/sync'),

  subscriptions: (status?: SubscriptionStatus) =>
    request<Subscription[]>('GET', `/api/subscriptions${status ? `?status=${status}` : ''}`),
  activeMembers: () => request<Subscription[]>('GET', '/api/members/active'),
  createSubscription: (input: { fullName: string; email: string; planId: number }) =>
    request<Subscription>('POST', '/api/subscriptions', input),
  pause: (id: number) => request<Subscription>('POST', `/api/subscriptions/${id}/pause`),
  resume: (id: number) => request<Subscription>('POST', `/api/subscriptions/${id}/resume`),
  cancel: (id: number) => request<CancelResult>('POST', `/api/subscriptions/${id}/cancel`),
  undoCancel: (id: number) => request<Subscription>('DELETE', `/api/subscriptions/${id}/cancel`),
  changePlan: (id: number, planId: number) =>
    request<ChangePlanResult>('PUT', `/api/subscriptions/${id}/plan`, { planId }),
  simulate: (id: number, status: 'authorized' | 'cancelled') =>
    request<Subscription>('POST', `/api/demo/subscriptions/${id}/simulate`, { status }),

  consumptions: (id: number) => request<ConsumptionSummary>('GET', `/api/subscriptions/${id}/consumptions`),
  addConsumption: (id: number, input: { hours: number; description: string; date?: string }) =>
    request<ConsumptionSummary>('POST', `/api/subscriptions/${id}/consumptions`, input),

  promotions: () => request<Promotion[]>('GET', '/api/promotions'),
  createPromotion: (input: PromotionInput) => request<Promotion>('POST', '/api/promotions', input),
  updatePromotion: (id: number, input: PromotionInput) => request<Promotion>('PUT', `/api/promotions/${id}`, input),
}
