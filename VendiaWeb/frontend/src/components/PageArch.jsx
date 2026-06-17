const LAYERS = [
  { color: '#818cf8', label: 'WEB',     title: 'Capa Web — Servidor de Aplicaciones',
    desc: 'VendiaWeb implementa MVC completo: Vista = React, Controlador = @RestController Spring Boot, Modelo = JdbcTemplate + MySQL.',
    chips: ['VendiaWeb', 'Spring Boot 3.2', 'REST API JSON', 'JdbcTemplate', 'Puerto 8080'] },
  { color: '#94a3b8', label: 'NAV.',    title: 'Navegador — Cliente Web',
    desc: 'Cualquier navegador accede al servidor de aplicaciones por HTTP. React (Vite) gestiona la interfaz del lado del cliente.',
    chips: ['HTML5 + CSS3', 'React 18', 'Vite', 'fetch API'] },
  { color: '#4ade80', label: 'DATOS',   title: 'Capa de Datos — Servidor de BD Operacional',
    desc: 'VendiaUpdater monitorea la carpeta compartida e inserta archivos .dat en MySQL. La BD es el estado autoritativo del sistema.',
    chips: ['VendiaUpdater', 'FolderWatcher', 'MySQL — logimarket', 'INSERT batch + ACK'] },
  { color: '#c084fc', label: 'MIRROR',  title: 'Capa Mirror — Servidor de Replica',
    desc: 'El servidor Mirror mantiene una copia sincronizada de los datos para tolerancia a fallos y alta disponibilidad.',
    chips: ['MirrorController', 'MySQL — logimarket_mirror', 'UPSERT idempotente'] },
  { color: '#fb923c', label: 'FTP',     title: 'Capa FTP — Transferencia de Archivos',
    desc: 'VendiaSender empaqueta ventas pendientes y las transfiere por FTP o carpeta compartida. El servidor responde con un .ack.',
    chips: ['VendiaSender', 'FtpSender (commons-net)', 'FileSender + ACK'] },
  { color: '#34d399', label: 'DW',      title: 'Capa DataWarehouse — Servidor Analitico',
    desc: 'GenerarDatawareHouse realiza el ETL desde la BD operacional al DW con esquema estrella para analisis OLAP.',
    chips: ['GenerarDatawareHouse', 'ETL Java', 'dim_vendedor', 'dim_fecha', 'fact_ventas'] },
  { color: '#f472b6', label: 'CLIENTE', title: 'Capa Cliente — Maquina del Cajero',
    desc: 'VendiaApp (JavaFX) corre en cada maquina cliente. Almacena en archivos binarios de tamano fijo hasta ser enviados.',
    chips: ['VendiaApp (JavaFX)', 'ventas.dat (130 bytes/reg)', 'ventas.idx', 'MVC local'] },
]

const ENDPOINTS = [
  { m: 'GET',    mc: 'm-get',    url: '/api/ventas',              desc: 'Lista todas las ventas activas' },
  { m: 'GET',    mc: 'm-get',    url: '/api/ventas/{id}',         desc: 'Busca una venta por ID' },
  { m: 'GET',    mc: 'm-get',    url: '/api/ventas/estadisticas', desc: 'Total y pendientes' },
  { m: 'POST',   mc: 'm-post',   url: '/api/ventas/registrar',    desc: 'Registra una nueva venta' },
  { m: 'PUT',    mc: 'm-put',    url: '/api/ventas/{id}/monto',   desc: 'Modifica el monto (solo estado P)' },
  { m: 'DELETE', mc: 'm-delete', url: '/api/ventas/{id}',         desc: 'Elimina logicamente (solo estado P)' },
  { m: 'POST',   mc: 'm-post',   url: '/api/mirror/sincronizar',  desc: 'Replica datos al servidor Mirror' },
  { m: 'GET',    mc: 'm-get',    url: '/api/mirror/estado',       desc: 'Verifica conectividad del Mirror' },
]

export function PageArch() {
  return (
    <div id="content">
      <div className="card">
        <div className="card-title">Modelo de N Capas</div>
        {LAYERS.map((layer, i) => (
          <div key={layer.label}>
            <div className="arch-layer">
              <div className="arch-stripe" style={{ background: layer.color }} />
              <div className="arch-body">
                <h4>{layer.title}</h4>
                <p>{layer.desc}</p>
                <div className="arch-chips">
                  {layer.chips.map(c => <span key={c} className="chip">{c}</span>)}
                </div>
              </div>
            </div>
            {i < LAYERS.length - 1 && <div className="arch-arrow">+</div>}
          </div>
        ))}
      </div>

      <div className="card">
        <div className="card-title">Endpoints REST disponibles</div>
        <div className="tbl-wrap">
          <table>
            <thead>
              <tr><th>Metodo</th><th>Endpoint</th><th>Descripcion</th></tr>
            </thead>
            <tbody>
              {ENDPOINTS.map(e => (
                <tr key={e.m + e.url}>
                  <td><span className={`method ${e.mc}`}>{e.m}</span></td>
                  <td><code>{e.url}</code></td>
                  <td style={{ color: '#94a3b8' }}>{e.desc}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
