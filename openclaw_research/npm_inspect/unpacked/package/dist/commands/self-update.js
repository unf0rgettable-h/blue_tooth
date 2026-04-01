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
exports.selfUpdateCommand = void 0;
const child_process_1 = require("child_process");
const ora_1 = __importDefault(require("ora"));
const chalk_1 = __importDefault(require("chalk"));
const path = __importStar(require("path"));
const fs = __importStar(require("fs-extra"));
const selfUpdateCommand = async (options) => {
    const spinner = (0, ora_1.default)('Checking for updates...').start();
    try {
        // Find package.json to determine package name
        // Assuming dist structure: dist/commands/self-update.js
        const packageJsonPath = path.resolve(__dirname, '../../package.json');
        let packageName = '';
        if (await fs.pathExists(packageJsonPath)) {
            const pkg = await fs.readJson(packageJsonPath);
            packageName = pkg.name;
        }
        else {
            spinner.fail('Could not find package.json to determine package name.');
            return;
        }
        // Detect if running via npx
        const isNpx = process.env.npm_lifecycle_event === 'npx' ||
            (process.env.npm_execpath && process.env.npm_execpath.includes('npx')) ||
            __dirname.includes('_npx');
        if (isNpx) {
            spinner.fail(chalk_1.default.yellow('It seems you are running this command via npx.'));
            console.log(chalk_1.default.yellow(' "self-update" is intended to update the globally installed package.'));
            console.log(chalk_1.default.yellow(` To update the global package, please run:\n\n   npm install -g ${packageName}@latest\n`));
            return;
        }
        const targetVersion = options.version || 'latest';
        // Use --force to overwrite existing binaries if there are conflicts (e.g. from previous package name)
        const installCmd = `npm install -g ${packageName}@${targetVersion} --force`;
        spinner.stop();
        console.log(chalk_1.default.blue(`Executing: ${installCmd}`));
        // Execute command
        (0, child_process_1.execSync)(installCmd, { stdio: 'inherit' });
        console.log(chalk_1.default.green(`\nUpdate completed successfully!`));
    }
    catch (error) {
        if (spinner.isSpinning) {
            spinner.fail(chalk_1.default.red(`Update failed: ${error.message}`));
        }
        else {
            console.error(chalk_1.default.red(`\nUpdate failed: ${error.message}`));
        }
        process.exit(1);
    }
};
exports.selfUpdateCommand = selfUpdateCommand;
//# sourceMappingURL=self-update.js.map