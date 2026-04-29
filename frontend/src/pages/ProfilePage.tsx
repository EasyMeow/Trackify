export default function ProfilePage() {
  return (
    <div
      style={{
        padding: 'var(--space-6)',
        maxWidth: 600,
      }}
    >
      <h1
        style={{
          fontSize: 'var(--font-size-2xl)',
          fontWeight: 'var(--font-weight-semibold)',
          color: 'var(--color-text)',
          marginBottom: 'var(--space-2)',
        }}
      >
        Profile
      </h1>
      <p style={{ fontSize: 'var(--font-size-base)', color: 'var(--color-text-muted)' }}>
        Profile settings coming soon.
      </p>
    </div>
  );
}
