import { useState } from 'react'
import { Sidebar }    from './components/Sidebar'
import { Topbar }     from './components/Topbar'
import { PageVentas } from './components/PageVentas'
import { PageMirror } from './components/PageMirror'
import { Toast, useToast } from './components/Toast'

const TITLES = {
  ventas: 'Gestion de Ventas',
  mirror: 'Capa Mirror — Replicacion',
}

export default function App() {
  const [page,  setPage]  = useState('ventas')
  const [stats, setStats] = useState({})
  const toast = useToast()

  return (
    <>
      <Sidebar page={page} setPage={setPage} />
      <div id="main">
        <Topbar title={TITLES[page]} stats={stats} />
        {page === 'ventas' && <PageVentas setStats={setStats} toast={toast} />}
        {page === 'mirror' && <PageMirror toast={toast} />}
      </div>
      <Toast />
    </>
  )
}
