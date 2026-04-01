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
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.FeishuAuth = void 0;
exports.validateAppCredentials = validateAppCredentials;
const axios_1 = __importDefault(require("axios"));
const qrcode = __importStar(require("qrcode-terminal"));
const config_1 = require("./config");
class FeishuAuth {
    constructor(options = {}) {
        this.env = options.env || 'prod';
        this.baseUrl = config_1.FEISHU_ENV_URLS[this.env];
        this.debug = !!options.debug;
        this.lane = options.lane;
        const headers = {
            'Content-Type': 'application/x-www-form-urlencoded',
        };
        if (this.lane) {
            headers['x-tt-env'] = this.lane;
        }
        this.client = axios_1.default.create({
            baseURL: this.baseUrl,
            headers,
            timeout: 10000,
        });
        if (this.debug) {
            this.client.interceptors.request.use(req => {
                console.log('[DEBUG] Request:', {
                    host: req.baseURL || this.baseUrl,
                    url: req.url,
                    method: req.method,
                    headers: req.headers,
                    data: req.data,
                });
                return req;
            });
            this.client.interceptors.response.use(res => {
                console.log('[DEBUG] Response:', {
                    status: res.status,
                    data: res.data,
                });
                return res;
            });
        }
    }
    setDomain(isLark) {
        const urls = isLark ? config_1.LARK_ENV_URLS : config_1.FEISHU_ENV_URLS;
        this.baseUrl = urls[this.env];
        this.client.defaults.baseURL = this.baseUrl;
        if (this.debug) {
            console.log(`[DEBUG] Updated API base URL to: ${this.baseUrl}`);
        }
    }
    async init() {
        const response = await this.client.post('/oauth/v1/app/registration', new URLSearchParams({
            action: 'init'
        }).toString(), {
            baseURL: this.baseUrl
        });
        return response.data;
    }
    async begin() {
        const response = await this.client.post('/oauth/v1/app/registration', new URLSearchParams({
            action: 'begin',
            archetype: 'PersonalAgent',
            auth_method: 'client_secret',
            request_user_info: 'open_id'
        }).toString(), {
            baseURL: this.baseUrl
        });
        return response.data;
    }
    async poll(deviceCode) {
        try {
            const response = await this.client.post('/oauth/v1/app/registration', new URLSearchParams({
                action: 'poll',
                device_code: deviceCode
            }).toString(), {
                baseURL: this.baseUrl
            });
            return response.data;
        }
        catch (error) {
            if (error.response && error.response.data) {
                return error.response.data;
            }
            throw error;
        }
    }
    static printQRCode(url) {
        qrcode.generate(url, { small: true });
    }
}
exports.FeishuAuth = FeishuAuth;
/**
 * Validates the Feishu app credentials (appId and appSecret) by calling the tenant_access_token_internal endpoint.
 *
 * @param appId The Feishu App ID
 * @param appSecret The Feishu App Secret
 * @returns Promise<boolean> True if credentials are valid, false otherwise.
 */
async function validateAppCredentials(appId, appSecret) {
    // Trim inputs to avoid whitespace issues
    const cleanAppId = appId ? appId.trim() : '';
    const cleanAppSecret = appSecret ? appSecret.trim() : '';
    if (!cleanAppId || !cleanAppSecret) {
        return false;
    }
    try {
        // Use feishu.cn for better compatibility and matching user feedback
        const response = await axios_1.default.post('https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal', {
            app_id: cleanAppId,
            app_secret: cleanAppSecret
        }, {
            timeout: 10000 // 10 seconds timeout
        });
        // If the call is successful and we get a tenant_access_token, the credentials are valid.
        // The API returns code: 0 on success.
        if (response.data && response.data.code === 0 && response.data.tenant_access_token) {
            return true;
        }
        return false;
    }
    catch (error) {
        // If the request fails (network error, 4xx, 5xx), consider it invalid or check specific error codes if needed.
        // For now, any error means validation failed.
        return false;
    }
}
//# sourceMappingURL=feishu-auth.js.map