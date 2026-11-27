export const adminPageHtml = `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="theme-color" content="#080d18">
  <title>SAFEGUARD Platform Center</title>
  <link rel="icon" href="/brand.svg" type="image/svg+xml">
  <link rel="stylesheet" href="/styles.css">
</head>
<body>
  <div class="mobile-head"><a class="brand" href="/"><img src="/brand.svg" alt=""><span>SAFEGUARD</span></a><button class="button small" data-admin-sign-out>Sign out</button></div>
  <div class="shell">
    <aside class="sidebar">
      <a class="brand" href="/"><img src="/brand.svg" alt=""><span>SAFEGUARD</span></a>
      <div><div class="side-label">Platform center</div><nav class="sidenav" id="adminNav">
        <a href="#overview" data-page="overview" class="active"><span>◫</span><span>Overview</span></a>
        <a href="#customers" data-page="customers"><span>♙</span><span>Customers</span></a>
        <a href="#devices" data-page="devices"><span>▣</span><span>Devices</span></a>
        <a href="#threats" data-page="threats"><span>⌁</span><span>Theft &amp; support</span></a>
        <a href="#audit" data-page="audit"><span>▤</span><span>Audit log</span></a>
      </nav></div>
      <div class="side-bottom"><div class="notice">Restricted workspace<br><span class="muted">Role checked on every request</span></div><p><a href="/" class="muted">← Public website</a></p><button type="button" class="button small" data-admin-sign-out>Sign out</button></div>
    </aside>
    <main class="shell-main">
      <header class="topbar"><h1 id="adminTitle">Platform overview</h1><div class="top-actions"><span class="chip"><i class="dot"></i> Admin session</span><button class="button small" data-admin-sign-out>Sign out</button></div></header>
      <div class="content" id="adminContent" aria-live="polite"><section class="card">Loading platform data…</section></div>
    </main>
  </div>
  <div id="adminDialog" class="auth-toast" role="dialog" aria-modal="true" hidden></div>
  <script type="module" src="/admin.js"></script>
</body>
</html>`;
