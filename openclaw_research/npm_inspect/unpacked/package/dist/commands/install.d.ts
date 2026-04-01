import { FeishuEnv, SecretInputMode } from '../utils/config';
export interface InstallOptions {
    version?: string;
    env?: FeishuEnv;
    lane?: string;
    debug?: boolean;
    verbose?: boolean;
    skipVersionCheck?: boolean;
    app?: string;
    useExisting?: boolean;
    secretInputMode?: SecretInputMode;
    secretFilePath?: string;
    secretFileId?: string;
    secretEnvVar?: string;
}
export declare function installCommand(options: InstallOptions): Promise<void>;
//# sourceMappingURL=install.d.ts.map