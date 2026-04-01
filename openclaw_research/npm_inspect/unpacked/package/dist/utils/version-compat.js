"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.VERSION_COMPAT_MAP = void 0;
exports.compareVersions = compareVersions;
exports.detectOpenclawVersion = detectOpenclawVersion;
exports.resolveOpenclawLarkVersion = resolveOpenclawLarkVersion;
exports.buildPackageIdentifier = buildPackageIdentifier;
exports.installPlugin = installPlugin;
const path = __importStar(require("path"));
const os = __importStar(require("os"));
const fs = __importStar(require("fs-extra"));
const chalk_1 = __importDefault(require("chalk"));
const system_1 = require("./system");
const config_1 = require("./config");
/**
 * Version compatibility mapping table.
 *
 * MUST be sorted by openclawLarkVersion in DESCENDING order (newest first).
 *
 * When adding a new openclaw-lark release:
 *   1. Add a new entry at the TOP of this array
 *   2. Set minOpenclawVersion to the minimum openclaw version it supports
 *   3. Optionally set maxOpenclawVersion if there's an upper bound
 *   4. Optionally add maxOpenclawVersion to the PREVIOUS entry if the new
 *      openclaw-lark version drops support for older openclaw versions
 *
 * 维护说明：
 *   - 每次 openclaw-lark 发布新版本时，在数组顶部添加新条目
 *   - minOpenclawVersion 为该 openclaw-lark 版本所需的最低 openclaw 版本
 *   - maxOpenclawVersion 可选，用于限定兼容的 openclaw 版本上限
 */
exports.VERSION_COMPAT_MAP = [
    { openclawLarkVersion: '2026.3.30', minOpenclawVersion: '2026.3.28' },
    { openclawLarkVersion: '2026.3.29', minOpenclawVersion: '2026.3.28' },
    { openclawLarkVersion: '2026.3.26', minOpenclawVersion: '2026.3.22' },
    { openclawLarkVersion: '2026.3.25', minOpenclawVersion: '2026.3.22' },
    { openclawLarkVersion: '2026.3.24', minOpenclawVersion: '2026.3.22' },
    { openclawLarkVersion: '2026.3.18', minOpenclawVersion: '2026.2.26', maxOpenclawVersion: '2026.3.13' },
    { openclawLarkVersion: '2026.3.17', minOpenclawVersion: '2026.2.26', maxOpenclawVersion: '2026.3.13' },
    { openclawLarkVersion: '2026.3.15', minOpenclawVersion: '2026.2.26', maxOpenclawVersion: '2026.3.13' },
    { openclawLarkVersion: '2026.3.12', minOpenclawVersion: '2026.2.26', maxOpenclawVersion: '2026.3.13' },
    { openclawLarkVersion: '2026.3.10', minOpenclawVersion: '2026.2.26', maxOpenclawVersion: '2026.3.13' },
    { openclawLarkVersion: '2026.3.9', minOpenclawVersion: '2026.2.26', maxOpenclawVersion: '2026.3.13' },
];
/**
 * Compare two version strings (e.g. "2026.3.13").
 * Returns 1 if v1 > v2, -1 if v1 < v2, 0 if equal.
 */
function compareVersions(v1, v2) {
    const parts1 = v1.split('.').map(Number);
    const parts2 = v2.split('.').map(Number);
    for (let i = 0; i < Math.max(parts1.length, parts2.length); i++) {
        const p1 = parts1[i] || 0;
        const p2 = parts2[i] || 0;
        if (p1 > p2)
            return 1;
        if (p1 < p2)
            return -1;
    }
    return 0;
}
/**
 * Detect the locally installed openclaw version.
 * Returns the version string (e.g. "2026.3.13") or null if detection fails.
 */
function detectOpenclawVersion() {
    try {
        const openclawCmd = (0, system_1.getPlatformCommand)('openclaw');
        const output = (0, system_1.runCommandQuiet)(openclawCmd, ['--version']);
        if (!output)
            return null;
        // openclaw --version may output warning lines before the actual version.
        // The real version is on the last non-empty line (e.g. "2026.2.26" or "OpenClaw 2026.3.22 (commit)").
        const lines = output.split('\n').map(l => l.trim()).filter(Boolean);
        const lastLine = lines[lines.length - 1];
        const versionMatch = lastLine ? lastLine.match(/(?:OpenClaw\s+)?(\d+\.\d+\.\d+)/) : null;
        return versionMatch ? versionMatch[1] ?? null : null;
    }
    catch {
        return null;
    }
}
/**
 * Resolve the best openclaw-lark version to install based on the local openclaw version.
 *
 * Resolution strategy:
 *   1. If the user explicitly specified a version, return it as-is (reason: 'exact').
 *   2. Detect the local openclaw version.
 *   3. Walk the compatibility table (newest first) to find the latest openclaw-lark
 *      version that is compatible with the local openclaw version.
 *   4. If the local openclaw version is newer than anything in the table,
 *      fall back to the latest openclaw-lark version (the first entry).
 *   5. If detection fails, fall back to the latest openclaw-lark version with a warning.
 *
 * @param userSpecifiedVersion - The version explicitly specified by the user (--version flag), or undefined
 * @param openclawVersion - Override for the local openclaw version (for testing). If not provided, auto-detected.
 */
