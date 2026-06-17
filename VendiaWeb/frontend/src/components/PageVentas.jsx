import { useState, useEffect, useCallback } from 'react'
import { ventasApi } from '../api/ventas'

const ESTADO_TXT = { P: 'Pendiente', E: 'Enviado', X: 'Eliminado' }

function Badge({ estado }) {
  return <span className={`badge badge-${estado}`}>{ESTADO_TXT[estado] ?? estado}</span>
}

function EditPanel({ venta, onClose, onRefresh, toast }) {
  const [nuevoMonto, setNuevoMonto] = useState('')

  async function modificar() {
    const m = parseFloat(nuevoMonto)
    if (!m || m <= 0) { toast('Ingrese un monto valido.', 'error'); return }
    const d = await ventasApi.actualizarMonto(venta.idVenta, m)
    if (d.error) { toast(d.error, 'error'); return }
    toast(d.mensaje, 'ok')
    onClose(); onRefresh()
  }

  async function eliminar() {
    if (!confirm(`Eliminar la venta ${venta.idVenta}?`)) return
    const d = await ventasApi.eliminar(venta.idVenta)
    if (d.error) { toast(d.error, 'error'); return }
    toast(d.mensaje, 'ok')
    onClose(); onRefresh()
  }

  return (
    <div className="edit-panel">
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
        <code style={{ fontSize: 11, color: '#94a3b8' }}>{venta.idVenta}</code>
        <Badge estado={venta.estado} />
      </div>
      <p style={{ fontSize: 12, color: '#64748b', marginBottom: 12 }}>
        Vendedor: <strong style={{ color: '#e2e8f0' }}>{venta.idVendedor}</strong>
        &nbsp;·&nbsp;
        Monto actual: <strong style={{ color: '#818cf8' }}>S/. {venta.montoTotal.toFixed(2)}</strong>
      </p>
      <div className="fg">
        <label>Nuevo Monto (S/.)</label>
        <input
          type="number" placeholder="Ej: 200.00" min="0.01" step="0.01"
          value={nuevoMonto} onChange={e => setNuevoMonto(e.target.value)}
        />
      </div>
      <div className="row" style={{ marginTop: 8 }}>
        <button className="btn btn-warning" style={{ flex: 1 }} onClick={modificar}>Guardar</button>
        <button className="btn btn-danger"  style={{ flex: 1 }} onClick={eliminar}>Eliminar</button>
        <button className="btn btn-ghost btn-sm" onClick={onClose}>X</button>
      </div>
    </div>
  )
}

export function PageVentas({ setStats, toast }) {
  const [ventas,    setVentas]    = useState([])
  const [vendedor,  setVendedor]  = useState('')
  const [monto,     setMonto]     = useState('')
  const [buscarId,  setBuscarId]  = useState('')
  const [editVenta, setEditVenta] = useState(null)

  const cargar = useCallback(async () => {
    try {
      const [lista, stats] = await Promise.all([
        ventasApi.listar(),
        ventasApi.estadisticas(),
      ])
      setVentas(lista)
      setStats(stats)
    } catch {
      toast('No se pudo conectar con el servidor de BD.', 'error')
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  async function registrar() {
    if (!vendedor.trim()) { toast('Ingrese el ID del vendedor.', 'error'); return }
    const m = parseFloat(monto)
    if (!m || m <= 0) { toast('Ingrese un monto valido.', 'error'); return }
    const d = await ventasApi.registrar({ idVendedor: vendedor, monto: m })
    if (d.error) { toast(d.error, 'error'); return }
    toast(`${d.mensaje} · ${d.idVenta}`, 'ok')
    setVendedor(''); setMonto(''); cargar()
  }

  async function buscar() {
    if (!buscarId.trim()) { toast('Ingrese un ID.', 'info'); return }
    const d = await ventasApi.buscar(buscarId.trim())
    if (!d.idVenta) { toast('Venta no encontrada.', 'error'); return }
    setEditVenta(d)
    toast('Venta cargada para edicion.', 'info')
  }

  return (
    <div id="content">
      <div className="g2">

        {/* Registro */}
        <div className="card">
          <div className="card-title">Nueva Venta</div>
          <div className="fg">
            <label>ID Vendedor</label>
            <input
              type="text" placeholder="Ej: VEN-001"
              value={vendedor} onChange={e => setVendedor(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && registrar()}
            />
          </div>
          <div className="fg">
            <label>Monto (S/.)</label>
            <input
              type="number" placeholder="Ej: 125.50" min="0.01" step="0.01"
              value={monto} onChange={e => setMonto(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && registrar()}
            />
          </div>
          <button className="btn btn-primary btn-full" onClick={registrar}>
            Registrar Venta
          </button>
        </div>

        {/* Buscar / Editar */}
        <div className="card">
          <div className="card-title">Buscar / Editar / Eliminar</div>
          <div className="row">
            <input
              type="text" placeholder="ID de venta"
              value={buscarId} onChange={e => setBuscarId(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && buscar()}
            />
            <button className="btn btn-ghost" onClick={buscar}>Buscar</button>
          </div>
          {editVenta && (
            <EditPanel
              venta={editVenta}
              onClose={() => setEditVenta(null)}
              onRefresh={cargar}
              toast={toast}
            />
          )}
        </div>
      </div>

      {/* Tabla */}
      <div className="card">
        <div className="card-title">
          <span>Listado de Ventas — Servidor BD</span>
          <button className="btn btn-ghost btn-sm" onClick={cargar}>Actualizar</button>
        </div>
        <div className="tbl-wrap">
          <table>
            <thead>
              <tr>
                <th>ID Venta</th><th>Vendedor</th><th>Fecha</th>
                <th>Monto</th><th>Estado</th><th>Accion</th>
              </tr>
            </thead>
            <tbody>
              {ventas.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', color: '#475569', padding: 28 }}>
                    Sin registros en la base de datos
                  </td>
                </tr>
              ) : ventas.map(v => (
                <tr key={v.idVenta}>
                  <td><code style={{ fontSize: 11 }}>{v.idVenta}</code></td>
                  <td style={{ fontWeight: 600, color: '#e2e8f0' }}>{v.idVendedor}</td>
                  <td style={{ color: '#64748b' }}>{v.fecha}</td>
                  <td style={{ fontWeight: 700, color: '#818cf8' }}>S/. {v.montoTotal.toFixed(2)}</td>
                  <td><Badge estado={v.estado} /></td>
                  <td>
                    <button
                      className="btn btn-ghost btn-sm"
                      onClick={() => { setEditVenta(v); setBuscarId(v.idVenta) }}
                    >
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
