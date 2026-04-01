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
exports.installCommand = installCommand;
const fs = __importStar(require("fs-extra"));
const path = __importStar(require("path"));
const chalk_1 = __importDefault(require("chalk"));
const system_1 = require("../utils/system");
const config_1 = require("../utils/config");
const install_prompts_1 = require("../utils/install-prompts");
const update_1 = require("./update");
const constants_1 = require("../utils/constants");
const migration_1 = require("../utils/migration");
const feishu_auth_1 = require("../utils/feishu-auth");
const version_compat_1 = require("../utils/version-compat");
const secret_ref_1 = require("../utils/secret-ref");
const tools_1 = require("../utils/tools");
const EXTENSIONS_DIR = (0, config_1.getExtensionsDir)();
const PLUGIN_NAME = constants_1.NEW_PLUGIN_ID;
const PLUGIN_PATH = path.join(EXTENSIONS_DIR, PLUGIN_NAME);
const CONFLICT_PLUGIN_PATH = path.join(EXTENSIONS_DIR, 'feishu');
const PACKAGE_NAME = constants_1.NEW_PLUGIN_PACKAGE;
/**
 * Validates CLI parameter combinations and returns errors/warnings.
 */
function validateSecretParams(options) {
    const warnings = [];
    // Mutual exclusion: --secret-env-var and --secret-file-path
    if (options.secretEnvVar && options.secretFilePath) {
        return { error: '--secret-env-var and --secret-file-path are mutually exclusive (互斥参数)', warnings };
    }
    // JSON Pointer format check
    if (options.secretFileId && !options.secretFileId.startsWith('/')) {
        return { error: '--secret-file-id must start with / (JSON Pointer format)', warnings };
    }
    // Mode mismatch warnings
    if (options.secretInputMode === 'plaintext') {
        if (options.secretFilePath)
            warnings.push('--secret-file-path is ignored in plaintext mode');
        if (options.secretFileId)
            warnings.push('--secret-file-id is ignored in plaintext mode');
        if (options.secretEnvVar)
            warnings.push('--secret-env-var is ignored in plaintext mode');
    }
    if (options.secretInputMode === 'env') {
        if (options.secretFilePath)
            warnings.push('--secret-file-path is ignored in env mode');
        if (options.secretFileId)
            warnings.push('--secret-file-id is ignored in env mode');
    }
    if (options.secretInputMode === 'file') {
        if (options.secretEnvVar)
            warnings.push('--secret-env-var is ignored in file mode');
    }
    return { warnings };
}
const SECRETREF_RANGE_MIN = '2026.3.2';
const SECRETREF_RANGE_MAX = '2026.3.11';
const SECRETREF_SKIP_VERSION = '2026.3.12';
const SECRETREF_RESUME_VERSION = '2026.3.13';
/**
 * Detects the installed openclaw version. Returns null if unavailable.
 */
function getOpenClawVersion() {
    try {
        const openclawCmd = (0, system_1.getPlatformCommand)('openclaw');
        const output = (0, system_1.runCommandQuiet)(openclawCmd, ['--version']);
        if (!output)
            return null;
        const lines = output.split('\n').map(l => l.trim()).filter(Boolean);
        const lastLine = lines[lines.length - 1];
        const match = lastLine ? lastLine.match(/(?:OpenClaw\s+)?(\d+\.\d+\.\d+)/) : null;
        return match ? match[1] : null;
    }
    catch {
        return null;
    }
}
/**
 * Determines the secret storage mode based on options and existing config.
 * Falls back to plaintext if openclaw version is outside 2026.3.2 ~ 2026.3.11 (no SecretRef support).
 */
