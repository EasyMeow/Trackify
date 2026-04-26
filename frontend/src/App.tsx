function App() {
  return (
    <main
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 'var(--space-6)',
        textAlign: 'center',
        gap: 'var(--space-3)',
      }}
    >
      <span
        aria-hidden="true"
        style={{
          width: 32,
          height: 32,
          borderRadius: 'var(--radius-lg)',
          background: 'var(--color-accent)',
          boxShadow: 'var(--shadow-sm)',
          display: 'inline-block',
        }}
      />
      <h1>Trackify</h1>
      <p style={{ color: 'var(--color-text-muted)', maxWidth: 360 }}>
        Design tokens are loaded. Sign-in, dashboard, and project workspaces
        will land in upcoming tasks.
      </p>
    </main>
  )
}

export default App
