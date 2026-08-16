// Initialises the rich-text editor on any page that has a #summernote field.
//
// Lives in its own file rather than inline in the form templates so that the Content-Security-Policy
// can keep script-src at 'self': inline scripts would force 'unsafe-inline', which would take away
// the second line of defence behind the server-side HTML sanitiser.
//
// "video" is deliberately absent from the toolbar: it inserts an iframe, which the sanitiser drops
// when the description is stored, so offering it would only produce content that silently vanishes.
$(document).ready(function () {
    var editor = $('#summernote');
    if (editor.length === 0) {
        return;
    }

    editor.summernote({
        height: 400,
        toolbar: [
            ["history", ["undo", "redo"]],
            ["style", ["style"]],
            ["font", ["bold", "italic", "underline", "fontname", "strikethrough", "superscript", "subscript", "clear"]],
            ["color", ["color"]],
            ["paragraph", ["ul", "ol", "paragraph", "height"]],
            ["table", ["table"]],
            ["insert", ["link", "picture"]],
            ["view", ["codeview"]]
        ]
    });
});
