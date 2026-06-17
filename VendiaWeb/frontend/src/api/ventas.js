const BASE = '/api/ventas'
const MIR  = '/api/mirror'

async function json(url, opts = {}) {
  const r = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...opts,
  })
  return r.json()
}

export const ventasApi = {
  listar:        ()           => json(BASE),
  estadisticas:  ()           => json(BASE + '/estadisticas'),
  buscar:        (id)         => json(BASE + '/' + encodeURIComponent(id)),
  registrar:     (body)       => json(BASE + '/registrar', { method: 'POST', body: JSON.stringify(body) }),
  actualizarMonto: (id, monto) =>
    json(BASE + '/' + encodeURIComponent(id) + '/monto', { method: 'PUT', body: JSON.stringify({ monto }) }),
  eliminar:      (id)         => json(BASE + '/' + encodeURIComponent(id), { method: 'DELETE' }),
}

export const mirrorApi = {
  estado:       () => json(MIR + '/estado'),
  sincronizar:  () => json(MIR + '/sincronizar', { method: 'POST' }),
}
