import type { OpenClawConfig } from './config';
export declare const DEFAULT_SECRETS_FILE = "~/.openclaw/credentials/lark.secrets.json";
export declare const DEFAULT_ENV_VAR = "LARK_APP_SECRET";
export declare const DEFAULT_FILE_PROVIDER_NAME = "lark-secrets";
export declare const DEFAULT_JSON_POINTER = "/lark/appSecret";
export interface SecretRef {
    source: 'env' | 'file' | 'exec';
    provider: string;
    id: string;
}
export type SecretInput = string | SecretRef;
export declare function isSecretRef(obj: any): obj is SecretRef;
export declare function getByJsonPointer(obj: any, pointer: string): any;
export declare function setByJsonPointer(obj: Record<string, any>, pointer: string, value: any): void;
export declare function resolveSecretValue(input: SecretInput, config: OpenClawConfig): Promise<string | undefined>;
export declare function writeSecretToFile(params: {
    secretValue: string;
    filePath?: string;
    jsonPointer?: string;
}): Promise<SecretRef>;
export declare function writeSecretToEnv(params: {
    secretValue: string;
    envVar?: string;
    openclawDir: string;
}): Promise<SecretRef>;
export declare function ensureFileProviderInConfig(config: OpenClawConfig, filePath: string, providerName?: string): void;
export declare function describeSecretRef(ref: SecretRef): string;
//# sourceMappingURL=secret-ref.d.ts.map