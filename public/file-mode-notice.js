if (location.protocol === 'file:') {
  const notice = document.createElement('aside');
  notice.className = 'file-mode-notice';
  notice.setAttribute('role', 'status');

  const title = document.createElement('strong');
  title.textContent = 'You opened a static file, not the SAFEGUARD app server.';
  const message = document.createElement('span');
  message.textContent = 'Sign-in, GPS, live maps and device controls need the backend server. In the project terminal, run npm start, then open ';
  const link = document.createElement('a');
  link.href = 'http://127.0.0.1:4173';
  link.textContent = 'http://127.0.0.1:4173';
  const ending = document.createElement('span');
  ending.textContent = '.';
  notice.append(title, message, link, ending);
  document.body.prepend(notice);

  const map = document.querySelector('#landingMap');
  if (map) {
    const mapNotice = document.createElement('div');
    mapNotice.className = 'map-file-notice';
    mapNotice.textContent = 'The interactive map loads when SAFEGUARD is opened through its server.';
    map.append(mapNotice);
  }
} else if (document.querySelector('#landingMap')) {
  import('./landing-map.js').catch(() => {
    const map = document.querySelector('#landingMap');
    if (map) {
      const mapNotice = document.createElement('div');
      mapNotice.className = 'map-file-notice';
      mapNotice.textContent = 'The map could not load. Check the server connection and map tile access, then refresh.';
      map.append(mapNotice);
    }
  });
}
