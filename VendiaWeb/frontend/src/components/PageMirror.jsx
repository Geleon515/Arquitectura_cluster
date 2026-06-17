import { useState, useEffect, useCallback } from 'react'
import { mirrorApi } from '../api/ventas'

export function PageMirror({ toast }) {
  const [estado,   setEstado]   = useState(null)
  const [loading,  setLoading]  = useState(false)

  const verificar = useCallback(async () => {
    const d = await mirrorApi.estado()
    setEstado(d)
  }, [])

  useEffect(() => { verificar() }, [verificar])

  async function sincronizar() {
    setLoading(true)
    toast('Iniciando sincronizacion...', 'info')
    const d = await mirrorApi.sincronizar()
    setLoading(false)
    if (d.estado === 'OK') {
      toast(`OK — ${d.registrosLeidos} leidos, ${d.registrosCopiados} copiados`, 'ok')
      verificar()
    } else {
      toast('Error: ' + d.mensaje, 'error')
    }
  }

  const conectado = estado?.estado === 'CONECTADO'

  return (
    <div id="content">
      <div className="g2">

        <div className="card">
          <div className="card-title">Estado del Servidor Mirror</div>
          {estado ? (
            <>
              <div className={`mirror-pill ${conectado ? 'mirror-ok' : 'mirror-err'}`}>
                <div className={`dot ${conectado ? 'dot-ok' : 'dot-err'}`} />
                <div>
                  <div style={{ fontWeight: 700 }}>{conectado ? 'Conectado' : 'Desconectado'}</div>
                  <div style={{ fontSize: 12, marginTop: 2 }}>
                    {conectado ? `${estado.registrosEnMirror} registros en mirror` : estado.mensaje}
                  </div>
                </div>
              </div>
              <div style={{ marginTop: 12, fontSize: 12, color: '#475569', lineHeight: 1.8 }}>
                <strong style={{ color: '#64748b' }}>Servidor:</strong> {estado.servidorMirror}<br />
                <strong style={{ color: '#64748b' }}>Ultima verificacion:</strong> {estado.timestamp}
              </div>
            </>
          ) : (
            <p style={{ color: '#475569', fontSize: 13 }}>Verificando...</p>
          )}
          <button className="btn btn-ghost btn-full" style={{ marginTop: 14 }} onClick={verificar}>
            Verificar Estado
          </button>
        </div>

        <div className="card">
          <div className="card-title">Sincronizar con Mirror</div>
          <p style={{ fontSize: 13, color: '#64748b', lineHeight: 1.7, marginBottom: 16 }}>
            Copia todos los registros activos del servidor operacional al Mirror.
            Usa <code>UPSERT</code> para ser idempotente — se puede ejecutar varias veces sin duplicar datos.
          </p>
          <button
            className="btn btn-mirror btn-full"
            onClick={sincronizar}
            disabled={loading}
            style={{ opacity: loading ? 0.6 : 1 }}
          >
            {loading ? 'Sincronizando...' : 'Iniciar Sincronizacion'}
          </button>
        </div>
      </div>

      <div className="card">
        <div className="card-title">Como funciona la replicacion</div>
        <div className="g3">
          {[
            { n: '1', title: 'Lectura',      desc: 'Se conecta a la base de datos principal y obtiene todas las ventas activas (las que no estan canceladas). Trae el ID, vendedor, producto, fecha, monto y estado de cada una.' },
            { n: '2', title: 'Copia',       desc: 'Guarda esas ventas en la base de datos espejo. Si una venta ya existia, la actualiza en lugar de duplicarla — por eso se puede sincronizar varias veces sin problema.' },
            { n: '3', title: 'Resultado',   desc: 'Al terminar muestra cuantas ventas se leyeron y cuantas se copiaron, junto con la hora en que se hizo la sincronizacion.' },
          ].map(s => (
            <div key={s.n} className="step-card">
              <div className="step-num">{s.n}</div>
              <div className="step-title">{s.title}</div>
              <div className="step-desc">{s.desc}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
