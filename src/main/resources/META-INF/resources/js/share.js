// UC-009 share control; see EventResource/detail.html. At most 2 KB (NFR-041).
(function () {
    var box = document.querySelector('[data-share]');
    if (!box) return;
    var button = box.querySelector('button');
    var status = box.querySelector('[role=status]');
    var address = box.querySelector('.share__address');
    var input = box.querySelector('input');
    var timer;
    // UC-009 BR-003: the origin the browser loaded, the path from the Short Name.
    var url = location.origin + box.dataset.path;
    input.value = url;
    address.hidden = true;
    button.hidden = false;

    function byHand() {
        status.textContent = '';
        address.hidden = false;
        box.querySelector('.share__hint').hidden = false;
        input.focus();
        input.select();
    }

    // UC-009 BR-002: the clipboard receives the address alone.
    function copy() {
        if (!navigator.clipboard) return byHand();
        navigator.clipboard.writeText(url).then(function () {
            // UC-009 BR-006: a copy is confirmed, and the note goes away on its own.
            status.textContent = 'Link kopiert';
            clearTimeout(timer);
            timer = setTimeout(function () { status.textContent = ''; }, 5000);
        }, byHand);
    }

    button.addEventListener('click', function () {
        // UC-009 BR-001: what the browser offers decides, not the device.
        if (!navigator.share) return copy();
        navigator.share({ title: box.dataset.title, text: box.dataset.text, url: url })
            .catch(function (e) {
                // UC-009 BR-006: a cancelled Share Dialog is answered with nothing.
                if (e.name !== 'AbortError') copy();
            });
    });
})();