function resolveOpenclawLarkVersion(userSpecifiedVersion, openclawVersion) {
    // 1. User explicitly specified a version — use it directly
    if (userSpecifiedVersion) {
        return {
            version: userSpecifiedVersion,
            reason: 'exact',
            message: `Using user-specified version: ${userSpecifiedVersion}`,
        };
    }
    // 2. No compatibility entries — fall back to latest (no version pin)
    if (exports.VERSION_COMPAT_MAP.length === 0) {
        return {
            version: null,
            reason: 'fallback-latest',
            message: 'Version compatibility table is empty. Installing latest version.',
        };
    }
    // 3. Detect local openclaw version
    const localVersion = openclawVersion !== undefined ? openclawVersion : detectOpenclawVersion();
    if (!localVersion) {
        // Cannot detect openclaw version — fall back to latest in compat table
        const latestCompat = exports.VERSION_COMPAT_MAP[0].openclawLarkVersion;
        return {
            version: latestCompat,
            reason: 'detection-failed',
            message: `Could not detect OpenClaw version. Defaulting to latest compatible openclaw-lark version: ${latestCompat}`,
        };
    }
    // 4. Find the latest compatible openclaw-lark version
    for (const entry of exports.VERSION_COMPAT_MAP) {
        const meetsMin = compareVersions(localVersion, entry.minOpenclawVersion) >= 0;
        const meetsMax = entry.maxOpenclawVersion
            ? compareVersions(localVersion, entry.maxOpenclawVersion) <= 0
            : true;
        if (meetsMin && meetsMax) {
            return {
                version: entry.openclawLarkVersion,
                reason: 'compat',
                message: `OpenClaw ${localVersion} is compatible with openclaw-lark ${entry.openclawLarkVersion}`,
            };
        }
    }
    // 5. No compatible entry found
    //    If the local version is newer than all maxOpenclawVersion entries,
    //    it's likely a very new openclaw that the table hasn't been updated for yet.
    //    Fall back to the latest openclaw-lark version.
    const latestEntry = exports.VERSION_COMPAT_MAP[0];
    if (compareVersions(localVersion, latestEntry.minOpenclawVersion) >= 0) {
        return {
            version: latestEntry.openclawLarkVersion,
            reason: 'compat',
            message: `OpenClaw ${localVersion} is compatible with openclaw-lark ${latestEntry.openclawLarkVersion}`,
        };
    }
    // Local openclaw version is older than anything in the table
    const oldestEntry = exports.VERSION_COMPAT_MAP[exports.VERSION_COMPAT_MAP.length - 1];
    return {
        version: null,
        reason: 'no-match',
        message: `OpenClaw ${localVersion} is too old. The oldest supported openclaw-lark version ` +
            `(${oldestEntry.openclawLarkVersion}) requires OpenClaw >= ${oldestEntry.minOpenclawVersion}. ` +
            `Please upgrade OpenClaw first.`,
    };
}
/**
 * Build the npm package identifier for installation.
 * If version is null, returns the bare package name (installs latest from npm).
 */
function buildPackageIdentifier(packageName, version) {
    return version ? `${packageName}@${version}` : packageName;
}
/** OpenClaw version threshold for local pack install strategy */
const LOCAL_PACK_MIN_VERSION = '2026.3.22';
/**
 * Install a plugin via `openclaw plugins install`.
 *
 * For OpenClaw >= 2026.3.22, clawhub-based install is slow, so we first
 * `npm pack` the package to a local tarball, then install from the local file.
 * For older versions, we install directly from the npm registry.
 */
async function installPlugin(packageName, version) {
    const openclawCmd = (0, system_1.getPlatformCommand)('openclaw');
    const packageIdentifier = buildPackageIdentifier(packageName, version);
    const openclawVersion = detectOpenclawVersion();
    const useLocalPack = openclawVersion
        ? compareVersions(openclawVersion, LOCAL_PACK_MIN_VERSION) >= 0
        : false;
    if (useLocalPack) {
        console.log(chalk_1.default.blue(`Packing ${packageIdentifier} locally...`));
        const npmCmd = (0, system_1.getPlatformCommand)('npm');
        const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'openclaw-lark-'));
        try {
            const packOutput = (0, system_1.runCommandQuiet)(npmCmd, ['pack', packageIdentifier, '--pack-destination', tmpDir]);
            // npm pack outputs the tarball filename
            const tarballName = packOutput.split('\n').pop().trim();
            const tarballPath = path.join(tmpDir, tarballName);
            console.log(chalk_1.default.blue(`Installing plugin from local package...`));
            await (0, config_1.withCleanFeishuChannel)(() => {
                (0, system_1.runCommand)(openclawCmd, ['plugins', 'install', tarballPath]);
            });
        }
        finally {
            fs.removeSync(tmpDir);
        }
    }
    else {
        console.log(chalk_1.default.blue(`Installing plugin ${packageIdentifier}...`));
        await (0, config_1.withCleanFeishuChannel)(() => {
            (0, system_1.runCommand)(openclawCmd, ['plugins', 'install', packageIdentifier]);
        });
    }
}
//# sourceMappingURL=version-compat.js.map