function resolveSecretMode(options, existingAppSecret) {
    // 0. Check openclaw version — SecretRef supported in [2026.3.2, 2026.3.11] and >= 2026.3.13 (not 2026.3.12)
    const version = getOpenClawVersion();
    const inFirstRange = version
        ? (0, version_compat_1.compareVersions)(version, SECRETREF_RANGE_MIN) >= 0 && (0, version_compat_1.compareVersions)(version, SECRETREF_RANGE_MAX) <= 0
        : false;
    const inSecondRange = version
        ? (0, version_compat_1.compareVersions)(version, SECRETREF_RESUME_VERSION) >= 0
        : false;
    const supportsSecretRef = inFirstRange || inSecondRange;
    if (!supportsSecretRef) {
        if (options.secretInputMode && options.secretInputMode !== 'plaintext') {
            console.warn(chalk_1.default.yellow(`Warning: --secret-input-mode ${options.secretInputMode} requires OpenClaw ${SECRETREF_RANGE_MIN}~${SECRETREF_RANGE_MAX} or >=${SECRETREF_RESUME_VERSION}, falling back to plaintext`));
        }
        return 'plaintext';
    }
    const isWindows = process.platform === 'win32';
    // 1. User explicitly specified mode
    if (options.secretInputMode) {
        // Windows only supports env and plaintext for writing
        if (isWindows && options.secretInputMode === 'file') {
            console.warn(chalk_1.default.yellow('Warning: file mode is not supported on Windows, falling back to plaintext (Windows 不支持 file 模式，回退为明文)'));
            return 'plaintext';
        }
        return options.secretInputMode;
    }
    // 2. Infer from companion params
    if (options.secretFilePath || options.secretFileId) {
        if (isWindows) {
            console.warn(chalk_1.default.yellow('Warning: file mode is not supported on Windows, falling back to plaintext (Windows 不支持 file 模式，回退为明文)'));
            return 'plaintext';
        }
        return 'file';
    }
    if (options.secretEnvVar)
        return 'env';
    // 3. Existing config — preserve current mode
    if (existingAppSecret !== undefined && existingAppSecret !== '') {
        if (typeof existingAppSecret === 'string')
            return 'plaintext';
        if ((0, secret_ref_1.isSecretRef)(existingAppSecret)) {
            if (existingAppSecret.source === 'env')
                return 'env';
            if (existingAppSecret.source === 'file') {
                // Windows cannot use file mode, fall back to plaintext
                if (isWindows)
                    return 'plaintext';
                return 'file';
            }
        }
    }
    // 4. Default: Windows → plaintext, others → file
    return isWindows ? 'plaintext' : 'file';
}
/**
 * Writes the appSecret into config according to the chosen mode.
 * Returns the SecretInput to store in feishuConfig.appSecret.
 */
