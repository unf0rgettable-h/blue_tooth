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
exports.migrateFromFeishuOpenClawPlugin = migrateFromFeishuOpenClawPlugin;
const fs = __importStar(require("fs-extra"));
const path = __importStar(require("path"));
const chalk_1 = __importDefault(require("chalk"));
const config_1 = require("./config");
const constants_1 = require("./constants");
const system_1 = require("./system");
async function migrateFromFeishuOpenClawPlugin() {
    const extensionsDir = (0, config_1.getExtensionsDir)();
    const oldPluginPath = path.join(extensionsDir, constants_1.OLD_PLUGIN_ID);
    // 1. Detect if old plugin is configured or installed
    const config = await (0, config_1.readConfig)();
    const hasConfigEntry = !!config.plugins?.entries?.[constants_1.OLD_PLUGIN_ID];
    const hasDirectory = await fs.pathExists(oldPluginPath);
    if (hasConfigEntry || hasDirectory) {
        console.log(chalk_1.default.blue(`Migrating: Old plugin ${constants_1.OLD_PLUGIN_ID} detected.`));
        let uninstallSuccess = false;
        // Try using openclaw uninstall command first (cleaner)
        try {
            console.log(chalk_1.default.blue(`Running uninstall command for ${constants_1.OLD_PLUGIN_ID}...`));
            const openclawCmd = (0, system_1.getPlatformCommand)('openclaw');
            await (0, config_1.withCleanFeishuChannel)(() => {
                (0, system_1.runCommand)(openclawCmd, ['plugins', 'uninstall', constants_1.OLD_PLUGIN_ID, '--force']);
            });
            console.log(chalk_1.default.green(`Successfully uninstalled ${constants_1.OLD_PLUGIN_ID} via CLI.`));
            uninstallSuccess = true;
        }
        catch (error) {
            console.warn(chalk_1.default.yellow(`Warning: CLI uninstall failed. Falling back to manual cleanup.`));
        }
        // 2. Failsafe: Cleanup manually if CLI command failed or left artifacts
        let manualCleanupNeeded = false;
        let configChanged = false;
        // Check directory
        if (await fs.pathExists(oldPluginPath)) {
            console.log(chalk_1.default.blue(`Cleanup: Removing residual plugin directory at ${oldPluginPath}...`));
            await fs.remove(oldPluginPath);
        }
        // Check config
        const currentConfig = await (0, config_1.readConfig)();
        // Remove from plugins.entries
        if (currentConfig.plugins?.entries?.[constants_1.OLD_PLUGIN_ID]) {
            console.log(chalk_1.default.blue(`Cleanup: Removing ${constants_1.OLD_PLUGIN_ID} from configuration entries...`));
            delete currentConfig.plugins.entries[constants_1.OLD_PLUGIN_ID];
            configChanged = true;
        }
        // Remove from plugins.allow
        if (currentConfig.plugins?.allow && currentConfig.plugins.allow.includes(constants_1.OLD_PLUGIN_ID)) {
            console.log(chalk_1.default.blue(`Cleanup: Removing ${constants_1.OLD_PLUGIN_ID} from allowed plugins...`));
            currentConfig.plugins.allow = currentConfig.plugins.allow.filter(id => id !== constants_1.OLD_PLUGIN_ID);
            configChanged = true;
        }
        if (configChanged) {
            await (0, config_1.writeConfig)(currentConfig);
        }
        console.log(chalk_1.default.green('Migration check completed.'));
    }
}
//# sourceMappingURL=migration.js.map