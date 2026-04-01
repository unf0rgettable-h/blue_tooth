import { SecretRef } from './secret-ref';
export declare const FEISHU_ENV_URLS: {
    prod: string;
    boe: string;
    pre: string;
};
export declare const LARK_ENV_URLS: {
    prod: string;
    boe: string;
    pre: string;
};
export type FeishuEnv = keyof typeof FEISHU_ENV_URLS;
export type SecretInput = string | SecretRef;
export type SecretInputMode = 'file' | 'env' | 'plaintext';
export interface OpenClawConfig {
    plugins?: {
        allow?: string[];
        entries?: {
            [key: string]: {
                enabled: boolean;
                [key: string]: any;
            };
        };
    };
    channels?: {
        feishu?: {
            enabled: boolean;
            appId: string;
            appSecret?: SecretInput;
            domain: string;
            connectionMode: string;
            requireMention: boolean;
            dmPolicy: string;
            groupPolicy?: string;
            groups?: {
                [key: string]: {
                    enabled: boolean;
                    [key: string]: any;
                };
            };
            allowFrom: any[];
            groupAllowFrom: any[];
        };
        [key: string]: any;
    };
    secrets?: {
        providers?: Record<string, any>;
        defaults?: {
            env?: string;
            file?: string;
        };
    };
    tools?: {
        alsoAllow?: string[];
        [key: string]: any;
    };
    agents?: {
        list?: Array<{
            tools?: {
                alsoAllow?: string[];
                [key: string]: any;
            };
            [key: string]: any;
        }>;
        [key: string]: any;
    };
    [key: string]: any;
}
export declare function getOpenClawDir(): string;
export declare function getConfigPath(): string;
export declare function getExtensionsDir(): string;
export declare function readConfig(): Promise<OpenClawConfig>;
export declare function ensureDirSecure(dirPath: string): Promise<void>;
/**
 * Checks whether file/directory permissions are too open (group/world accessible).
 * Returns true if permissions are secure (no group/world bits set).
 */
export declare function isPermissionSecure(mode: number): boolean;
/**
 * Temporarily strips extra fields from channels.feishu, runs `fn`, then restores them.
 *
 * OpenClaw >= 3.28 validates channels.feishu strictly — extra fields cause
 * `openclaw plugins install/uninstall` to fail. This wrapper removes non-allowed
 * keys before the command and puts them back afterwards so nothing is lost.
 */
export declare function withCleanFeishuChannel<T>(fn: () => T | Promise<T>): Promise<T>;
export declare function writeConfig(config: OpenClawConfig): Promise<void>;
//# sourceMappingURL=config.d.ts.map