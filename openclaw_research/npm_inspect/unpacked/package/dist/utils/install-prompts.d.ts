import { FeishuAuthOptions } from './feishu-auth';
import type { SecretInputMode } from './config';
interface AuthConfig {
    appId: string;
    appSecret: string;
    userInfo?: {
        openId?: string | undefined;
    };
    domain?: string;
    /** True when the user chose to reuse an existing bot — caller should skip re-storing the secret. */
    isExisting?: boolean;
}
/**
 * Handles the interactive installation flow for Feishu configuration.
 *
 * @param currentAppId Optional existing App ID
 * @param currentAppSecret Optional existing App Secret
 * @param options Auth options (env, lane, debug)
 * @returns Promise<AuthConfig> The validated App ID and App Secret
 */
export declare function runInstallAuthFlow(currentAppId?: string, currentAppSecret?: string, options?: FeishuAuthOptions, hasSecretRef?: boolean): Promise<AuthConfig>;
/**
 * Prompts the user to choose a secret storage mode during interactive install.
 */
export declare function promptStorageMode(defaultMode: SecretInputMode): Promise<SecretInputMode>;
export {};
//# sourceMappingURL=install-prompts.d.ts.map