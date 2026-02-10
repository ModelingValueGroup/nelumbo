//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
