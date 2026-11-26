import { createMap, showLocation } from './map.js';
const el = document.querySelector('#landingMap');
if (el) {
  let location = null;
  try {
    const session = await fetch('/api/auth/me', { credentials: 'same-origin' });
    if (session.ok) {
      const result = await fetch('/api/devices', { credentials: 'same-origin' });
      if (result.ok) {
        const data = await result.json();
        location = data.devices.map(device => device.location).filter(Boolean).sort((a,b) => new Date(b.recordedAt)-new Date(a.recordedAt))[0] || null;
      }
    }
  } catch { /* Keep the public map free of invented device markers. */ }
  const map = createMap(el, location);
  if (location) map.once('load', () => showLocation(map, null, location));
}
