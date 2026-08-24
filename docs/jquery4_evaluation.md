# jQuery 4 Upgrade Evaluation

**Date:** 2026-08-17 · **Verdict:** deferred — stay on jQuery 3.7.1 (slim).
**Closed on 2026-08-24:** obsolete. Summernote and jQuery were both removed from the application;
the rich-text editor is now Jodit 4, which has no dependencies. See
[Section 6](#6-outcome) and `docs/editor_evaluation.md`.

jQuery 4.0.0 was released on 2026-01-18 and is available as `org.webjars.npm:jquery:4.0.0`. The
bump was evaluated against this codebase and **rejected for now**: the blocker is not jQuery but
Summernote, which has not been released since October 2024 and does not support jQuery 4.

---

## 1. What breaks

Bumping `webjar-jquery.version` to `4.0.0` and leaving everything else alone breaks the rich-text
editor completely. Two independent causes, both in Summernote 0.9.1:

| # | Cause | Effect |
|---|---|---|
| 1 | `$.now()` was **removed** in jQuery 4. Summernote calls it in `Context._initialize()` — on every `.summernote()` call. | `TypeError: r(...).now is not a function`. The editor chrome half-renders (`.note-editable` exists) but never wires up, so the form submits an **empty description**. `VoluntaryFormE2ETest` fails. |
| 2 | `$.Deferred` was **dropped from the slim build** in jQuery 4 (4.0 slim also drops `callbacks` and `queue`; 3.x slim still shipped them). `base.qute.html` loads `jquery.slim.min.js`. | Summernote uses `$.Deferred` in the link, picture, help and video dialogs. Link and picture are both in our toolbar (`summernote-init.js`). |

Upstream tracking issue: [summernote#4787](https://github.com/summernote/summernote/issues/4787)
(opened February 2026) — open, no fix, no pull request, no assignee.

## 2. What does *not* break

Everything else in jQuery 4's breaking-change list is irrelevant here:

- **Our own JavaScript** (`summernote-init.js`) uses only `$(document).ready` and `$('#summernote')`.
  Both are unchanged in jQuery 4.
- **Summernote's `summernote-lite` bundle** uses none of the other removed APIs — checked for
  `$.type`, `$.isFunction`, `$.isArray`, `$.trim`, `$.parseJSON`, `$.isNumeric`, `$.camelCase` and
  `$.nodeName`. All ten `toggleClass` calls use the two-argument form, which survives; only the
  removed `toggleClass(Boolean)` signature would have been a problem.
- The widely reported `i.a.type is not a function` error comes from the Bootstrap builds of
  Summernote, not from `summernote-lite`, which is the build this project uses.

So `$.now` is the only removed API actually in play.

## 3. A workaround exists and is proven green

Switching from the slim to the full build and shimming the one missing function makes the upgrade
work:

```html
<!-- base.qute.html -->
<script src="/webjars/jquery/dist/jquery.min.js"></script>
```

```js
// summernote-init.js, before the .summernote() call
if (typeof $.now !== "function") { $.now = Date.now; }
```

Measured with this combination: **80 unit tests + all 7 e2e tests pass**, with no browser console
errors and no page errors. A probe of the link dialog, style dropdown and codeview was
inconclusive because the probe's own selectors were wrong — a **control run on 3.7.1 failed
identically**, confirming it was not a jQuery 4 regression.

**Cost:** jQuery 4 full is 27.4 KB gzipped against 24.0 KB for today's 3.7.1 slim — **+3.4 KB**. It
also pulls in the `ajax` and `effects` modules, which this application does not use.

| Build | raw | gzipped |
|---|---|---|
| 3.7.1 slim (current) | 70.3 KB | 24.0 KB |
| 4.0.0 slim | 56.0 KB | 19.4 KB |
| 4.0.0 full (needed for the workaround) | 78.7 KB | 27.4 KB |

## 4. Why it was deferred anyway

- jQuery 3.7.1 is still maintained, so there is **no security pressure** forcing the move.
- The shim is a monkey-patch on a combination upstream has explicitly *not* validated. Summernote
  could break in ways our tests do not reach.
- Summernote e2e coverage is a single "type some text" test (`VoluntaryFormE2ETest`). A latent
  break in the toolbar dialogs would ship unnoticed.
- The gain is 3.4 KB and Trusted Types support that this application does not currently use.

## 5. When to revisit

Any of these changes the calculation:

- Summernote ships a jQuery 4-compatible release, or moves off jQuery entirely
  ([summernote#4505](https://github.com/summernote/summernote/issues/4505)).
- jQuery 3.7.x stops receiving security fixes.
- The editor is replaced with a maintained alternative — a larger decision that would remove this
  constraint outright.

Until then, Dependabot will keep proposing the bump weekly. Consider an `ignore` entry for
`org.webjars.npm:jquery` version `4.x` in `.github/dependabot.yml`, referencing summernote#4787.

## References

- [jQuery Core 4.0 Upgrade Guide](https://jquery.com/upgrade-guide/4.0/)
- [summernote#4787 — Error after upgrading jQuery to 4.0](https://github.com/summernote/summernote/issues/4787)
- [summernote#4505 — Dropping jQuery support](https://github.com/summernote/summernote/issues/4505)

## 6. Outcome

Section 5 named "the editor is replaced with a maintained alternative" as the change that would
remove the constraint outright, and that is what happened a week later: Summernote failed the newly
recorded NFR-035 (an upstream release not older than 12 months) and was exchanged for Jodit 4, an
MIT-licensed editor without dependencies. The jQuery question disappeared with it — the application
now loads no jQuery at all, so neither the `$.now` shim nor the switch from the slim to the full
build is needed, and no `ignore` entry for `org.webjars.npm:jquery` has to be added to
`.github/dependabot.yml`.

The comparison of the candidates and the reasons for choosing Jodit are in
`docs/editor_evaluation.md`.
