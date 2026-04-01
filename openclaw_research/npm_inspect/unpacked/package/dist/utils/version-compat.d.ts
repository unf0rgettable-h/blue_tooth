/**
 * Version compatibility entry.
 * Each entry declares the minimum openclaw version required by a specific openclaw-lark version.
 */
interface CompatEntry {
    /** @larksuite/openclaw-lark version */
    openclawLarkVersion: string;
    /** Minimum openclaw version required (inclusive) */
    minOpenclawVersion: string;
    /** Maximum openclaw version supported (inclusive). If omitted, no upper bound. */
    maxOpenclawVersion?: string;
}
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
export declare const VERSION_COMPAT_MAP: CompatEntry[];
/**
 * Compare two version strings (e.g. "2026.3.13").
 * Returns 1 if v1 > v2, -1 if v1 < v2, 0 if equal.
 */
export declare function compareVersions(v1: string, v2: string): number;
/**
 * Detect the locally installed openclaw version.
 * Returns the version string (e.g. "2026.3.13") or null if detection fails.
 */
export declare function detectOpenclawVersion(): string | null;
export interface VersionResolutionResult {
    /** The resolved openclaw-lark version to install, or null if no compatible version found */
    version: string | null;
    /** Reason for the resolution result */
    reason: 'exact' | 'compat' | 'fallback-latest' | 'no-match' | 'detection-failed';
    /** Human-readable message */
    message: string;
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
export declare function resolveOpenclawLarkVersion(userSpecifiedVersion?: string, openclawVersion?: string | null): VersionResolutionResult;
/**
 * Build the npm package identifier for installation.
 * If version is null, returns the bare package name (installs latest from npm).
 */
export declare function buildPackageIdentifier(packageName: string, version: string | null): string;
/**
 * Install a plugin via `openclaw plugins install`.
 *
 * For OpenClaw >= 2026.3.22, clawhub-based install is slow, so we first
 * `npm pack` the package to a local tarball, then install from the local file.
 * For older versions, we install directly from the npm registry.
 */
export declare function installPlugin(packageName: string, version: string | null): Promise<void>;
export {};
//# sourceMappingURL=version-compat.d.ts.map