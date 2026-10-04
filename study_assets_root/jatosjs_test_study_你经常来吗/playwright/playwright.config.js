const { existsSync } = require("node:fs");
const { defineConfig, devices, firefox, webkit } = require("@playwright/test");

module.exports = defineConfig({
    testDir: "./tests",
    timeout: 150_000,
    expect: {
        timeout: 5_000
    },
    fullyParallel: false,
    workers: 1,
    retries: 0,
    reporter: [
        ["list"],
        ["html", { open: "never" }]
    ],
    use: {
        trace: "retain-on-failure",
        screenshot: "only-on-failure"
    },
    projects: [
        {
            name: "chromium",
            use: { ...devices["Desktop Chrome"] }
        },
        ...(existsSync(firefox.executablePath()) ? [{
            name: "firefox", use: {...devices["Desktop Firefox"]}
        }] : []),
        ...(existsSync(webkit.executablePath()) ? [{
            name: "webkit", use: {...devices["Desktop Safari"]}
        }] : [])
    ]
});
