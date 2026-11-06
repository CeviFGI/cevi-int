// UC-010 A1 Live Results below the search field of base.qute.html. The form works without
// this file (BR-006); it only adds results while typing. Talks to its own origin only.
(function () {
    var form = document.querySelector('[data-search]');
    if (!form || !window.fetch || !window.DOMParser) return;
    var input = form.querySelector('input[name=q]');
    var box = form.querySelector('.live-results');
    var status = form.querySelector('[data-search-status]');
    var timer, asked = 0;

    // UC-010 A1/A3: two letters or digits, as on the Search Results Page.
    function term() {
        var v = input.value.trim();
        return Array.from(v.replace(/[^\p{L}\p{N}]/gu, '')).length >= 2 ? v : '';
    }

    function hide() {
        clearTimeout(timer);
        asked++;
        box.hidden = true;
        box.replaceChildren();
        status.textContent = '';
    }

    function links() {
        return Array.prototype.slice.call(box.querySelectorAll('a'));
    }

    // UC-010 A1 step 1: once the visitor pauses typing.
    function ask() {
        var q = term();
        if (!q) return hide();
        var mine = ++asked;
        fetch('/suche/live?q=' + encodeURIComponent(q), { headers: { Accept: 'text/html' } })
            .then(function (r) { return r.ok ? r.text() : Promise.reject(new Error('HTTP ' + r.status)); })
            .then(function (html) {
                // UC-010 A1 step 3: an answer to an older Search Term is dropped.
                if (mine !== asked) return;
                var doc = new DOMParser().parseFromString(html, 'text/html');
                box.replaceChildren(...doc.body.childNodes);
                box.hidden = false;
                // UC-010 A1 step 2: the number of Live Results, for assistive technology.
                var count = box.querySelector('[data-count]');
                status.textContent = count ? count.textContent : '';
            })
            .catch(hide);
    }

    input.addEventListener('input', function () {
        clearTimeout(timer);
        if (!term()) return hide();
        timer = setTimeout(ask, 250);
    });

    // UC-010 A1 step 3: leaving the search field and its results removes them.
    form.addEventListener('focusout', function (e) {
        if (!e.relatedTarget || !form.contains(e.relatedTarget)) hide();
    });

    // UC-010 A1 step 2: arrow keys move through the results, Escape closes them.
    form.addEventListener('keydown', function (e) {
        if (box.hidden) return;
        var all = links();
        var at = all.indexOf(document.activeElement);
        if (e.key === 'Escape') {
            hide();
            input.focus();
        } else if (e.key === 'ArrowDown' && all.length) {
            all[Math.min(at + 1, all.length - 1)].focus();
        } else if (e.key === 'ArrowUp' && at >= 0) {
            (at === 0 ? input : all[at - 1]).focus();
        } else {
            return;
        }
        e.preventDefault();
    });
})();
