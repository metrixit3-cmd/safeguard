document.querySelector('.menu')?.addEventListener('click', () => {
  const nav = document.querySelector('.navlinks');
  if (!nav) return;
  const open = nav.dataset.open === 'true';
  nav.dataset.open = String(!open);
  if (open) { nav.style.cssText = ''; return; }
  Object.assign(nav.style, { display: 'grid', position: 'absolute', top: '60px', left: '15px', right: '15px', padding: '18px', background: '#111b2b', border: '1px solid rgba(255,255,255,.1)', borderRadius: '12px', zIndex: '5' });
});
