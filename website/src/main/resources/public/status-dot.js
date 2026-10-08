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

// Server status dot: every <a class="status-dot"> on the page shows the /stats status as a coloured dot
// (ok / busy / overloaded, grey when /stats cannot be read) with a short tooltip, refreshed every 15 s while the
// page is visible.
(function () {
    var POLL_MS = 15000;
    var style   = document.createElement('style');
    style.textContent = '.status-dot{display:inline-block;width:8px;height:8px;border-radius:50%;'
                      + 'background:#5c6270;margin-left:6px;vertical-align:middle}'
                      + '.status-dot.ok{background:#5fb87a}'
                      + '.status-dot.busy{background:#e0a050}'
                      + '.status-dot.overloaded{background:#f1707b}';
    document.head.appendChild(style);

    // status null: unknown, the grey default
    function show(status, title) {
        var dots = document.querySelectorAll('.status-dot');
        for (var i = 0; i < dots.length; i++) {
            dots[i].classList.remove('ok', 'busy', 'overloaded');
            if (status) {
                dots[i].classList.add(status);
            }
            dots[i].title = title;
        }
    }

    function update() {
        if (document.hidden) {
            return;
        }
        fetch('/stats').then(function (response) {
            return response.json();
        }).then(function (stats) {
            show(stats.status, stats.sessions.open + ' sessions, CPU ' + Math.round(stats.cpu.load * 100) + '%');
        }).catch(function () {
            show(null, 'server unreachable');
        });
    }

    update();
    setInterval(update, POLL_MS);
    document.addEventListener('visibilitychange', update);
})();
