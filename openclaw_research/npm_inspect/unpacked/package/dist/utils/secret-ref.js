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
exports.DEFAULT_JSON_POINTER = exports.DEFAULT_FILE_PROVIDER_NAME = exports.DEFAULT_ENV_VAR = exports.DEFAULT_SECRETS_FILE = void 0;
exports.isSecretRef = isSecretRef;
exports.getByJsonPointer = getByJsonPointer;
exports.setByJsonPointer = setByJsonPointer;
exports.resolveSecretValue = resolveSecretValue;
exports.writeSecretToFile = writeSecretToFile;
exports.writeSecretToEnv = writeSecretToEnv;
exports.ensureFileProviderInConfig = ensureFileProviderInConfig;
exports.describeSecretRef = describeSecretRef;
const fs = __importStar(require("fs-extra"));
const path = __importStar(require("path"));
const os = __importStar(require("os"));
const child_process_1 = require("child_process");
const config_1 = require("./config");
// ---- Constants ----
exports.DEFAULT_SECRETS_FILE = '~/.openclaw/credentials/lark.secrets.json';
exports.DEFAULT_ENV_VAR = 'LARK_APP_SECRET';
exports.DEFAULT_FILE_PROVIDER_NAME = 'lark-secrets';
exports.DEFAULT_JSON_POINTER = '/lark/appSecret';
// ---- Type guard ----
function isSecretRef(obj) {
    return (obj !== null &&
        typeof obj === 'object' &&
        typeof obj.source === 'string' &&
        typeof obj.provider === 'string' &&
        typeof obj.id === 'string' &&
        ['env', 'file', 'exec'].includes(obj.source));
}
// ---- Path resolution ----
function resolveUserPath(p) {
    if (p.startsWith('~/') || p === '~') {
        return path.join(os.homedir(), p.slice(1));
    }
    return path.resolve(p);
}
// ---- JSON Pointer (RFC 6901) ----
function getByJsonPointer(obj, pointer) {
    if (pointer === '' || pointer === '/')
        return obj;
    const tokens = pointer.split('/').slice(1);
    let current = obj;
    for (const token of tokens) {
        const unescaped = token.replace(/~1/g, '/').replace(/~0/g, '~');
        if (current === null || current === undefined || typeof current !== 'object')
            return undefined;
        current = current[unescaped];
    }
    return current;
}
function setByJsonPointer(obj, pointer, value) {
    const tokens = pointer.split('/').slice(1);
    let current = obj;
    for (let i = 0; i < tokens.length - 1; i++) {
        const key = tokens[i].replace(/~1/g, '/').replace(/~0/g, '~');
        if (current[key] === undefined || current[key] === null || typeof current[key] !== 'object') {
            current[key] = {};
        }
        current = current[key];
    }
    const lastKey = tokens[tokens.length - 1].replace(/~1/g, '/').replace(/~0/g, '~');
    current[lastKey] = value;
}
// ---- Resolve ----
async function resolveSecretValue(input, config) {
    if (typeof input === 'string')
        return input;
    switch (input.source) {
        case 'env': {
            // 1. Check process environment first
            const val = process.env[input.id];
            if (val)
                return val;
            // 2. Fall back to reading ~/.openclaw/.env file
            try {
                const envFilePath = path.join((0, config_1.getOpenClawDir)(), '.env');
                if (await fs.pathExists(envFilePath)) {
                    const envContent = await fs.readFile(envFilePath, 'utf8');
                    const lineRegex = new RegExp(`^${input.id}=(.*)$`, 'm');
                    const match = envContent.match(lineRegex);
                    if (match && match[1])
                        return match[1];
                }
            }
            catch {
                // ignore read errors
            }
            return undefined;
        }
        case 'file': {
            const provider = config.secrets?.providers?.[input.provider];
            if (!provider || provider.source !== 'file')
                return undefined;
            const filePath = resolveUserPath(provider.path);
            if (!await fs.pathExists(filePath))
                return undefined;
            // Validate file permissions — openclaw rejects files that are group/world accessible
            if (process.platform !== 'win32') {
                const stat = await fs.lstat(filePath);
                if (stat.isSymbolicLink()) {
                    throw new Error(`Secret file at ${filePath} must not be a symlink.`);
                }
                if (!stat.isFile()) {
                    throw new Error(`Secret file at ${filePath} must be a regular file.`);
                }
                if (!(0, config_1.isPermissionSecure)(stat.mode)) {
                    throw new Error(`Secret file permissions are too open (${filePath}). ` +
                        `openclaw requires 600. Run: chmod 600 "${filePath}"`);
                }
            }
            try {
                const data = await fs.readJSON(filePath);
                if (provider.mode === 'singleValue')
                    return String(data);
                return getByJsonPointer(data, input.id);
            }
            catch (e) {
                if (e.message?.includes('permissions are too open') || e.message?.includes('must not be a symlink') || e.message?.includes('must be a regular file')) {
                    throw e;
                }
                return undefined;
            }
        }
        case 'exec': {
            try {
                const result = (0, child_process_1.execSync)('openclaw', {
                    input: JSON.stringify(['config', 'resolve-secret', '--source', input.source, '--provider', input.provider, '--id', input.id]),
                    encoding: 'utf-8',
                    timeout: 30000,
                    stdio: ['pipe', 'pipe', 'pipe'],
                });
                return result?.trim() || undefined;
            }
            catch {
                return undefined;
            }
        }
    }
}
// ---- Write: file mode ----
async function writeSecretToFile(params) {
    const filePath = resolveUserPath(params.filePath ?? exports.DEFAULT_SECRETS_FILE);
    const pointer = params.jsonPointer ?? exports.DEFAULT_JSON_POINTER;
    // Ensure directory exists with secure permissions (0o700)
    await (0, config_1.ensureDirSecure)(path.dirname(filePath));
    // Read existing content
    let existing = {};
    if (await fs.pathExists(filePath)) {
        try {
            existing = await fs.readJSON(filePath);
        }
        catch {
            existing = {};
        }
    }
    // Set value by JSON Pointer
    setByJsonPointer(existing, pointer, params.secretValue);
    // Write file with secure permissions
    await fs.writeJSON(filePath, existing, { spaces: 2, mode: 0o600 });
    await fs.chmod(filePath, 0o600);
    return {
        source: 'file',
        provider: exports.DEFAULT_FILE_PROVIDER_NAME,
        id: pointer,
    };
}
// ---- Write: env mode ----
async function writeSecretToEnv(params) {
    const envVar = params.envVar ?? exports.DEFAULT_ENV_VAR;
    const envFilePath = path.join(params.openclawDir, '.env');
    let envContent = '';
    if (await fs.pathExists(envFilePath)) {
        envContent = await fs.readFile(envFilePath, 'utf8');
    }
    const lineRegex = new RegExp(`^${envVar}=.*$`, 'm');
    const newLine = `${envVar}=${params.secretValue}`;
    if (lineRegex.test(envContent)) {
        envContent = envContent.replace(lineRegex, newLine);
    }
    else {
        envContent = envContent.trimEnd() + '\n' + newLine + '\n';
    }
    await (0, config_1.ensureDirSecure)(path.dirname(envFilePath));
    await fs.writeFile(envFilePath, envContent, { mode: 0o600 });
    if (process.platform !== 'win32') {
        await fs.chmod(envFilePath, 0o600);
    }
    return {
        source: 'env',
        provider: 'default',
        id: envVar,
    };
}
// ---- Config helpers ----
function ensureFileProviderInConfig(config, filePath, providerName) {
    const name = providerName ?? exports.DEFAULT_FILE_PROVIDER_NAME;
    if (!config.secrets)
        config.secrets = {};
    if (!config.secrets.providers)
        config.secrets.providers = {};
    if (!config.secrets.providers[name]) {
        config.secrets.providers[name] = {
            source: 'file',
            path: filePath,
        };
    }
    else if (config.secrets.providers[name].path !== filePath) {
        // Actual write path differs from provider config — update to keep in sync
        config.secrets.providers[name].path = filePath;
    }
}
function describeSecretRef(ref) {
    switch (ref.source) {
        case 'env':
            return `env:${ref.id}`;
        case 'file':
            return `file:${ref.provider}:${ref.id}`;
        case 'exec':
            return `exec:${ref.provider}:${ref.id}`;
        default:
            return `${ref.source}:${ref.provider}:${ref.id}`;
    }
}
//# sourceMappingURL=secret-ref.js.map