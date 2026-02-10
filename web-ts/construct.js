// This file is plain JS, not processed by Vite
setTimeout(function() {
  var status = document.getElementById('status');
  status.textContent += '\n3. External JS running';

  if (!window.editorReady) {
    status.textContent += '\n   Not ready, retrying...';
    setTimeout(arguments.callee, 100);
    return;
  }

  status.textContent += '\n4. Creating element...';
  var el = document.createElement('div');
  el.style.height = '200px';
  document.body.appendChild(el);

  status.textContent += '\n5. Constructing Editor...';
  try {
    var editor = new window.EditorClass({ container: el, theme: 'dark' });
    status.textContent += '\n6. Done!';
  } catch(e) {
    status.textContent += '\n6. Error: ' + e.message;
  }
}, 1000);
