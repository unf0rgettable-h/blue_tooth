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
exports.getPlatformCommand = getPlatformCommand;
exports.runCommand = runCommand;
exports.runCommandQuiet = runCommandQuiet;
exports.spawnCommand = spawnCommand;
const child_process_1 = require("child_process");
const os = __importStar(require("os"));
function getPlatformCommand(command) {
    const isWindows = os.platform() === 'win32';
    if (isWindows && command === 'npm') {
        return `${command}.cmd`;
    }
    return command;
}
function runCommand(command, args = [], options = {}) {
    const isWindows = os.platform() === 'win32';
    try {
        const { status, error } = (0, child_process_1.spawnSync)(command, args, {
            stdio: 'inherit',
            cwd: options.cwd,
            shell: options.shell ?? isWindows
        });
        if (error) {
            throw error;
        }
        if (status !== 0) {
            throw new Error(`Command failed with exit code ${status}`);
        }
    }
    catch (error) {
        throw new Error(`Command failed: ${command} ${args.join(' ')}`);
    }
}
function runCommandQuiet(command, args = [], options = {}) {
    const isWindows = os.platform() === 'win32';
    try {
        const { stdout, error, status } = (0, child_process_1.spawnSync)(command, args, {
            encoding: 'utf-8',
            cwd: options.cwd,
            shell: options.shell ?? isWindows
        });
        if (error) {
            throw error;
        }
        if (status !== 0) {
            throw new Error(`Command failed with exit code ${status}`);
        }
        return stdout.trim();
    }
    catch (error) {
        throw new Error(`Command failed: ${command} ${args.join(' ')}`);
    }
}
/**
 * Spawns a command and inherits stdio. This is useful for interactive commands or long-running processes
 * like logs tailing.
 * This function returns a Promise that resolves when the command exits.
 */
function spawnCommand(command, args, options = {}) {
    const isWindows = os.platform() === 'win32';
    return new Promise((resolve, reject) => {
        // Use 'inherit' to directly use parent stdio. This avoids pipe issues and TTY detection problems.
        const child = (0, child_process_1.spawn)(command, args, {
            stdio: 'inherit',
            cwd: options.cwd,
            shell: options.shell ?? isWindows
        });
        child.on('error', (error) => {
            reject(error);
        });
        child.on('close', (code) => {
            if (code === 0) {
                resolve();
            }
            else {
                // Some commands (like tail -f) might be killed with signals which results in non-zero or null code.
                // But for openclaw logs, usually user interrupts it.
                // If code is null (signal killed), treat as success/resolve? 
                // Or if user Ctrl+C, node process also gets signal.
                // Let's resolve on non-zero too if it's just logs command? 
                // But to be safe, reject on error code, caller can catch.
                if (code === null) {
                    resolve(); // Signal killed
                }
                else {
                    reject(new Error(`Command failed with exit code ${code}`));
                }
            }
        });
    });
}
//# sourceMappingURL=system.js.map