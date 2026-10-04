const { test, expect } = require("@playwright/test");

test("jatos.js test study passes", async ({ page }) => {
    const studyUrl = process.env.JATOS_TEST_URL ||
        "http://localhost:9000/publix/iWlkb05ocEZ";
    const scriptFilename = process.env.JATOS_SCRIPT || "jatos.js";

    expect(["jatos.js", "jatos.min.js", "jatos-slim.js", "jatos-slim.min.js"]).toContain(scriptFilename);
    // Match either filename used by the study's components, including the first
    // component's minified script. Always select the requested build explicitly.
    await page.route(/\/jatos(?:-slim)?(?:\.min)?\.js(?:\?.*)?$/, async route => {
        const scriptUrl = new URL(route.request().url());
        scriptUrl.pathname = scriptUrl.pathname.replace(/[^/]+$/, scriptFilename);
        const response = await route.fetch({ url: scriptUrl.href });
        await route.fulfill({ response });
    });

    await page.addInitScript(({slim}) => {
        window.jatosPlaywrightRun = true;
        window.jatosExpectedSlim = slim;
    }, {slim: scriptFilename.startsWith("jatos-slim")});

    const pageErrors = [];
    page.on("pageerror", error => {
        pageErrors.push(error.stack || error.message);
    });

    const response = await page.goto(studyUrl, {
        waitUntil: "domcontentloaded"
    });

    expect(response, "The JATOS study URL did not return a document").not.toBeNull();
    expect(response.ok(), `Opening the study returned HTTP ${response.status()}`).toBe(true);

    await page.waitForFunction(
        () => window.jatosTestResult?.status === "finished",
        undefined,
        { timeout: 120_000 }
    );

    const result = await page.evaluate(() => window.jatosTestResult);

    printCheckResults(result);
    await test.info().attach("jatos.js checks", {
        body: JSON.stringify(result, null, 2),
        contentType: "application/json"
    });

    expect(pageErrors, "Uncaught errors occurred in the study page").toEqual([]);
    expect(result.failed, formatFailures(result.failed)).toEqual([]);
    expect(result.passed.length, "The study did not report any passing checks").toBeGreaterThan(0);

    console.log(
        `${result.passed.length} checks passed in ` +
        `${result.finishedAt - result.startedAt} ms`
    );
});

function printCheckResults(result) {
    console.log("\njatos.js checks:");
    for (const name of result.passed) {
        console.log(`  ✓ ${name}`);
    }
    for (const { name, detail } of result.failed) {
        console.log(`  ✗ ${name}${detail ? `: ${detail}` : ""}`);
    }
    console.log("");
}

function formatFailures(failures) {
    if (!failures || failures.length === 0) return "The study reported failed checks";

    return "The study reported failed checks:\n" + failures
        .map(({ name, detail }) => `- ${name}${detail ? `: ${detail}` : ""}`)
        .join("\n");
}
