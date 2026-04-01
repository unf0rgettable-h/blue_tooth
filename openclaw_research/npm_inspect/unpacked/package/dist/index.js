#!/usr/bin/env node
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const commander_1 = require("commander");
const install_1 = require("./commands/install");
const info_1 = require("./commands/info");
const doctor_1 = require("./commands/doctor");
const update_1 = require("./commands/update");
const self_update_1 = require("./commands/self-update");
const program = new commander_1.Command();
program
    .name('feishu-plugin-onboard')
    .description('CLI for managing Feishu official plugin for OpenClaw')
    .version('1.0.0', '-V, --cli-version');
program
    .command('install')
    .description('Install and configure Feishu official plugin')
    .option('--version <version>', 'Install a specific version of the plugin')
    .option('--env <env>', 'Environment to use (prod, boe, pre)', 'prod')
    .option('--lane <lane>', 'Traffic lane for requests')
    .option('--debug', 'Enable debug logging')
    .option('--verbose', 'Show detailed output including URLs')
    .option('--skip-version-check', 'Skip OpenClaw version validation')
    .option('--app <credentials>', 'Use provided app credentials in format appId:appSecret')
    .option('--use-existing', 'Use existing app configuration without prompting')
    .option('--secret-input-mode <mode>', 'Secret storage mode: file (default), env, or plaintext')
    .option('--secret-file-path <path>', 'File path for secrets file (file mode only)')
    .option('--secret-file-id <pointer>', 'JSON Pointer for secret location in file (file mode only)')
    .option('--secret-env-var <name>', 'Environment variable name (env mode only)')
    .action((options) => (0, install_1.installCommand)(options));
program
    .command('info')
    .description('Show configuration information')
    .option('--all', 'Show all information including configuration content')
    .action((options) => (0, info_1.infoCommand)(options));
program
    .command('doctor')
    .description('Diagnose installation issues')
    .option('--fix', 'Attempt to automatically fix issues')
    .action((options) => (0, doctor_1.doctorCommand)(options));
program
    .command('update')
    .description('Update Feishu official plugin')
    .option('--version <version>', 'Update to a specific version of the plugin')
    .action(async (options) => { await (0, update_1.updateCommand)(options); });
program
    .command('self-update')
    .description('Update the CLI tool to the latest version')
    .option('--version <ver>', 'Specify the version to install')
    .action(async (options) => { await (0, self_update_1.selfUpdateCommand)(options); });
program.parse(process.argv);
//# sourceMappingURL=index.js.map