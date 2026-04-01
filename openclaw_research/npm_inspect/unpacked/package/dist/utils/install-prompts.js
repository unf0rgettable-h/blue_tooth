"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.runInstallAuthFlow = runInstallAuthFlow;
exports.promptStorageMode = promptStorageMode;
const inquirer_1 = __importDefault(require("inquirer"));
const chalk_1 = __importDefault(require("chalk"));
const ora_1 = __importDefault(require("ora"));
const feishu_auth_1 = require("./feishu-auth");
/**
 * Handles the interactive installation flow for Feishu configuration.
 *
 * @param currentAppId Optional existing App ID
 * @param currentAppSecret Optional existing App Secret
 * @param options Auth options (env, lane, debug)
 * @returns Promise<AuthConfig> The validated App ID and App Secret
 */
async function runInstallAuthFlow(currentAppId, currentAppSecret, options = {}, hasSecretRef = false) {
    let targetAppId = currentAppId;
    let targetAppSecret = currentAppSecret;
    // 1. Check for existing appId and Confirm
    if (targetAppId) {
        const { useExisting } = await inquirer_1.default.prompt([
            {
                type: 'confirm',
                name: 'useExisting',
                message: `Found Feishu bot with App ID ${targetAppId}. Use it for this setup (当前已配置飞书机器人[App ID: ${targetAppId}]，是否继续使用该机器人)?`,
                default: true
            }
        ]);
        if (useExisting) {
            // Yes branch: Proceed directly to verification
            return handleExistingInstallation(targetAppId, targetAppSecret, hasSecretRef);
        }
        else {
            // No branch: User explicitly rejected the existing one.
            // Based on feedback, proceed directly to creating a new bot.
            return handleNewInstallation(options);
        }
    }
    // 2. No appId, directly create new bot
    return handleNewInstallation(options);
}
async function showOptimizationPrompt(appId) {
    // Wait for user to acknowledge optimization
    await inquirer_1.default.prompt([
        {
            type: 'confirm',
            name: 'done',
            message: '完成后请按回车',
            default: true
        }
    ]);
}
async function handleNewInstallation(options) {
    const auth = new feishu_auth_1.FeishuAuth(options);
    let spinner;
    try {
        // 1. Init
        // console.log(chalk.blue('Initializing registration...'));
        const initRes = await auth.init();
        if (!initRes.supported_auth_methods.includes('client_secret')) {
            console.error(chalk_1.default.red('Error: Current environment does not support client_secret auth. Please upgrade onboard tool.'));
            process.exit(1);
        }
        // 2. Begin
        const beginRes = await auth.begin();
        const qrUrl = new URL(beginRes.verification_uri_complete);
        qrUrl.searchParams.set('from', 'onboard');
        const qrUrlStr = qrUrl.toString();
        console.log(chalk_1.default.cyan('\nScan with Feishu to configure your bot (请使用飞书扫码，配置机器人):'));
        if (options.verbose) {
            console.log(chalk_1.default.underline(qrUrlStr));
        }
        feishu_auth_1.FeishuAuth.printQRCode(qrUrlStr);
        console.log('\n');
        // 3. Poll
        const startTime = Date.now();
        let currentInterval = beginRes.interval || 5;
        const expireIn = beginRes.expire_in || 600;
        let isLark = false;
        let domainSwitched = false;
        spinner = (0, ora_1.default)('Fetching configuration results (正在获取你的机器人配置结果)...').start();
        while (Date.now() - startTime < expireIn * 1000) {
            const pollRes = await auth.poll(beginRes.device_code);
            if (options.debug) {
                spinner.stop();
                console.log('[DEBUG] Poll result:', JSON.stringify(pollRes, null, 2));
                spinner.start();
            }
            // Check if domain needs to be switched
            if (pollRes.user_info?.tenant_brand) {
                isLark = pollRes.user_info.tenant_brand === 'lark';
                if (!domainSwitched && isLark) {
                    auth.setDomain(isLark);
                    domainSwitched = true;
                    if (options.debug) {
                        spinner.info(`[DEBUG] Tenant brand is lark, switching domain and retrying poll...`);
                        spinner.start();
                    }
                    continue;
                }
            }
            if (pollRes.client_id && pollRes.client_secret) {
                spinner.succeed(chalk_1.default.green('Success! Bot configured. (机器人配置成功!)'));
                return {
                    appId: pollRes.client_id,
                    appSecret: pollRes.client_secret,
                    userInfo: {
                        openId: pollRes.user_info?.open_id
                    },
                    domain: isLark ? 'lark' : 'feishu'
                };
            }
            if (pollRes.error) {
                if (pollRes.error === 'authorization_pending') {
                    // Just continue waiting
                }
                else if (pollRes.error === 'slow_down') {
                    currentInterval += 5;
                    if (options.debug) {
                        spinner.info(`[DEBUG] Slow down, new interval: ${currentInterval}`);
                        spinner.start('Fetching configuration results (正在获取你的机器人配置结果)...');
                    }
                }
                else if (pollRes.error === 'access_denied') {
                    spinner.fail(chalk_1.default.red('User denied authorization. (用户拒绝授权)'));
                    break; // Fallback to manual
                }
                else if (pollRes.error === 'expired_token') {
                    spinner.fail(chalk_1.default.red('Session expired. Please try again. (会话过期，请重试)'));
                    const { retry } = await inquirer_1.default.prompt([{
                            type: 'confirm',
                            name: 'retry',
                            message: 'Session expired. Retry (会话过期，是否重试)?',
                            default: true
                        }]);
                    if (retry)
                        return handleNewInstallation(options);
                    break;
                }
                else {
                    spinner.fail(chalk_1.default.red(`Error: ${pollRes.error} - ${pollRes.error_description}`));
                    break; // Fallback
                }
            }
            await new Promise(resolve => setTimeout(resolve, currentInterval * 1000));
        }
        if (spinner.isSpinning) {
            spinner.fail(chalk_1.default.yellow('Failed to get results. Try running the install command again, or just type in the app details manually. (获取机器人配置结果失败，你可以重新运行安装命令，或手动输入应用信息)'));
        }
    }
    catch (error) {
        if (spinner && spinner.isSpinning) {
            spinner.fail(chalk_1.default.red('Failed to get results. Try running the install command again, or just type in the app details manually. (获取机器人配置结果失败，你可以重新运行安装命令，或手动输入应用信息)'));
        }
        if (options.debug)
            console.error(error);
    }
    // Fallback
    return promptAndValidateCredentials();
}
async function handleExistingInstallation(appId, appSecret, hasSecretRef = false) {
    let validatedSecret = appSecret;
    // If we don't have a secret, or we need to validate, ensure we have one to test
    if (!validatedSecret) {
        const response = await inquirer_1.default.prompt([
            {
                type: 'password',
                name: 'inputSecret',
                message: 'Enter your App Secret [press Enter to confirm] (请输入 App Secret [按回车确认]):',
                mask: '*',
                validate: (input) => input.length > 0 || 'App Secret cannot be empty (App Secret 不能为空)'
            }
        ]);
        validatedSecret = response.inputSecret;
    }
    // console.log(chalk.yellow('Verifying credentials (正在校验凭证)...'));
    const isValid = await (0, feishu_auth_1.validateAppCredentials)(appId, validatedSecret);
    if (isValid) {
        // console.log(chalk.green('✓ Credentials verified (✓ 凭证校验成功)'));
        // Prompt for optimization
        // await showOptimizationPrompt(appId);
        return { appId, appSecret: validatedSecret, isExisting: true };
    }
    else {
        console.log(chalk_1.default.red('✗ Invalid App ID or App Secret (✗ 无效的 App ID 或 App Secret)'));
        // Fallback to manual entry for both, pre-filling App ID.
        // IMPORTANT: After manual entry success, we MUST also show the optimization prompt.
        const creds = await promptAndValidateCredentials(appId);
        // await showOptimizationPrompt(creds.appId);
        return creds;
    }
}
async function promptAndValidateCredentials(defaultAppId, silent = false) {
    let currentDefaultAppId = defaultAppId;
    while (true) {
        const answers = await inquirer_1.default.prompt([
            {
                type: 'input',
                name: 'appId',
                message: 'Enter your App ID (请输入 App ID):',
                default: currentDefaultAppId,
                validate: (input) => input.length > 0 || 'App ID cannot be empty (App ID 不能为空)'
            },
            {
                type: 'password',
                name: 'appSecret',
                message: 'Enter your App Secret [press Enter to confirm] (请输入 App Secret [按回车确认]):',
                mask: '*',
                validate: (input) => input.length > 0 || 'App Secret cannot be empty (App Secret 不能为空)'
            }
        ]);
        if (!silent) {
            console.log(chalk_1.default.yellow('Verifying credentials (正在校验凭证)...'));
        }
        const isValid = await (0, feishu_auth_1.validateAppCredentials)(answers.appId, answers.appSecret);
        if (isValid) {
            if (!silent) {
                console.log(chalk_1.default.green('✓ Credentials verified (✓ 凭证校验成功)'));
            }
            return { appId: answers.appId, appSecret: answers.appSecret };
        }
        else {
            console.log(chalk_1.default.red('✗ Invalid App ID or App Secret. Please try again. (✗ App ID 或 App Secret 无效，请重试)'));
            // Update defaultAppId for next iteration so they don't have to re-type if it was correct
            currentDefaultAppId = answers.appId;
        }
    }
}
/**
 * Prompts the user to choose a secret storage mode during interactive install.
 */
async function promptStorageMode(defaultMode) {
    const modeChoices = [
        { name: 'Write to a separate file (recommended) (写入独立文件，推荐)', value: 'file' },
        { name: 'Environment variable reference (环境变量引用)', value: 'env' },
        { name: 'Store as plaintext in config (明文存储在配置中)', value: 'plaintext' },
    ];
    const { mode } = await inquirer_1.default.prompt([
        {
            type: 'list',
            name: 'mode',
            message: 'How would you like to store the App Secret (请选择 App Secret 的存储方式)?',
            choices: modeChoices,
            default: defaultMode,
        }
    ]);
    return mode;
}
//# sourceMappingURL=install-prompts.js.map