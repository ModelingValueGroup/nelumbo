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

// Site-wide light/dark theme, shared by every page. Loaded blocking in <head> so the first paint
// already has the right colors (no flash). Sets two attributes on <html>:
//   data-theme       the resolved theme, 'light' or 'dark' - the pages' CSS keys off this
//   data-theme-mode  the user's choice, 'auto', 'light' or 'dark' - picks the switch's icon
// The choice lives in localStorage; no stored choice means 'auto' = follow the OS preference.
// nelumbo-fields.ts watches data-theme to switch the Monaco editors along.
// Also, being the one script on every page: shift+click on the status dot ([data-status-link]) opens the
// internal /status.html page, which the site deliberately does not link visibly (a plain click does nothing).
(function () {
    var KEY   = 'nelumbo-theme';
    var NEXT  = { auto: 'light', light: 'dark', dark: 'auto' };
    var media = window.matchMedia('(prefers-color-scheme: light)');
    var root  = document.documentElement;

    function mode() {
        var stored = null;
        try { stored = localStorage.getItem(KEY); } catch (e) { /* storage disabled: stay on auto */ }
        return stored === 'light' || stored === 'dark' ? stored : 'auto';
    }

    function label() {
        var m    = mode();
        var text = 'Theme: ' + m + ' (switch to ' + NEXT[m] + ')';
        document.querySelectorAll('.theme-toggle').forEach(function (button) {
            button.setAttribute('aria-label', text);
            button.title = text;
        });
    }

    function apply() {
        var m = mode();
        root.setAttribute('data-theme-mode', m);
        root.setAttribute('data-theme', m === 'auto' ? (media.matches ? 'light' : 'dark') : m);
        label();
    }

    function cycle() {
        var next = NEXT[mode()];
        try {
            if (next === 'auto') { localStorage.removeItem(KEY); } else { localStorage.setItem(KEY, next); }
        } catch (e) { /* storage disabled: the switch cannot persist */ }
        apply();
    }

    apply();
    media.addEventListener('change', apply);
    document.addEventListener('DOMContentLoaded', label);
    document.addEventListener('click', function (e) {
        if (e.target.closest && e.target.closest('.theme-toggle')) { cycle(); }
        if (e.shiftKey && e.target.closest && e.target.closest('[data-status-link]')) {
            e.preventDefault();
            window.location.href = '/status.html';
        }
    });
})();
