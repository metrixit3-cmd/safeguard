import * as maplibregl from 'https://unpkg.com/maplibre-gl@6.19.0/dist/maplibre-gl.mjs';

const DEFAULT_CENTER = [39.6461, -0.4532];
const ESRI_IMAGERY = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';
const markers = new WeakMap();

function rasterStyle(name, tiles, attribution, maxzoom = 19) {
  return {
    version: 8,
    name,
    sources: {
      base: { type: 'raster', tiles: [tiles], tileSize: 256, maxzoom, attribution }
    },
    layers: [{ id: 'base', type: 'raster', source: 'base', minzoom: 0, maxzoom: 24 }]
  };
}

const styles = {
  street: {
    label: 'Street Map',
    style: 'https://tiles.openfreemap.org/styles/liberty'
  },
  satellite: {
    label: 'Satellite',
    style: {
      version: 8,
      name: 'Esri World Imagery',
      sources: {
        imagery: {
          type: 'raster', tiles: [ESRI_IMAGERY], tileSize: 256, maxzoom: 17,
          attribution: 'Tiles © Esri — Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community'
        }
      },
      layers: [{ id: 'imagery', type: 'raster', source: 'imagery', minzoom: 0, maxzoom: 24 }]
    }
  }
};

function addBuildings(map) {
  if (!map.getSource('safeguard-buildings')) {
    map.addSource('safeguard-buildings', { type: 'vector', url: 'https://tiles.openfreemap.org/planet', attribution: 'OpenFreeMap © OpenMapTiles, data from OpenStreetMap' });
  }
  if (map.getLayer('safeguard-3d-buildings')) return;
  map.addLayer({
    id: 'safeguard-3d-buildings',
    source: 'safeguard-buildings',
    'source-layer': 'building',
    type: 'fill-extrusion',
    minzoom: 14,
    filter: ['!=', ['get', 'hide_3d'], true],
    paint: {
      'fill-extrusion-color': ['interpolate', ['linear'], ['coalesce', ['get', 'render_height'], 0], 0, '#263952', 120, '#347181'],
      'fill-extrusion-height': ['interpolate', ['linear'], ['zoom'], 14, 0, 15, ['coalesce', ['get', 'render_height'], 0]],
      'fill-extrusion-base': ['coalesce', ['get', 'render_min_height'], 0],
      'fill-extrusion-opacity': 0.82
    }
  });
}

export function createMap(container, initialLocation = null, { showStatus = true } = {}) {
  const center = initialLocation ? [initialLocation.longitude, initialLocation.latitude] : DEFAULT_CENTER;
  let activeStyleKey = 'street';
  const map = new maplibregl.Map({
    container,
    style: styles.street.style,
    center,
    zoom: 15,
    pitch: 60,
    bearing: -20,
    maxPitch: 75,
    maxZoom: 22,
    canvasContextAttributes: { antialias: true }
  });
  map.getCanvasContainer().style.background = '#111b2a';
  map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), 'top-right');

  map.on('style.load', () => {
    map.resize();
    if (activeStyleKey === 'street') addBuildings(map);
  });

  const switcher = document.createElement('div');
  switcher.className = 'map-switcher';
  switcher.setAttribute('role', 'group');
  switcher.setAttribute('aria-label', 'Map style');
  const activateStyle = key => switcher.querySelectorAll('button').forEach(button => {
    const active = button.dataset.style === key;
    button.classList.toggle('active', active);
    button.setAttribute('aria-pressed', String(active));
  });

  let failed = false;
  for (const key of Object.keys(styles)) {
    const button = document.createElement('button');
    button.type = 'button';
    button.dataset.style = key;
    button.textContent = styles[key].label;
    button.className = `map-style-button ${key === 'street' ? 'active' : ''}`;
    button.setAttribute('aria-pressed', String(key === 'street'));
    button.addEventListener('click', () => {
      activeStyleKey = key;
      failed = false;
      activateStyle(key);
      map.setStyle(styles[key].style);
      requestAnimationFrame(() => { map.resize(); map.setPitch(60); });
    });
    switcher.append(button);
  }
  container.append(switcher);

  const status = document.createElement('div');
  status.className = 'map-status';
  status.textContent = initialLocation ? 'Showing the latest GPS report.' : 'Waiting for the first GPS report';
  if (showStatus) container.append(status);
  if (initialLocation) updateLocationStatus(container, `Last GPS report · ${new Date(initialLocation.recordedAt).toLocaleString()}`);

  map.on('error', () => {
    if (!failed) {
      failed = true;
      if (showStatus) status.textContent = 'Map tiles are unavailable. Location reports remain saved; try another map style.';
    }
  });

  const resizeObserver = new ResizeObserver(() => {
    if (container.clientWidth && container.clientHeight) map.resize();
  });
  resizeObserver.observe(container);
  map.on('remove', () => resizeObserver.disconnect());
  map.once('load', () => map.resize());
  requestAnimationFrame(() => map.resize());
  return map;
}

export function showLocation(map, previousMarker, location, label = 'Last reported GPS location') {
  if (!location || !Number.isFinite(location.latitude) || !Number.isFinite(location.longitude)) return previousMarker;
  const existing = previousMarker || markers.get(map);
  existing?.remove();
  const element = document.createElement('div');
  element.className = 'gps-pulse';
  element.setAttribute('aria-label', label);
  element.title = label;
  const marker = new maplibregl.Marker({ element, anchor: 'center' })
    .setLngLat([location.longitude, location.latitude])
    .setPopup(new maplibregl.Popup({ offset: 16 }).setText(`${label} · ±${Math.round(location.accuracy || 0)} m`))
    .addTo(map);
  markers.set(map, marker);
  map.flyTo({ center: [location.longitude, location.latitude], zoom: Math.max(map.getZoom(), 16), duration: 500 });
  return marker;
}

export function updateLocationStatus(container, message) {
  const status = container?.querySelector('.map-status');
  if (status) status.textContent = message;
}
