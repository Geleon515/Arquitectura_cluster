import { useState, useCallback } from 'react'

let _setToast

export function useToast() {
  const show = useCallback((msg, tipo = 'ok') => {
    _setToast({ msg, tipo, key: Date.now() })
  }, [])
  return show
}

export function Toast() {
  const [state, setState] = useState(null)
  _setToast = setState

  if (!state) return null

  return (
    <div
      key={state.key}
      className={`toast show toast-${state.tipo}`}
    >
      {state.msg}
    </div>
  )
}
