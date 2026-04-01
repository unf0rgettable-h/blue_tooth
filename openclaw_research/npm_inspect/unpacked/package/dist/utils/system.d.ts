export interface RunCommandOptions {
    cwd?: string;
    shell?: boolean;
}
export declare function getPlatformCommand(command: string): string;
export declare function runCommand(command: string, args?: string[], options?: RunCommandOptions): void;
export declare function runCommandQuiet(command: string, args?: string[], options?: RunCommandOptions): string;
/**
 * Spawns a command and inherits stdio. This is useful for interactive commands or long-running processes
 * like logs tailing.
 * This function returns a Promise that resolves when the command exits.
 */
export declare function spawnCommand(command: string, args: string[], options?: RunCommandOptions): Promise<void>;
//# sourceMappingURL=system.d.ts.map