import * as maplibregl from 'https://unpkg.com/maplibre-gl@6.19.0/dist/maplibre-gl.mjs';

const DEFAULT_CENTER=[39.6461,-0.4532];
const OSM_TILES='https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const styles={
 dark:{label:'3D Dark',url:'https://tiles.openfreemap.org/styles/dark'},
 street:{label:'Street Map',style:{version:8,name:'Street Map',sources:{base:{type:'raster',tiles:[OSM_TILES],tileSize:256,attribution:'© OpenStreetMap contributors'}},layers:[{id:'base',type:'raster',source:'base',minzoom:0,maxzoom:24}]}},
 satellite:{label:'Satellite',style:{version:8,name:'Satellite',sources:{imagery:{type:'raster',tiles:['https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'],tileSize:256,maxzoom:17,attribution:'Tiles © Esri — Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community'}},layers:[{id:'imagery',type:'raster',source:'imagery',minzoom:0,maxzoom:24}]}}
};

function addBuildings(map){
  if(!map.getSource('safeguard-buildings'))map.addSource('safeguard-buildings',{type:'vector',url:'https://tiles.openfreemap.org/planet'});
  if(map.getLayer('safeguard-3d-buildings'))return;
  const label=map.getStyle().layers?.find(layer=>layer.type==='symbol'&&layer.layout?.['text-field'])?.id;
  map.addLayer({id:'safeguard-3d-buildings',source:'safeguard-buildings','source-layer':'building',type:'fill-extrusion',minzoom:14,filter:['!=',['get','hide_3d'],true],paint:{'fill-extrusion-color':['interpolate',['linear'],['coalesce',['get','render_height'],0],0,'#263952',120,'#347181'],'fill-extrusion-height':['interpolate',['linear'],['zoom'],14,0,15,['coalesce',['get','render_height'],0]],'fill-extrusion-base':['coalesce',['get','render_min_height'],0],'fill-extrusion-opacity':0.82}},label);
}

export function createMap(container,initialLocation=null,{showStatus=true}={}){
  const center=initialLocation?[initialLocation.longitude,initialLocation.latitude]:DEFAULT_CENTER;
  let activeStyleKey='dark';
  const map=new maplibregl.Map({container,style:styles.dark.style||styles.dark.url,center,zoom:15,pitch:60,bearing:-20,maxPitch:75,maxZoom:22,canvasContextAttributes:{antialias:true}});
  map.getCanvasContainer().style.background='#111b2a';
  map.addControl(new maplibregl.NavigationControl({visualizePitch:true}),'top-right');
  map.on('style.load',()=>{map.resize();if(activeStyleKey==='dark')addBuildings(map)});
  const switcher=document.createElement('div');switcher.className='map-switcher';switcher.setAttribute('role','group');switcher.setAttribute('aria-label','Map style');
  const activateStyle=key=>switcher.querySelectorAll('button').forEach(button=>{const active=button.dataset.style===key;button.classList.toggle('active',active);button.setAttribute('aria-pressed',String(active))});
  let failed=false,usingFallback=false;
  for(const key of Object.keys(styles)){const button=document.createElement('button');button.type='button';button.dataset.style=key;button.textContent=styles[key].label;button.className=`map-style-button ${key==='dark'?'active':''}`;button.setAttribute('aria-pressed',String(key==='dark'));button.addEventListener('click',()=>{activeStyleKey=key;activateStyle(key);usingFallback=false;failed=false;map.setStyle(styles[key].style||styles[key].url);requestAnimationFrame(()=>map.resize())});switcher.append(button)}
  container.append(switcher);
  const status=document.createElement('div');status.className='map-status';status.textContent=initialLocation?'Showing the latest GPS report.':'Waiting for the first GPS report';if(showStatus)container.append(status);
  if(initialLocation)updateLocationStatus(container,`Last GPS report · ${new Date(initialLocation.recordedAt).toLocaleString()}`);
  map.on('error',event=>{if(activeStyleKey==='dark'&&!usingFallback&&event.error&&event.error.status){usingFallback=true;activeStyleKey='street';activateStyle('street');map.setStyle(styles.street.style||styles.street.url);requestAnimationFrame(()=>map.resize());return}if(!failed){failed=true;if(showStatus)status.textContent='Map tiles are unavailable. Location reports remain saved; try another map style.'}});
  requestAnimationFrame(()=>map.resize());
  return map;
}

export function showLocation(map,previousMarker,location,label='Last reported GPS location'){
  if(!location||!Number.isFinite(location.latitude)||!Number.isFinite(location.longitude))return previousMarker;
  previousMarker?.remove();const element=document.createElement('div');element.className='gps-pulse';element.setAttribute('aria-label',label);element.title=label;
  const marker=new maplibregl.Marker({element,anchor:'center'}).setLngLat([location.longitude,location.latitude]).setPopup(new maplibregl.Popup({offset:16}).setText(`${label} · ±${Math.round(location.accuracy||0)} m`)).addTo(map);
  map.flyTo({center:[location.longitude,location.latitude],zoom:Math.max(map.getZoom(),16),duration:500});return marker;
}

export function updateLocationStatus(container,message){const status=container?.querySelector('.map-status');if(status)status.textContent=message}
