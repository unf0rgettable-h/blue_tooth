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
exports.doctorCommand = doctorCommand;
const fs = __importStar(require("fs-extra"));
const path = __importStar(require("path"));
const chalk_1 = __importDefault(require("chalk"));
const config_1 = require("../utils/config");
const prompts_1 = require("../utils/prompts");
const system_1 = require("../utils/system");
const constants_1 = require("../utils/constants");
const secret_ref_1 = require("../utils/secret-ref");
const EXTENSIONS_DIR = (0, config_1.getExtensionsDir)();
const PLUGIN_NAME = constants_1.NEW_PLUGIN_ID;
const PLUGIN_PATH = path.join(EXTENSIONS_DIR, PLUGIN_NAME);
async function doctorCommand(options = {}) {
    console.log(chalk_1.default.blue('Running diagnostic checks...'));
    let checksPassed = true;
    // 1. Check plugin directory
    if (!(await fs.pathExists(PLUGIN_PATH))) {
        console.error(chalk_1.default.red(`[FAIL] Plugin directory missing at ${PLUGIN_PATH}`));
        console.warn(chalk_1.default.yellow('Suggestion: Plugin is not installed. Use "feishu-plugin-onboard install" command to install it.'));
        process.exit(1);
    }
    // 2. Check node_modules
    if (await fs.pathExists(PLUGIN_PATH)) {
        if (!(await fs.pathExists(path.join(PLUGIN_PATH, 'node_modules')))) {
            console.error(chalk_1.default.red(`[FAIL] node_modules missing in plugin directory`));
            if (options.fix) {
                console.log(chalk_1.default.blue('Attempting to fix: Running npm install...'));
                try {
                    const npmCmd = (0, system_1.getPlatformCommand)('npm');
                    (0, system_1.runCommand)(npmCmd, ['install'], { cwd: PLUGIN_PATH });
                    console.log(chalk_1.default.green('[FIXED] npm install completed.'));
                }
                catch (e) {
                    console.error(chalk_1.default.red('[FIX FAIL] Failed to run npm install. Please try manually.'));
                }
            }
            else {
                console.warn(chalk_1.default.yellow(`Suggestion: Plugin dependencies not installed. Run "npm install" in ${PLUGIN_PATH} or run "feishu-plugin-onboard doctor --fix".`));
            }
            checksPassed = false;
        }
    }
    // 3. Check file permissions (openclaw rejects files/dirs that are group/world accessible)
    if (process.platform !== 'win32') {
        const stateDir = (0, config_1.getOpenClawDir)();
        const configPath = (0, config_1.getConfigPath)();
        // Check state directory permissions
        if (await fs.pathExists(stateDir)) {
            try {
                const dirStat = await fs.lstat(stateDir);
                if (!(0, config_1.isPermissionSecure)(dirStat.mode)) {
                    console.error(chalk_1.default.red(`[FAIL] State directory permissions are too open (${stateDir}). Recommend chmod 700.`));
                    if (options.fix) {
                        try {
                            await fs.chmod(stateDir, 0o700);
                            console.log(chalk_1.default.green(`[FIXED] Tightened permissions on ${stateDir} to 700.`));
                        }
                        catch (e) {
                            console.error(chalk_1.default.red(`[FIX FAIL] Failed to chmod ${stateDir}.`));
                        }
                    }
                    else {
                        console.warn(chalk_1.default.yellow(`Suggestion: Run chmod 700 "${stateDir}" or "feishu-plugin-onboard doctor --fix".`));
                    }
                    checksPassed = false;
                }
            }
            catch (e) {
                console.warn(chalk_1.default.yellow(`Warning: Could not check permissions on ${stateDir}.`));
            }
        }
        // Check config file permissions
        if (await fs.pathExists(configPath)) {
            try {
                const fileStat = await fs.lstat(configPath);
                if (!(0, config_1.isPermissionSecure)(fileStat.mode)) {
                    console.error(chalk_1.default.red(`[FAIL] Config file is group/world readable (${configPath}). Recommend chmod 600.`));
                    if (options.fix) {
                        try {
                            await fs.chmod(configPath, 0o600);
                            console.log(chalk_1.default.green(`[FIXED] Tightened permissions on ${configPath} to 600.`));
                        }
                        catch (e) {
                            console.error(chalk_1.default.red(`[FIX FAIL] Failed to chmod ${configPath}.`));
                        }
                    }
                    else {
                        console.warn(chalk_1.default.yellow(`Suggestion: Run chmod 600 "${configPath}" or "feishu-plugin-onboard doctor --fix".`));
                    }
                    checksPassed = false;
                }
            }
            catch (e) {
                console.warn(chalk_1.default.yellow(`Warning: Could not check permissions on ${configPath}.`));
            }
        }
        // Check credentials directory and secret files
        const credsDir = path.join(stateDir, 'credentials');
        if (await fs.pathExists(credsDir)) {
            try {
                const credsDirStat = await fs.lstat(credsDir);
                if (!(0, config_1.isPermissionSecure)(credsDirStat.mode)) {
                    console.error(chalk_1.default.red(`[FAIL] Credentials directory permissions are too open (${credsDir}). Recommend chmod 700.`));
                    if (options.fix) {
                        try {
                            await fs.chmod(credsDir, 0o700);
                            console.log(chalk_1.default.green(`[FIXED] Tightened permissions on ${credsDir} to 700.`));
                        }
                        catch (e) {
                            console.error(chalk_1.default.red(`[FIX FAIL] Failed to chmod ${credsDir}.`));
                        }
                    }
                    else {
                        console.warn(chalk_1.default.yellow(`Suggestion: Run chmod 700 "${credsDir}" or "feishu-plugin-onboard doctor --fix".`));
                    }
                    checksPassed = false;
                }
                // Check individual secret files
                const entries = await fs.readdir(credsDir, { withFileTypes: true });
                for (const entry of entries) {
                    if (!entry.isFile())
                        continue;
                    const filePath = path.join(credsDir, entry.name);
                    const fileStat = await fs.lstat(filePath);
                    if (!(0, config_1.isPermissionSecure)(fileStat.mode)) {
                        console.error(chalk_1.default.red(`[FAIL] Secret file permissions are too open (${filePath}). Recommend chmod 600.`));
                        if (options.fix) {
                            try {
                                await fs.chmod(filePath, 0o600);
                                console.log(chalk_1.default.green(`[FIXED] Tightened permissions on ${filePath} to 600.`));
                            }
                            catch (e) {
                                console.error(chalk_1.default.red(`[FIX FAIL] Failed to chmod ${filePath}.`));
                            }
                        }
                        else {
                            console.warn(chalk_1.default.yellow(`Suggestion: Run chmod 600 "${filePath}" or "feishu-plugin-onboard doctor --fix".`));
                        }
                        checksPassed = false;
                    }
                }
            }
            catch (e) {
                // Credentials directory may not exist yet, that's fine
            }
        }
        // Check .env file permissions
        const envFile = path.join(stateDir, '.env');
        if (await fs.pathExists(envFile)) {
            try {
                const envStat = await fs.lstat(envFile);
                if (!(0, config_1.isPermissionSecure)(envStat.mode)) {
                    console.error(chalk_1.default.red(`[FAIL] .env file permissions are too open (${envFile}). Recommend chmod 600.`));
                    if (options.fix) {
                        try {
                            await fs.chmod(envFile, 0o600);
                            console.log(chalk_1.default.green(`[FIXED] Tightened permissions on ${envFile} to 600.`));
                        }
                        catch (e) {
                            console.error(chalk_1.default.red(`[FIX FAIL] Failed to chmod ${envFile}.`));
                        }
                    }
                    else {
                        console.warn(chalk_1.default.yellow(`Suggestion: Run chmod 600 "${envFile}" or "feishu-plugin-onboard doctor --fix".`));
                    }
                    checksPassed = false;
                }
            }
            catch (e) {
                // .env may not exist, that's fine
            }
        }
    }
    // 4. Check configuration (plugins.allow)
    const config = await (0, config_1.readConfig)();
    if (!config.plugins?.allow?.includes(PLUGIN_NAME)) {
        console.error(chalk_1.default.red(`[FAIL] Plugin not found in plugins.allow`));
        if (options.fix) {
            console.log(chalk_1.default.blue('Attempting to fix: Adding plugin to allow list...'));
            try {
                if (!config.plugins)
                    config.plugins = {};
                if (!config.plugins.allow)
                    config.plugins.allow = [];
                config.plugins.allow.push(PLUGIN_NAME);
                await (0, config_1.writeConfig)(config);
                console.log(chalk_1.default.green('[FIXED] Plugin added to allow list.'));
            }
            catch (e) {
                console.error(chalk_1.default.red('[FIX FAIL] Failed to update configuration.'));
            }
        }
        else {
            console.warn(chalk_1.default.yellow('Suggestion: Plugin is not allowed in configuration. Run "feishu-plugin-onboard doctor --fix" to automatically add it.'));
        }
        checksPassed = false;
    }
    // 5. Check channel configuration (supports both plaintext and SecretRef)
    const feishuChannel = config.channels?.feishu;
    let channelConfigValid = false;
    if (feishuChannel) {
        const hasAppId = typeof feishuChannel.appId === 'string' && feishuChannel.appId.length > 0;
        const hasAppSecret = (typeof feishuChannel.appSecret === 'string' && feishuChannel.appSecret.length > 0) || (0, secret_ref_1.isSecretRef)(feishuChannel.appSecret);
        channelConfigValid = hasAppId && hasAppSecret;
        // Try to resolve SecretRef for validation
        if ((0, secret_ref_1.isSecretRef)(feishuChannel.appSecret)) {
            try {
                const resolved = await (0, secret_ref_1.resolveSecretValue)(feishuChannel.appSecret, config);
                if (!resolved) {
                    console.warn(chalk_1.default.yellow(`[WARN] appSecret could not be resolved (will be resolved at gateway runtime)`));
                }
            }
            catch (e) {
                console.warn(chalk_1.default.yellow(`[WARN] appSecret could not be resolved: ${e.message} (will be resolved at gateway runtime)`));
            }
        }
    }
    if (!channelConfigValid) {
        console.error(chalk_1.default.red(`[FAIL] Feishu channel configuration missing or incomplete`));
        if (options.fix) {
            console.log(chalk_1.default.blue('Attempting to fix: Configuring App ID and Secret...'));
            try {
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
                        groupPolicy: "allowlist",
                        allowFrom: [],
                        groupAllowFrom: []
                    };
                }
                const { appId, appSecret } = await (0, prompts_1.promptAppIdSecret)();
                config.channels.feishu.appId = appId;
                config.channels.feishu.appSecret = appSecret;
                await (0, config_1.writeConfig)(config);
                console.log(chalk_1.default.green('[FIXED] Channel configuration updated.'));
            }
            catch (e) {
                console.error(chalk_1.default.red('[FIX FAIL] Failed to update channel configuration.'));
            }
        }
        else {
            console.warn(chalk_1.default.yellow('Suggestion: App ID or Secret missing. Run "feishu-plugin-onboard doctor --fix" to configure them.'));
        }
        checksPassed = false;
    }
    if (checksPassed) {
        console.log(chalk_1.default.green('All checks passed!'));
    }
    else {
        if (options.fix) {
            console.log(chalk_1.default.blue('Fix attempts finished. Please run doctor again to verify.'));
        }
        else {
            console.log(chalk_1.default.yellow('Some checks failed. Use "feishu-plugin-onboard doctor --fix" to attempt automatic repair.'));
            process.exit(1);
        }
    }
}
//# sourceMappingURL=doctor.js.map