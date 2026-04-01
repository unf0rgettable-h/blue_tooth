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
Object.defineProperty(exports, "__esModule", { value: true });
exports.LARK_ENV_URLS = exports.FEISHU_ENV_URLS = void 0;
exports.getOpenClawDir = getOpenClawDir;
exports.getConfigPath = getConfigPath;
exports.getExtensionsDir = getExtensionsDir;
exports.readConfig = readConfig;
exports.ensureDirSecure = ensureDirSecure;
exports.isPermissionSecure = isPermissionSecure;
exports.withCleanFeishuChannel = withCleanFeishuChannel;
exports.writeConfig = writeConfig;
const path = __importStar(require("path"));
const fs = __importStar(require("fs-extra"));
const os = __importStar(require("os"));
exports.FEISHU_ENV_URLS = {
    prod: 'https://accounts.feishu.cn',
    boe: 'https://accounts.feishu-boe.cn',
    pre: 'https://accounts.feishu-pre.cn',
};
exports.LARK_ENV_URLS = {
    prod: 'https://accounts.larksuite.com',
    boe: 'https://accounts.larksuite-boe.com',
    pre: 'https://accounts.larksuite-pre.com',
};
function getOpenClawDir() {
    return process.env.OPENCLAW_STATE_DIR || path.join(os.homedir(), '.openclaw');
}
function getConfigPath() {
    return path.join(getOpenClawDir(), 'openclaw.json');
}
function getExtensionsDir() {
    return path.join(getOpenClawDir(), 'extensions');
}
async function readConfig() {
    const configPath = getConfigPath();
    if (await fs.pathExists(configPath)) {
        return fs.readJSON(configPath);
    }
    return {};
}
async function ensureDirSecure(dirPath) {
    await fs.ensureDir(dirPath);
    if (process.platform !== 'win32') {
        await fs.chmod(dirPath, 0o700);
    }
}
/**
 * Checks whether file/directory permissions are too open (group/world accessible).
 * Returns true if permissions are secure (no group/world bits set).
 */
function isPermissionSecure(mode) {
    return (mode & 0o077) === 0;
}
/**
 * Keys in channels.feishu that openclaw validates.
 * Any extra keys must be temporarily removed before running `openclaw plugins install/uninstall`.
 */
const FEISHU_ALLOWED_KEYS = new Set(['enabled', 'domain', 'connectionMode', 'appId', 'appSecret']);
/**
 * Temporarily strips extra fields from channels.feishu, runs `fn`, then restores them.
 *
 * OpenClaw >= 3.28 validates channels.feishu strictly — extra fields cause
 * `openclaw plugins install/uninstall` to fail. This wrapper removes non-allowed
 * keys before the command and puts them back afterwards so nothing is lost.
 */
async function withCleanFeishuChannel(fn) {
    const config = await readConfig();
    const feishu = config.channels?.feishu;
    if (!feishu) {
        return await fn();
    }
    // Save the full snapshot so we can restore everything after the command,
    // because openclaw plugins install/uninstall may reset channels.feishu.
    const savedFeishu = { ...feishu };
    // Strip extra fields for the command
    let stripped = false;
    for (const key of Object.keys(feishu)) {
        if (!FEISHU_ALLOWED_KEYS.has(key)) {
            delete feishu[key];
            stripped = true;
        }
    }
    if (stripped) {
        await writeConfig(config);
    }
    try {
        const result = await fn();
        return result;
    }
    finally {
        // Restore: merge saved snapshot back, preserving the full original state
        const currentConfig = await readConfig();
        if (!currentConfig.channels)
            currentConfig.channels = {};
        currentConfig.channels.feishu = { ...currentConfig.channels.feishu, ...savedFeishu };
        await writeConfig(currentConfig);
    }
}
async function writeConfig(config) {
    const configPath = getConfigPath();
    await ensureDirSecure(path.dirname(configPath));
    await fs.writeJSON(configPath, config, { spaces: 2, mode: 0o600 });
    if (process.platform !== 'win32') {
        await fs.chmod(configPath, 0o600);
    }
}
//# sourceMappingURL=config.js.map