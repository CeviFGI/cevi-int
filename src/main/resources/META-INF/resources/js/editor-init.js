// Initialises the rich-text editor on the description field of the two administrator forms.
//
// Lives in its own file rather than inline in the form templates so that the Content-Security-Policy
// can keep script-src at 'self': inline scripts would force 'unsafe-inline', which would take away
// the second line of defence behind the server-side HTML sanitiser (NFR-024).
//
// The editor is Jodit, which has no dependencies — no jQuery is loaded anywhere in this
// application. Only the two form templates pull it in, so a visitor never downloads it (NFR-036).
//
// The button list is chosen so that every button produces markup the server-side allow-list keeps
// (FR-033, docs/entity_model.md). "video" and "file" are deliberately absent: they insert an iframe
// or a link to an upload endpoint that does not exist here, so offering them would only produce
// content that silently vanishes on save.
document.addEventListener('DOMContentLoaded', function () {
    var textarea = document.getElementById('description');
    if (textarea === null) {
        return;
    }

    var editor = Jodit.make(textarea, {
        language: 'de',
        height: 400,
        // Keeps the full button list visible instead of collapsing it into a "more" menu, so the
        // toolbar is the one the administrator learned regardless of window width.
        toolbarAdaptive: false,
        buttons: [
            'undo', 'redo', '|',
            'paragraph', '|',
            'bold', 'italic', 'underline', 'strikethrough', 'superscript', 'subscript', '|',
            'font', 'fontsize', 'brush', 'eraser', '|',
            'ul', 'ol', 'align', 'lineHeight', '|',
            'table', 'link', 'image', '|',
            'source'
        ],
        // No upload endpoint exists: a chosen picture is embedded as a data: URI, which is what the
        // sanitiser accepts for img src.
        uploader: {
            insertImageAsBase64URI: true
        },
        showXPathInStatusbar: false
    });

    // Jodit writes the edited value back into the textarea as the content changes; syncing once
    // more on submit makes sure the very last keystroke is part of the posted form.
    var form = textarea.form;
    if (form !== null) {
        form.addEventListener('submit', function () {
            textarea.value = editor.value;
        });
    }
});
