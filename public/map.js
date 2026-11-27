import * as maplibregl from 'https://unpkg.com/maplibre-gl@6.19.0/dist/maplibre-gl.mjs';

const DEFAULT_CENTER = [39.6461, -0.4532];
const ESRI_IMAGERY = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';
const markers = new WeakMap();
const styles = {
  street: { label: 'OpenStreetMap', style: 'https://tiles.openfreemap.org/styles/liberty' },
  satellite: {
    label: 'Satellite',
    style: {
      version: 8,
      name: 'Esri World Imagery',
      sources: { imagery: { type: 'raster', tiles: [ESRI_IMAGERY], tileSize: 256, maxzoom: 17, attribution: 'Tiles ' + String.fromCharCode(169) + ' Esri - Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community' } },
      layers: [{ id: 'imagery', type: 'raster', source: 'imagery', minzoom: 0, maxzoom: 24 }]
    }
  }
};

function addBuildings(map) {
  if (!map.getSource('safeguard-buildings')) map.addSource('safeguard-buildings', { type: 'vector', url: 'https://tiles.openfreemap.org/planet', attribution: 'OpenFreeMap - OpenMapTiles, data from OpenStreetMap' });
  if (map.getLayer('safeguard-3d-buildings')) return;
  map.addLayer({
    id: 'safeguard-3d-buildings', source: 'safeguard-buildings', 'source-layer': 'building', type: 'fill-extrusion', minzoom: 14,
    filter: ['!=', ['get', 'hide_3d'], true],
    paint: {
      'fill-extrusion-color': ['interpolate', ['linear'], ['coalesce', ['get', 'render_height'], 0], 0, '#263952', 120, '#347181'],
      'fill-extrusion-height': ['interpolate', ['linear'], ['zoom'], 14, 0, 15, ['coalesce', ['get', 'render_height'], 0]],
      'fill-extrusion-base': ['coalesce', ['get', 'render_min_height'], 0], 'fill-extrusion-opacity': 0.82
    }
  });
}

export function createMap(container, initialLocation = null) {
  const center = initialLocation ? [initialLocation.longitude, initialLocation.latitude] : DEFAULT_CENTER;
  let activeStyleKey = 'street';
  const map = new maplibregl.Map({
    container, style: styles.street.style, center, zoom: 15, pitch: 60, bearing: -20, maxPitch: 75, maxZoom: 22,
    attributionControl: { compact: true }, canvasContextAttributes: { antialias: true }
  });
  map.getCanvasContainer().style.background = '#111b2a';
  map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), 'top-right');
  map.on('style.load', () => { map.resize(); if (activeStyleKey === 'street') addBuildings(map); });

  const switcher = document.createElement('div');
  switcher.className = 'map-layer-control';
  const toggle = document.createElement('button');
  toggle.type = 'button'; toggle.className = 'map-layers-button'; toggle.textContent = 'Layers';
  toggle.setAttribute('aria-expanded', 'false'); toggle.setAttribute('aria-haspopup', 'true');
  const menu = document.createElement('div');
  menu.className = 'map-layer-menu'; menu.setAttribute('role', 'group'); menu.setAttribute('aria-label', 'Choose map layer'); menu.hidden = true;
  const setMenuOpen = open => { menu.hidden = !open; toggle.setAttribute('aria-expanded', String(open)); };
  for (const key of Object.keys(styles)) {
    const option = document.createElement('button');
    option.type = 'button'; option.className = 'map-layer-option'; option.dataset.style = key;
    option.setAttribute('aria-pressed', String(key === 'street'));
    const detail = key === 'street' ? 'OpenFreeMap - 3D streets and buildings' : 'Esri World Imagery';
    option.innerHTML = `<span><b>${styles[key].label}</b><small>${detail}</small></span><span class="map-layer-check" aria-hidden="true">${key === 'street' ? '&#10003;' : ''}</span>`;
    option.addEventListener('click', event => {
      event.stopPropagation();
      if (activeStyleKey !== key) { activeStyleKey = key; map.setStyle(styles[key].style); }
      menu.querySelectorAll('.map-layer-option').forEach(item => {
        const active = item.dataset.style === key;
        item.setAttribute('aria-pressed', String(active)); item.querySelector('.map-layer-check').textContent = active ? String.fromCharCode(0x2713) : '';
      });
      requestAnimationFrame(() => { map.resize(); map.setPitch(60); }); setMenuOpen(false);
    });
    menu.append(option);
  }
  toggle.addEventListener('click', event => { event.stopPropagation(); setMenuOpen(menu.hidden); });
  menu.addEventListener('click', event => event.stopPropagation());
  switcher.append(toggle, menu); container.append(switcher);
  map.on('click', () => setMenuOpen(false));

  const resizeObserver = new ResizeObserver(() => { if (container.clientWidth && container.clientHeight) map.resize(); });
  resizeObserver.observe(container); map.on('remove', () => resizeObserver.disconnect());
  map.once('load', () => map.resize()); requestAnimationFrame(() => map.resize());
  return map;
}

export function showLocation(map, previousMarker, location, label = 'Last reported GPS location') {
  if (!location || !Number.isFinite(location.latitude) || !Number.isFinite(location.longitude)) return previousMarker;
  const existing = previousMarker || markers.get(map); existing?.remove();
  const element = document.createElement('div'); element.className = 'gps-pulse'; element.setAttribute('aria-label', label); element.title = label;
  const marker = new maplibregl.Marker({ element, anchor: 'center' }).setLngLat([location.longitude, location.latitude])
    .setPopup(new maplibregl.Popup({ offset: 16 }).setText(`${label} - accuracy ${Math.round(location.accuracy || 0)} m`)).addTo(map);
  markers.set(map, marker); map.flyTo({ center: [location.longitude, location.latitude], zoom: Math.max(map.getZoom(), 16), duration: 500 });
  return marker;
}

export function updateLocationStatus() { return false; }
