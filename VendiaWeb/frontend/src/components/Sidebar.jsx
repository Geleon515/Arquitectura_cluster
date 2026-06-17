const ITEMS = [
  { id: 'ventas', label: 'Gestion de Ventas' },
  { id: 'mirror', label: 'Capa Mirror'        },
]

export function Sidebar({ page, setPage }) {
  return (
    <div id="sidebar">
      <div id="sidebar-logo">
        <h1>Vendia Web</h1>
        <p>LogiMarket Peru S.A.</p>
      </div>

      <nav>
        {ITEMS.map(item => (
          <button
            key={item.id}
            className={`nav-item${page === item.id ? ' active' : ''}`}
            onClick={() => setPage(item.id)}
          >
            <span className="nav-dot" />
            {item.label}
          </button>
        ))}
      </nav>

    </div>
  )
}
