export function Topbar({ title, stats }) {
  return (
    <div id="topbar">
      <h2>{title}</h2>
      <div className="topbar-badges">
        {stats.total !== undefined && (
          <span className="tbadge tbadge-purple">{stats.total} ventas</span>
        )}
        {stats.pendientes !== undefined && (
          <span className="tbadge tbadge-green">{stats.pendientes} pendientes</span>
        )}
      </div>
    </div>
  )
}
