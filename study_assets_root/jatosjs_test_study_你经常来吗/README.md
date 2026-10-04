# jatos.js test study

This JATOS study exercises the public browser API provided by `jatos.js`. It is
intended for manual development checks and as the target of the Playwright smoke
test in [`playwright/`](playwright/).

## Study layout

- `tests.html` is the first component and runs the automated, non-navigating
  checks.
- `second.html` and `third.html` support manual component-navigation checks.
- `endPage1.html` supports study-ending checks.
- `media/` contains the image and video fixtures used by `tests.html`.
- `libs/` contains the study's own jQuery. This also verifies that the jQuery
  loaded by a study can coexist with `jatos.js`.
- `playwright/` contains the external browser runner. It is not executed by the
  study page.

The test study currently expects the study/component properties and UUIDs used
in `tests.html`. If the study is recreated instead of using the existing JATOS
configuration, update those expected values accordingly.

## Manual usage

1. Start the local JATOS instance containing this study.
2. Run the study through the JATOS GUI or a study link.
3. Wait for the checks rendered by `tests.html` to finish.
4. Confirm that every automatic check is marked `OK`.
5. Use the buttons at the top of the page to check component navigation,
   ending, aborting, and redirect behavior. These operations are intentionally
   manual because each one navigates away from or terminates the current run.

The page continues to render its human-readable report. It additionally exposes
the following machine-readable object for Playwright:

```js
window.jatosTestResult = {
    status: "running" | "finished",
    startedAt: 0,
    finishedAt: null,
    passed: [],
    failed: []
};
```

## Playwright usage

The runner defaults to the local General Multiple study link
`http://localhost:9000/publix/iWlkb05ocEZ`. The link must be able to start a
fresh study run each time it is opened.

The runner is already installed locally. To run it:

```bash
cd playwright
npm test
```

Run the same checks against `jatos.min.js`:

```bash
npm run test:min
```

To watch Chromium:

```bash
npm run test:headed
```

Override the URL when JATOS uses another host, port, or study code:

```bash
JATOS_TEST_URL='https://jatos.example/publix/other-code' npm test
```

The Playwright test opens the link, waits until `tests.html` reports
`status === "finished"`, and fails if the page reports any failed check or an
uncaught JavaScript error. It prints every individual check in the terminal and
attaches the complete JSON result to the Playwright HTML report. Open the latest
report with `npm run report`.

Before loading the study, Playwright sets `window.jatosPlaywrightRun`. After the
checks, `tests.html` uses `jatos.endStudyWithoutRedirect` to finalize the study
result: a passing run becomes `FINISHED`, while a run with failed checks becomes
`FAIL`. Manual runs do not set this flag, are not finalized automatically, and
keep the lifecycle buttons available for manual testing.

For a fresh installation of the runner dependencies:

```bash
cd playwright
npm install
npx playwright install chromium
```

More runner-specific details are in [`playwright/README.md`](playwright/README.md).

## Build and browser coverage

The runner explicitly selects the requested build even when a component loads
`jatos.min.js`. Use `npm test`, `npm run test:min`, `npm run test:slim`, and
`npm run test:slim:min` from `playwright/` to cover all four builds.
Standard builds must expose their own jQuery 3.7.1 with caching enabled; slim
builds must omit the `jatos.jQuery` property. Both preserve the study's globals.

Chromium is always configured. Firefox and WebKit are included automatically
when their Playwright executables are installed. Check the active projects with
`npx playwright test --list`. To enable the additional engines, run
`npx playwright install firefox webkit` (system dependencies may also be needed).
WebKit provides Safari-engine coverage, not testing in the Safari application.
All projects run sequentially to avoid concurrent edits to shared session data.