async function storeSecret(secretValue, mode, options, config) {
    switch (mode) {
        case 'file': {
            const filePath = options.secretFilePath ?? secret_ref_1.DEFAULT_SECRETS_FILE;
            const writeParams = {
                secretValue,
                filePath,
            };
            if (options.secretFileId)
                writeParams.jsonPointer = options.secretFileId;
            const ref = await (0, secret_ref_1.writeSecretToFile)(writeParams);
            (0, secret_ref_1.ensureFileProviderInConfig)(config, filePath);
            return ref;
        }
        case 'env': {
            const envParams = {
                secretValue,
                openclawDir: (0, config_1.getOpenClawDir)(),
            };
            if (options.secretEnvVar)
                envParams.envVar = options.secretEnvVar;
            const ref = await (0, secret_ref_1.writeSecretToEnv)(envParams);
            return ref;
        }
        case 'plaintext': {
            return secretValue;
        }
    }
}
async function ensureChannelConfig(options = {}) {
    let config = await (0, config_1.readConfig)();
    if (!config)
        config = {};
    // Validate secret params
    const { error, warnings } = validateSecretParams(options);
    if (error) {
        console.error(chalk_1.default.red(`Error(错误): ${error}`));
        process.exit(1);
    }
    for (const w of warnings) {
        console.warn(chalk_1.default.yellow(`Warning: ${w}`));
    }
    // Ensure basic structure
    if (!config.channels)
        config.channels = {};
    if (!config.channels.feishu) {
        config.channels.feishu = {
            enabled: true,
            appId: "",
            appSecret: "",
            domain: "feishu",
            connectionMode: "websocket",
            requireMention: true,
            dmPolicy: "pairing",
            allowFrom: [],
            groupAllowFrom: []
        };
    }
    const feishuConfig = config.channels.feishu;
    // HANDLE --app FLAG
    if (options.app) {
        const firstColonIndex = options.app.indexOf(':');
        if (firstColonIndex === -1 || firstColonIndex === 0 || firstColonIndex === options.app.length - 1) {
            console.error(chalk_1.default.red('Error(错误): --app format must be appId:appSecret (--app 格式必须为 appId:appSecret)'));
            process.exit(1);
        }
        const appId = options.app.substring(0, firstColonIndex);
        const appSecret = options.app.substring(firstColonIndex + 1);
        console.log(chalk_1.default.yellow(`Validating provided credentials for App ID: ${appId} (正在验证 App ID ${appId} 的凭证)...`));
        const isValid = await (0, feishu_auth_1.validateAppCredentials)(appId, appSecret);
        if (!isValid) {
            console.error(chalk_1.default.red('Error(错误): Invalid App ID or App Secret (无效的 App ID 或 App Secret)'));
            process.exit(1);
        }
        feishuConfig.appId = appId;
        const mode = resolveSecretMode(options, feishuConfig.appSecret);
        feishuConfig.appSecret = await storeSecret(appSecret, mode, options, config);
    }
    // HANDLE --use-existing FLAG
    else if (options.useExisting) {
        const appId = feishuConfig.appId;
        const appSecretRaw = feishuConfig.appSecret;
        if (!appId || !appSecretRaw) {
            console.error(chalk_1.default.red('Error(错误): --use-existing specified but no valid configuration found (指定了 --use-existing 但未找到有效配置)'));
            process.exit(1);
        }
        // Resolve secret (supports string, file, env, exec)
        const appSecret = await (0, secret_ref_1.resolveSecretValue)(appSecretRaw, config);
        if (!appSecret) {
            if ((0, secret_ref_1.isSecretRef)(appSecretRaw)) {
                console.error(chalk_1.default.red(`Error(错误): Could not resolve SecretRef (${(0, secret_ref_1.describeSecretRef)(appSecretRaw)}). Check your configuration.`));
            }
            else {
                console.error(chalk_1.default.red('Error(错误): App Secret is empty'));
            }
            process.exit(1);
        }
        console.log(chalk_1.default.yellow(`Validating existing credentials for App ID: ${appId} (正在验证现有 App ID ${appId} 的凭证)...`));
        const isValid = await (0, feishu_auth_1.validateAppCredentials)(appId, appSecret);
        if (!isValid) {
            console.error(chalk_1.default.red('Error(错误): Existing credentials are invalid (现有凭证无效)'));
            process.exit(1);
        }
        console.log(chalk_1.default.green('✓ Credentials verified (✓ 凭证校验成功)'));
        // Keep appSecretRaw unchanged — don't alter existing storage mode
    }
    // HANDLE INTERACTIVE FLOW
    else {
        const currentAppId = feishuConfig.appId || "";
        // Resolve existing secret (plaintext or SecretRef) so interactive flow can validate without re-prompting
        let currentAppSecret;
        const hasSecretRef = (0, secret_ref_1.isSecretRef)(feishuConfig.appSecret);
        if (feishuConfig.appSecret) {
            currentAppSecret = await (0, secret_ref_1.resolveSecretValue)(feishuConfig.appSecret, config) ?? undefined;
        }
        const { appId, appSecret, userInfo, domain, isExisting } = await (0, install_prompts_1.runInstallAuthFlow)(currentAppId, currentAppSecret, options, hasSecretRef);
        if (isExisting) {
            // User chose to reuse existing bot — don't modify existing config, just read/validate
            console.log(chalk_1.default.green('✓ Using existing configuration (✓ 使用现有配置)'));
        }
        else {
            feishuConfig.appId = appId;
            // Determine storage mode — ignore existing config, treat as fresh install (defaults to file)
            const mode = resolveSecretMode(options, undefined);
            feishuConfig.appSecret = await storeSecret(appSecret, mode, options, config);
            if (domain) {
                feishuConfig.domain = domain;
            }
            // Update dmPolicy and allowFrom based on userInfo
            if (userInfo && userInfo.openId) {
                feishuConfig.dmPolicy = "allowlist";
                if (!feishuConfig.allowFrom)
                    feishuConfig.allowFrom = [];
                if (!feishuConfig.allowFrom.includes(userInfo.openId)) {
                    feishuConfig.allowFrom.push(userInfo.openId);
                }
                if (!feishuConfig.groupPolicy) {
                    feishuConfig.groupPolicy = "allowlist";
                    if (!feishuConfig.groupAllowFrom)
                        feishuConfig.groupAllowFrom = [];
                    if (!feishuConfig.groupAllowFrom.includes(userInfo.openId)) {
                        feishuConfig.groupAllowFrom.push(userInfo.openId);
                    }
                    if (!feishuConfig.groups) {
                        feishuConfig.groups = { "*": { enabled: true } };
                    }
                }
            }
            else if (userInfo) {
                feishuConfig.dmPolicy = "open";
            }
            else {
                if (!feishuConfig.dmPolicy) {
                    feishuConfig.dmPolicy = "pairing";
                }
            }
        }
    }
    // Ensure groupPolicy defaults
    if (!feishuConfig.groupPolicy) {
        feishuConfig.groupPolicy = "open";
    }
    // Update plugins.allow
    if (!config.plugins)
        config.plugins = {};
    if (!config.plugins.allow)
        config.plugins.allow = [];
    if (!config.plugins.allow.includes(PLUGIN_NAME)) {
        config.plugins.allow.push(PLUGIN_NAME);
    }
    // Explicitly enable the plugin
    if (!config.plugins.entries)
        config.plugins.entries = {};
    if (!config.plugins.entries[PLUGIN_NAME]) {
        config.plugins.entries[PLUGIN_NAME] = { enabled: true };
    }
    else {
        config.plugins.entries[PLUGIN_NAME].enabled = true;
    }
    await (0, config_1.writeConfig)(config);
}
async function verifyAndStart(options) {
    const finalConfig = await (0, config_1.readConfig)();
    const checks = [
        !finalConfig.plugins?.entries?.feishu?.enabled,
        finalConfig.plugins?.entries?.[PLUGIN_NAME]?.enabled,
        await fs.pathExists(path.join(PLUGIN_PATH, 'node_modules'))
    ];
    if (checks.every(Boolean)) {
        const openclawCmd = (0, system_1.getPlatformCommand)('openclaw');
        try {
            // gateway install is required on OpenClaw >= 2026.3.7 to register the service
            // before restart, otherwise the service ends up in a "disabled" state
            (0, system_1.runCommand)(openclawCmd, ['gateway', 'install']);
        }
        catch (error) {
            // Older versions may not support 'gateway install', ignore
        }
        try {
            (0, system_1.runCommand)(openclawCmd, ['gateway', 'restart']);
        }
        catch (error) {
            console.warn(chalk_1.default.yellow('Warning: Failed to restart OpenClaw gateway.'));
        }
        await new Promise(resolve => setTimeout(resolve, 1000));
        let healthOk = false;
        for (let i = 0; i < 5; i++) {
            try {
                const healthOutput = (0, system_1.runCommandQuiet)(openclawCmd, ['health', '--json']);
                if (healthOutput) {
                    const jsonMatch = healthOutput.match(/\{[\s\S]*\}/);
                    if (jsonMatch) {
                        const health = JSON.parse(jsonMatch[0]);
                        if (health && health.ok === true) {
                            healthOk = true;
                            break;
                        }
                    }
                }
            }
            catch (e) {
                // Ignore errors and retry
            }
            await new Promise(resolve => setTimeout(resolve, 2000));
        }
        if (!healthOk) {
            console.warn(chalk_1.default.yellow('Warning: Health check failed after installation. The service might not be running correctly.'));
        }
        else {
            console.log(chalk_1.default.green('OpenClaw is all set. (OpenClaw 已就绪)'));
        }
    }
    else {
        console.warn(chalk_1.default.yellow('Installation finished but some checks failed. Please run "doctor" command to diagnose.'));
    }
}
async function installCommand(options) {
    const openclawCmd = (0, system_1.getPlatformCommand)('openclaw');
    if (options.version && !/^[0-9a-zA-Z.\-+]+$/.test(options.version)) {
        console.error(chalk_1.default.red('Error: Invalid version format.'));
        process.exit(1);
    }
    // 1. Check openclaw version and resolve compatible plugin version
    let detectedOpenclawVersion = null;
    if (options.skipVersionCheck) {
        console.warn(chalk_1.default.yellow('Warning: Skipping OpenClaw version check. Compatibility is not guaranteed.'));
    }
    else {
        try {
            const output = (0, system_1.runCommandQuiet)(openclawCmd, ['--version']);
            // Parse version from the last line of output (earlier lines may contain config warnings)
            const lines = output ? output.split('\n').map(l => l.trim()).filter(Boolean) : [];
            const lastLine = lines[lines.length - 1];
            const versionMatch = lastLine ? lastLine.match(/(?:OpenClaw\s+)?(\d+\.\d+\.\d+)/) : null;
            detectedOpenclawVersion = versionMatch ? versionMatch[1] ?? null : null;
            if (detectedOpenclawVersion && (0, version_compat_1.compareVersions)(detectedOpenclawVersion, '2026.2.26') < 0) {
                console.error(chalk_1.default.red(`Error: OpenClaw version mismatch. Expected >= 2026.2.26, found ${detectedOpenclawVersion} (raw: ${output}). Please upgrade.`));
                process.exit(1);
            }
            else if (!detectedOpenclawVersion) {
                console.warn(chalk_1.default.yellow(`Warning: Could not parse OpenClaw version from "${output}". Proceeding with installation but compatibility is not guaranteed.`));
            }
        }
        catch (error) {
            console.error(chalk_1.default.red('Error: OpenClaw is not installed or not in PATH.'));
            process.exit(1);
        }
    }
    // Resolve the best compatible openclaw-lark version
    const resolution = (0, version_compat_1.resolveOpenclawLarkVersion)(options.version, detectedOpenclawVersion);
    if (resolution.reason === 'no-match') {
        console.error(chalk_1.default.red(`Error: ${resolution.message}`));
        process.exit(1);
    }
    if (resolution.reason === 'detection-failed') {
        console.warn(chalk_1.default.yellow(`Warning: ${resolution.message}`));
    }
    if (resolution.reason === 'fallback-latest') {
        console.warn(chalk_1.default.yellow(`Warning: ${resolution.message}`));
    }
    if (resolution.reason === 'compat') {
        console.log(chalk_1.default.blue(`${resolution.message}`));
    }
    // Pass resolved version to update if plugin already exists
    const resolvedVersion = resolution.version ?? undefined;
    // For the install package identifier
    const resolvedVersionForInstall = resolution.version;
    // Run migration
    await (0, migration_1.migrateFromFeishuOpenClawPlugin)();
    // 2. Check existing installation
    if (await fs.pathExists(PLUGIN_PATH)) {
        console.log(chalk_1.default.yellow('Plugin is already installed. Starting update process...'));
        const updateOptions = resolvedVersion ? { version: resolvedVersion } : {};
        const newVersion = await (0, update_1.updateCommand)(updateOptions, true, true);
        try {
            await ensureChannelConfig(options);
        }
        catch (error) {
            console.error(chalk_1.default.red('Failed to configure channels (配置渠道失败):'));
            console.error(error);
            process.exit(1);
        }
        await (0, tools_1.ensureFeishuTools)();
        await verifyAndStart(options);
        return;
    }
    // 3. Set npm registry
    try {
        const npmCmd = (0, system_1.getPlatformCommand)('npm');
        (0, system_1.runCommand)(npmCmd, ['config', 'set', 'registry', 'https://registry.npmjs.org/']);
    }
    catch (e) {
        console.error(chalk_1.default.red('Failed to set npm registry.'));
    }
    // 4. Disable built-in plugin
    const config = await (0, config_1.readConfig)();
    if (!config.plugins)
        config.plugins = {};
    if (!config.plugins.entries)
        config.plugins.entries = {};
    if (!config.plugins.entries.feishu)
        config.plugins.entries.feishu = { enabled: false };
    config.plugins.entries.feishu.enabled = false;
    await (0, config_1.writeConfig)(config);
    // 5. Remove conflicting directory
    if (await fs.pathExists(CONFLICT_PLUGIN_PATH)) {
        await fs.remove(CONFLICT_PLUGIN_PATH);
    }
    try {
        await (0, version_compat_1.installPlugin)(PACKAGE_NAME, resolvedVersionForInstall);
    }
    catch (error) {
        console.error(chalk_1.default.red('Failed to install plugin from npm.'));
        console.error(error);
        process.exit(1);
    }
    // 6. Configure channels
    try {
        await ensureChannelConfig(options);
    }
    catch (error) {
        console.error(chalk_1.default.red('Failed to configure channels (配置渠道失败):'));
        console.error(error);
        process.exit(1);
    }
    // 7. Ensure feishu tools are allowed
    await (0, tools_1.ensureFeishuTools)();
    // 8. Final Verification and Start
    await verifyAndStart(options);
}
//# sourceMappingURL=install.js.map