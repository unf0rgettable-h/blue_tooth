import { FeishuEnv } from './config';
export interface FeishuAuthOptions {
    env?: FeishuEnv;
    lane?: string;
    debug?: boolean;
    verbose?: boolean;
}
export interface InitResponse {
    nonce: string;
    supported_auth_methods: string[];
}
export interface BeginResponse {
    device_code: string;
    verification_uri: string;
    user_code: string;
    verification_uri_complete: string;
    interval: number;
    expire_in: number;
}
export interface PollResponse {
    client_id?: string;
    client_secret?: string;
    user_info?: {
        open_id?: string;
        tenant_brand?: 'feishu' | 'lark';
    };
    error?: string;
    error_description?: string;
}
export declare class FeishuAuth {
    private baseUrl;
    private client;
    private debug;
    env: FeishuEnv;
    private lane;
    constructor(options?: FeishuAuthOptions);
    setDomain(isLark: boolean): void;
    init(): Promise<InitResponse>;
    begin(): Promise<BeginResponse>;
    poll(deviceCode: string): Promise<PollResponse>;
    static printQRCode(url: string): void;
}
/**
 * Validates the Feishu app credentials (appId and appSecret) by calling the tenant_access_token_internal endpoint.
 *
 * @param appId The Feishu App ID
 * @param appSecret The Feishu App Secret
 * @returns Promise<boolean> True if credentials are valid, false otherwise.
 */
export declare function validateAppCredentials(appId: string, appSecret: string): Promise<boolean>;
//# sourceMappingURL=feishu-auth.d.ts.map