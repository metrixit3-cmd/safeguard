import * as maplibregl from 'https://unpkg.com/maplibre-gl@6.19.0/dist/maplibre-gl.mjs';

const DEFAULT_CENTER = [39.6461, -0.4532];
const LIGHT_STYLE = 'https://tiles.openfreemap.org/styles/positron';
const GRAY_BASE_TILES = 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}';
const GRAY_REFERENCE_TILES = 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Reference/MapServer/tile/{z}/{y}/{x}';
const SATELLITE_TILES = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}';
const TRANSPORTATION_TILES = 'https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}';
const LABEL_TILES = 'https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}';
const markers = new WeakMap();
const styles = {
  light3d: { label: '3D Light', detail: 'Light streets and 3D buildings', style: LIGHT_STYLE },
  street: {
    label: 'Street Map',
    detail: 'Light gray streets and labels',
    style: {
      version: 8,
      name: 'Street Map',
      sources: {
        base: { type: 'raster', tiles: [GRAY_BASE_TILES], tileSize: 256, maxzoom: 16, attribution: 'Tiles (c) Esri' },
        reference: { type: 'raster', tiles: [GRAY_REFERENCE_TILES], tileSize: 256, maxzoom: 16, attribution: 'Map labels (c) Esri' }
      },
      layers: [
        { id: 'base', type: 'raster', source: 'base', minzoom: 0, maxzoom: 24 },
        { id: 'reference', type: 'raster', source: 'reference', minzoom: 0, maxzoom: 24 }
      ]
    }
  },
  satellite: {
    label: 'Satellite Hybrid',
    detail: 'Imagery with roads and place labels',
    style: {
      version: 8,
      name: 'Satellite Hybrid',
      sources: {
        imagery: { type: 'raster', tiles: [SATELLITE_TILES], tileSize: 256, maxzoom: 17, attribution: 'Tiles (c) Esri - Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community' },
        transportation: { type: 'raster', tiles: [TRANSPORTATION_TILES], tileSize: 256, maxzoom: 19, attribution: 'Reference overlay (c) Esri' },
        labels: { type: 'raster', tiles: [LABEL_TILES], tileSize: 256, maxzoom: 19, attribution: 'Place labels (c) Esri' }
      },
      layers: [
        { id: 'imagery', type: 'raster', source: 'imagery', minzoom: 0, maxzoom: 24 },
        { id: 'transportation', type: 'raster', source: 'transportation', minzoom: 0, maxzoom: 24 },
        { id: 'labels', type: 'raster', source: 'labels', minzoom: 0, maxzoom: 24 }
      ]
    }
  }
};

function addBuildings(map) {
  if (!map.getSource('safeguard-buildings')) {
    map.addSource('safeguard-buildings', {
      type: 'vector',
      url: 'https://tiles.openfreemap.org/planet',
      attribution: 'OpenFreeMap - OpenMapTiles, data from OpenStreetMap'
    });
  }
  if (map.getLayer('safeguard-3d-buildings')) return;
  const label = map.getStyle().layers?.find(layer => layer.type === 'symbol' && layer.layout?.['text-field'])?.id;
  map.addLayer({
    id: 'safeguard-3d-buildings',
    source: 'safeguard-buildings',
    'source-layer': 'building',
    type: 'fill-extrusion',
    minzoom: 14,
    filter: ['!=', ['get', 'hide_3d'], true],
    paint: {
      'fill-extrusion-color': ['interpolate', ['linear'], ['coalesce', ['get', 'render_height'], 0], 0, '#e8e4dc', 120, '#c9c4ba'],
      'fill-extrusion-height': ['interpolate', ['linear'], ['zoom'], 14, 0, 15, ['coalesce', ['get', 'render_height'], 0]],
      'fill-extrusion-base': ['coalesce', ['get', 'render_min_height'], 0],
      'fill-extrusion-opacity': 0.88
    }
  }, label);
}

