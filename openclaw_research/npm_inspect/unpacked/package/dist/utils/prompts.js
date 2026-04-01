"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.promptAppIdSecret = promptAppIdSecret;
exports.promptConfirmAppId = promptConfirmAppId;
exports.promptAppId = promptAppId;
exports.promptAppSecret = promptAppSecret;
const inquirer_1 = __importDefault(require("inquirer"));
async function promptAppIdSecret() {
    return inquirer_1.default.prompt([
        {
            type: 'input',
            name: 'appId',
            message: 'Enter your App ID: (请输入 App ID:)',
            validate: (input) => input ? true : 'App ID cannot be empty (App ID 不能为空)',
        },
        {
            type: 'input',
            name: 'appSecret',
            message: 'Enter your App Secret: (请输入 App Secret:)',
            validate: (input) => input ? true : 'App Secret cannot be empty (App Secret 不能为空)',
        },
    ]);
}
async function promptConfirmAppId(currentAppId) {
    const answer = await inquirer_1.default.prompt([
        {
            type: 'confirm',
            name: 'confirm',
            message: `Current Feishu App ID is ${currentAppId}. Do you want to use it? (当前 Feishu App ID 为 ${currentAppId}，是否继续使用？)`,
            default: true,
        },
    ]);
    return answer.confirm;
}
async function promptAppId() {
    const answer = await inquirer_1.default.prompt([
        {
            type: 'input',
            name: 'appId',
            message: 'Enter your App ID: (请输入 App ID:)',
            validate: (input) => input ? true : 'App ID cannot be empty (App ID 不能为空)',
        },
    ]);
    return answer.appId;
}
async function promptAppSecret() {
    const answer = await inquirer_1.default.prompt([
        {
            type: 'input',
            name: 'appSecret',
            message: 'Enter your App Secret: (请输入 App Secret:)',
            validate: (input) => input ? true : 'App Secret cannot be empty (App Secret 不能为空)',
        },
    ]);
    return answer.appSecret;
}
//# sourceMappingURL=prompts.js.map