// Site-wide light/dark theme, shared by every page. Loaded blocking in <head> so the first paint
// already has the right colors (no flash). Sets two attributes on <html>:
//   data-theme       the resolved theme, 'light' or 'dark' - the pages' CSS keys off this
//   data-theme-mode  the user's choice, 'auto', 'light' or 'dark' - picks the switch's icon
// The choice lives in localStorage; no stored choice means 'auto' = follow the OS preference.
// nelumbo-fields.ts watches data-theme to switch the Monaco editors along.
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
    });
})();
