import { useState } from 'react'

const SOAP_BASE = '/api/soap'

async function postJson(url, body = {}) {
  const r = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  return r.json()
}

function XmlViewer({ label, xml }) {
  if (!xml) return null
  return (
    <div className="xml-viewer">
      <div className="xml-label">{label}</div>
      <pre className="xml-code">{xml}</pre>
    </div>
  )
}

function ResultBanner({ data }) {
  if (!data) return null
  return (
    <div className={`soap-result ${data.exito ? 'soap-ok' : 'soap-err'}`}>
      <span className={`dot ${data.exito ? 'dot-ok' : 'dot-err'}`} />
      <span>{data.mensaje}</span>
    </div>
  )
}

/* ─── Operación 1: getVenta ───────────────────────────────────── */
function OpGetVenta({ toast }) {
  const [id, setId]       = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  async function ejecutar() {
    if (!id.trim()) { toast('Ingrese un ID de venta.', 'error'); return }
    setLoading(true)
    try {
      const d = await postJson(SOAP_BASE + '/getVenta', { idVenta: id.trim() })
      setResult(d)
      toast(d.mensaje, d.exito ? 'ok' : 'error')
    } catch { toast('Error de conexión.', 'error') }
    setLoading(false)
  }

  return (
    <div className="card">
      <div className="card-title">
        <span>⬛ getVenta</span>
        <span className="method m-post">SOAP</span>
      </div>
      <p className="soap-desc">Busca una venta por su ID en la base de datos.</p>
      <div className="fg">
        <label>ID Venta</label>
        <div className="row">
          <input
            type="text" placeholder="Ej: VTA-20260708-091400"
            value={id} onChange={e => setId(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && ejecutar()}
          />
          <button className="btn btn-soap" onClick={ejecutar} disabled={loading}>
            {loading ? 'Enviando...' : 'Ejecutar'}
          </button>
        </div>
      </div>
      <ResultBanner data={result} />
      {result?.venta && (
        <div className="soap-venta-detail">
          <div className="soap-field"><span>ID Venta:</span><strong>{result.venta.idVenta}</strong></div>
          <div className="soap-field"><span>Vendedor:</span><strong>{result.venta.idVendedor}</strong></div>
          <div className="soap-field"><span>Producto:</span><strong>{result.venta.idProducto}</strong></div>
          <div className="soap-field"><span>Fecha:</span><strong>{result.venta.fecha}</strong></div>
          <div className="soap-field"><span>Monto:</span><strong style={{color:'#818cf8'}}>S/. {result.venta.montoTotal.toFixed(2)}</strong></div>
          <div className="soap-field"><span>Estado:</span><strong>{result.venta.estado}</strong></div>
        </div>
      )}
      <XmlViewer label="SOAP Request" xml={result?.xmlRequest} />
      <XmlViewer label="SOAP Response" xml={result?.xmlResponse} />
    </div>
  )
}

/* ─── Operación 2: getAllVentas ────────────────────────────────── */
function OpGetAllVentas({ toast }) {
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  async function ejecutar() {
    setLoading(true)
    try {
      const d = await postJson(SOAP_BASE + '/getAllVentas')
      setResult(d)
      toast(d.mensaje, 'ok')
    } catch { toast('Error de conexión.', 'error') }
    setLoading(false)
  }

  return (
    <div className="card">
      <div className="card-title">
        <span>⬛ getAllVentas</span>
        <span className="method m-post">SOAP</span>
      </div>
      <p className="soap-desc">Lista todas las ventas registradas en la base de datos.</p>
      <button className="btn btn-soap btn-full" onClick={ejecutar} disabled={loading}>
        {loading ? 'Consultando...' : 'Ejecutar getAllVentas'}
      </button>
      <ResultBanner data={result} />
      {result?.ventas && result.ventas.length > 0 && (
        <div className="tbl-wrap" style={{marginTop: 12}}>
          <table>
            <thead>
              <tr>
                <th>ID Venta</th><th>Vendedor</th><th>Producto</th>
                <th>Fecha</th><th>Monto</th><th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {result.ventas.map(v => (
                <tr key={v.idVenta}>
                  <td><code style={{fontSize:11}}>{v.idVenta}</code></td>
                  <td style={{fontWeight:600, color:'#e2e8f0'}}>{v.idVendedor}</td>
                  <td style={{color:'#94a3b8'}}>{v.idProducto}</td>
                  <td style={{color:'#64748b'}}>{v.fecha}</td>
                  <td style={{fontWeight:700, color:'#818cf8'}}>S/. {v.montoTotal.toFixed(2)}</td>
                  <td><span className={`badge badge-${v.estado}`}>{v.estado === 'P' ? 'Pendiente' : v.estado === 'E' ? 'Enviado' : v.estado}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <XmlViewer label="SOAP Request" xml={result?.xmlRequest} />
      <XmlViewer label="SOAP Response" xml={result?.xmlResponse} />
    </div>
  )
}

/* ─── Operación 3: registrarVenta ─────────────────────────────── */
function OpRegistrarVenta({ toast }) {
  const [vendedor, setVendedor] = useState('')
  const [producto, setProducto] = useState('')
  const [monto,    setMonto]    = useState('')
  const [result,   setResult]   = useState(null)
  const [loading,  setLoading]  = useState(false)

  async function ejecutar() {
    if (!vendedor.trim()) { toast('Ingrese el ID del vendedor.', 'error'); return }
    const m = parseFloat(monto)
    if (!m || m <= 0) { toast('Ingrese un monto válido.', 'error'); return }

    setLoading(true)
    try {
      const d = await postJson(SOAP_BASE + '/registrarVenta', {
        idVendedor: vendedor.trim(),
        idProducto: producto.trim() || 'P001',
        montoTotal: m,
      })
      setResult(d)
      toast(d.mensaje + (d.idVenta ? ` · ${d.idVenta}` : ''), d.exito ? 'ok' : 'error')
      if (d.exito) { setVendedor(''); setProducto(''); setMonto('') }
    } catch { toast('Error de conexión.', 'error') }
    setLoading(false)
  }

  return (
    <div className="card">
      <div className="card-title">
        <span>⬛ registrarVenta</span>
        <span className="method m-post">SOAP</span>
      </div>
      <p className="soap-desc">Registra una nueva venta en el sistema a través del web service SOAP.</p>
      <div className="fg">
        <label>ID Vendedor</label>
        <input type="text" placeholder="Ej: VEN-001"
          value={vendedor} onChange={e => setVendedor(e.target.value)} />
      </div>
      <div className="fg">
        <label>ID Producto</label>
        <input type="text" placeholder="Ej: P001 (opcional)"
          value={producto} onChange={e => setProducto(e.target.value)} />
      </div>
      <div className="fg">
        <label>Monto Total (S/.)</label>
        <input type="number" placeholder="Ej: 250.50" min="0.01" step="0.01"
          value={monto} onChange={e => setMonto(e.target.value)} />
      </div>
      <button className="btn btn-soap btn-full" onClick={ejecutar} disabled={loading}>
        {loading ? 'Registrando...' : 'Ejecutar registrarVenta'}
      </button>
      <ResultBanner data={result} />
      {result?.idVenta && (
        <div className="soap-venta-detail" style={{marginTop: 10}}>
          <div className="soap-field"><span>ID Venta generado:</span><strong style={{color:'#34d399'}}>{result.idVenta}</strong></div>
        </div>
      )}
      <XmlViewer label="SOAP Request" xml={result?.xmlRequest} />
      <XmlViewer label="SOAP Response" xml={result?.xmlResponse} />
    </div>
  )
}

/* ─── Página principal ────────────────────────────────────────── */
export function PageSoap({ toast }) {
  return (
    <div id="content">
      {/* Header informativo */}
      <div className="card">
        <div className="card-title">
          <span>Web Service SOAP — Tester</span>
          <span className="tbadge tbadge-purple" style={{fontSize:11}}>WSDL: /ws/ventas.wsdl</span>
        </div>
        <p style={{fontSize:13, color:'#94a3b8', lineHeight: 1.7}}>
          Este panel permite probar las <strong style={{color:'#e2e8f0'}}>3 operaciones SOAP</strong> del
          web service de ventas. Cada operación muestra los mensajes XML que se envían y reciben,
          simulando lo que haría un cliente SOAP como <strong style={{color:'#e2e8f0'}}>SoapUI</strong>.
        </p>
        <div className="soap-endpoint-info">
          <div className="soap-info-item">
            <span className="soap-info-label">Endpoint:</span>
            <code>http://localhost:8080/ws</code>
          </div>
          <div className="soap-info-item">
            <span className="soap-info-label">Namespace:</span>
            <code>http://arqui.grupo5.web/soap/ventas</code>
          </div>
          <div className="soap-info-item">
            <span className="soap-info-label">Port Type:</span>
            <code>VentasPort</code>
          </div>
        </div>
      </div>

      {/* Grid de operaciones */}
      <div className="g2">
        <div>
          <OpGetVenta toast={toast} />
          <OpRegistrarVenta toast={toast} />
        </div>
        <div>
          <OpGetAllVentas toast={toast} />
        </div>
      </div>
    </div>
  )
}
