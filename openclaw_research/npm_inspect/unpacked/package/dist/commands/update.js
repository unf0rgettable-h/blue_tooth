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
exports.updateCommand = updateCommand;
const fs = __importStar(require("fs-extra"));
const path = __importStar(require("path"));
const chalk_1 = __importDefault(require("chalk"));
const config_1 = require("../utils/config");
const system_1 = require("../utils/system");
const doctor_1 = require("./doctor");
const constants_1 = require("../utils/constants");
const migration_1 = require("../utils/migration");
const version_compat_1 = require("../utils/version-compat");
const tools_1 = require("../utils/tools");
const EXTENSIONS_DIR = (0, config_1.getExtensionsDir)();
const PLUGIN_NAME = constants_1.NEW_PLUGIN_ID;
const PLUGIN_PATH = path.join(EXTENSIONS_DIR, PLUGIN_NAME);
const PACKAGE_NAME = constants_1.NEW_PLUGIN_PACKAGE;
async function updateCommand(options, silent = false, skipDoctor = false) {
    if (options.version && !/^[0-9a-zA-Z.\-+]+$/.test(options.version)) {
        console.error(chalk_1.default.red('Error: Invalid version format.'));
        process.exit(1);
    }
    console.log(chalk_1.default.blue('Starting update process...'));
    // NEW: Run migration
    await (0, migration_1.migrateFromFeishuOpenClawPlugin)();
    // 1. Remove from configuration
    console.log(chalk_1.default.blue('Cleaning up configuration...'));
    const config = await (0, config_1.readConfig)();
    // Remove plugins.entries.openclaw-lark
    if (config.plugins?.entries?.[PLUGIN_NAME]) {
        delete config.plugins.entries[PLUGIN_NAME];
    }
    // Remove from plugins.allow
    if (config.plugins?.allow) {
        config.plugins.allow = config.plugins.allow.filter(name => name !== PLUGIN_NAME);
    }
    await (0, config_1.writeConfig)(config);
    // 2. Remove plugin (Uninstall via CLI first, then file cleanup)
    console.log(chalk_1.default.blue('Uninstalling old plugin version...'));
    try {
        const openclawCmd = (0, system_1.getPlatformCommand)('openclaw');
        // Try graceful uninstall first
        await (0, config_1.withCleanFeishuChannel)(() => {
            (0, system_1.runCommand)(openclawCmd, ['plugins', 'uninstall', PLUGIN_NAME, '--force']);
        });
    }
    catch (e) {
        // Ignore uninstall error (might not be installed via CLI registry or other issues)
        // We will clean up files manually next
    }
    // Ensure plugin directory is fully removed — openclaw uninstall may leave remnants,
    // which causes the subsequent install to fail with "package.json missing openclaw.hooks"
    if (await fs.pathExists(PLUGIN_PATH)) {
        console.log(chalk_1.default.blue('Cleaning up leftover plugin directory...'));
        await fs.remove(PLUGIN_PATH);
    }
    // 3. Resolve compatible version and install
    console.log(chalk_1.default.blue('Resolving compatible version...'));
    const resolution = (0, version_compat_1.resolveOpenclawLarkVersion)(options.version);
    if (resolution.reason === 'no-match') {
        console.error(chalk_1.default.red(`Error: ${resolution.message}`));
        process.exit(1);
    }
    if (resolution.reason === 'detection-failed' || resolution.reason === 'fallback-latest') {
        console.warn(chalk_1.default.yellow(`Warning: ${resolution.message}`));
    }
    if (resolution.reason === 'compat') {
        console.log(chalk_1.default.blue(`${resolution.message}`));
    }
    console.log(chalk_1.default.blue('Installing new version...'));
    try {
        const npmCmd = (0, system_1.getPlatformCommand)('npm');
        (0, system_1.runCommand)(npmCmd, ['config', 'set', 'registry', 'https://registry.npmjs.org/']);
    }
    catch (e) {
        console.error(chalk_1.default.red('Failed to set npm registry.'));
    }
    try {
        await (0, version_compat_1.installPlugin)(PACKAGE_NAME, resolution.version);
    }
    catch (error) {
        console.error(chalk_1.default.red('Failed to install plugin from npm.'));
        console.error(error);
        process.exit(1);
    }
    // 4. Update configuration again
    const newConfig = await (0, config_1.readConfig)();
    if (!newConfig.plugins)
        newConfig.plugins = {};
    if (!newConfig.plugins.allow)
        newConfig.plugins.allow = [];
    if (!newConfig.plugins.allow.includes(PLUGIN_NAME)) {
        newConfig.plugins.allow.push(PLUGIN_NAME);
    }
    // Remove built-in feishu plugin from allow list if present
    if (newConfig.plugins.allow.includes('feishu')) {
        newConfig.plugins.allow = newConfig.plugins.allow.filter(name => name !== 'feishu');
    }
    // Ensure built-in feishu plugin is disabled to prevent conflicts
    if (newConfig.plugins.entries?.feishu?.enabled) {
        console.log(chalk_1.default.blue('Disabling built-in Feishu plugin...'));
        newConfig.plugins.entries.feishu.enabled = false;
    }
    await (0, config_1.writeConfig)(newConfig);
    // 5. Ensure feishu tools are allowed
    await (0, tools_1.ensureFeishuTools)();
    // 6. Run Doctor Check
    if (!skipDoctor) {
        await (0, doctor_1.doctorCommand)();
    }
    // 7. Report Success and Version
    let version;
    try {
        const packageJsonPath = path.join(PLUGIN_PATH, 'package.json');
        if (await fs.pathExists(packageJsonPath)) {
            const pkg = await fs.readJSON(packageJsonPath);
            version = pkg.version;
            if (!silent) {
                console.log(chalk_1.default.green(`Update complete! New version: ${version}`));
            }
        }
        else {
            if (!silent) {
                console.warn(chalk_1.default.yellow('Update complete, but could not determine new version (package.json missing).'));
            }
        }
    }
    catch (error) {
        if (!silent) {
            console.warn(chalk_1.default.yellow('Update complete, but failed to read version info.'));
        }
    }
    return version;
}
//# sourceMappingURL=update.js.map