export function createMap(container, initialLocation = null, { showStatus = true } = {}) {
  const center = initialLocation ? [initialLocation.longitude, initialLocation.latitude] : DEFAULT_CENTER;
  let activeStyleKey = 'light3d';
  const map = new maplibregl.Map({
    container,
    style: styles.light3d.style,
    center,
    zoom: 15,
    pitch: 60,
    bearing: 0,
    maxPitch: 75,
    maxZoom: 22,
    attributionControl: { compact: true },
    canvasContextAttributes: { antialias: true }
  });
  map.getCanvasContainer().style.background = '#f4f3ef';
  map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), 'top-right');
  map.on('style.load', () => {
    map.resize();
    if (activeStyleKey === 'light3d') addBuildings(map);
  });

  const switcher = document.createElement('div');
  switcher.className = 'map-layer-control';
  const toggle = document.createElement('button');
  toggle.type = 'button';
  toggle.className = 'map-layers-button';
  toggle.textContent = 'Layers';
  toggle.setAttribute('aria-expanded', 'false');
  toggle.setAttribute('aria-haspopup', 'true');
  const menu = document.createElement('div');
  menu.className = 'map-layer-menu';
  menu.setAttribute('role', 'group');
  menu.setAttribute('aria-label', 'Choose map layer');
  menu.hidden = true;
  const setMenuOpen = open => {
    menu.hidden = !open;
    toggle.setAttribute('aria-expanded', String(open));
  };
  for (const key of Object.keys(styles)) {
    const option = document.createElement('button');
    option.type = 'button';
    option.className = 'map-layer-option';
    option.dataset.style = key;
    option.setAttribute('aria-pressed', String(key === 'light3d'));
    const title = document.createElement('span');
    const name = document.createElement('b');
    name.textContent = styles[key].label;
    const detail = document.createElement('small');
    detail.textContent = styles[key].detail;
    title.append(name, detail);
    const check = document.createElement('span');
    check.className = 'map-layer-check';
    check.setAttribute('aria-hidden', 'true');
    check.textContent = key === 'light3d' ? '✓' : '';
    option.replaceChildren(title, check);
    option.addEventListener('click', event => {
      event.stopPropagation();
      activeStyleKey = key;
      map.setStyle(styles[key].style);
      map.setPitch(key === 'light3d' ? 60 : 0);
      map.setBearing(0);
      menu.querySelectorAll('.map-layer-option').forEach(item => {
        const active = item.dataset.style === key;
        item.setAttribute('aria-pressed', String(active));
        item.querySelector('.map-layer-check').textContent = active ? '✓' : '';
      });
      requestAnimationFrame(() => map.resize());
      setMenuOpen(false);
    });
    menu.append(option);
  }
  toggle.addEventListener('click', event => {
    event.stopPropagation();
    setMenuOpen(menu.hidden);
  });
  menu.addEventListener('click', event => event.stopPropagation());
  switcher.append(toggle, menu);
  container.append(switcher);
  map.on('click', () => setMenuOpen(false));

  const status = document.createElement('div');
  status.className = 'map-status';
  status.textContent = initialLocation
    ? 'Showing the latest GPS report.'
    : 'Waiting for the first GPS report - showing the default map center';
  if (showStatus) container.append(status);
  if (initialLocation) updateLocationStatus(container, `Last GPS report - ${new Date(initialLocation.recordedAt).toLocaleString()}`);
  map.on('error', event => {
    if (!event.error?.status) return;
    if (showStatus) status.textContent = 'Map tiles are unavailable. Location reports remain saved; try another map style.';
  });

  const resizeObserver = new ResizeObserver(() => {
    if (container.clientWidth && container.clientHeight) map.resize();
  });
  resizeObserver.observe(container);
  map.on('remove', () => resizeObserver.disconnect());
  map.once('load', () => map.resize());
  requestAnimationFrame(() => map.resize());
  if (!initialLocation) {
    showLocation(map, null, {
      latitude: DEFAULT_CENTER[1],
      longitude: DEFAULT_CENTER[0],
      accuracy: 0
    }, 'Default map center - waiting for GPS', { flyTo: false });
  }
  return map;
}

export function showLocation(map, previousMarker, location, label = 'Last reported GPS location', { flyTo = true } = {}) {
  if (!location || !Number.isFinite(location.latitude) || !Number.isFinite(location.longitude)) return previousMarker;
  const existing = previousMarker || markers.get(map);
  existing?.remove();
  const element = document.createElement('div');
  element.className = 'gps-pulse';
  element.setAttribute('aria-label', label);
  element.title = label;
  const accuracy = Math.round(location.accuracy || 0);
  const popupText = accuracy ? `${label} - accuracy ${accuracy} m` : label;
  const marker = new maplibregl.Marker({ element, anchor: 'center' })
    .setLngLat([location.longitude, location.latitude])
    .setPopup(new maplibregl.Popup({ offset: 16 }).setText(popupText))
    .addTo(map);
  markers.set(map, marker);
  if (flyTo) map.flyTo({ center: [location.longitude, location.latitude], zoom: Math.max(map.getZoom(), 16), duration: 500 });
  return marker;
}

export function updateLocationStatus(container, message) {
  const status = container?.querySelector('.map-status');
  if (status) status.textContent = message;
}
