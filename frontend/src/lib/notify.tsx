import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'

type Tone = 'ok' | 'error' | 'info'
interface Toast { id: number; tone: Tone; text: string }

const NotifyContext = createContext<(text: string, tone?: Tone) => void>(() => {})

export function NotifyProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const notify = useCallback((text: string, tone: Tone = 'ok') => {
    const id = Date.now() + Math.random()
    setToasts((t) => [...t, { id, tone, text }])
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 4500)
  }, [])

  return (
    <NotifyContext.Provider value={notify}>
      {children}
      <div className="toasts" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.tone}`}>{t.text}</div>
        ))}
      </div>
    </NotifyContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotify() {
  return useContext(NotifyContext)
}

/** Ejecuta una accion async mostrando el error de la API como toast. */
// eslint-disable-next-line react-refresh/only-export-components
export function useRun() {
  const notify = useNotify()
  return useCallback(async <T,>(action: () => Promise<T>, okMessage?: string | ((r: T) => string)) => {
    try {
      const result = await action()
      if (okMessage) notify(typeof okMessage === 'function' ? okMessage(result) : okMessage)
      return result
    } catch (e) {
      notify(e instanceof Error ? e.message : String(e), 'error')
      return undefined
    }
  }, [notify])
}
