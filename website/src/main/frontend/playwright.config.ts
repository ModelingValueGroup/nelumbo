import { defineConfig, devices } from '@playwright/test';
import { readdirSync, statSync } from 'node:fs';
import { resolve }               from 'node:path';

const PORT:    number = 8899;
const LIBSDIR: string = resolve(__dirname, '../../../build/libs');

function serverJar(): string {
    let jars: string[] = [];
    try {
        jars = readdirSync(LIBSDIR)
                .filter((f: string): boolean => /^nelumbo-http-server-.*\.jar$/.test(f))
                .sort((a: string, b: string): number => statSync(resolve(LIBSDIR, b)).mtimeMs - statSync(resolve(LIBSDIR, a)).mtimeMs);
    } catch {
        jars = [];
    }
    if (jars.length === 0) {
        throw new Error('No nelumbo-http-server jar in ' + LIBSDIR + '. Build it first: ./gradlew :website:serverJar');
    }
    return resolve(LIBSDIR, jars[0]);
}

export default defineConfig({
    testDir: './e2e',
    timeout: 30_000,
    retries: process.env.CI ? 1 : 0,
    use:     {
        baseURL: 'http://localhost:' + PORT,
    },
    projects: [
        { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    ],
    webServer: {
        command:             'java -jar "' + serverJar() + '" --port ' + PORT + ' --no-gui',
        url:                 'http://localhost:' + PORT + '/health',
        reuseExistingServer: !process.env.CI,
        timeout:             60_000,
    },
});
