/**
 * Feishu plugin tools that must be allowed in openclaw.json for the plugin to function.
 */
export declare const FEISHU_TOOLS: string[];
/**
 * Ensures feishu plugin tools are present in openclaw.json.
 *
 * Logic:
 * 1. If agents.list exists, iterate each agent:
 *    - If agent.tools.alsoAllow has any feishu tool → skip
 *    - Otherwise → add all feishu tools to agent.tools.alsoAllow
 * 2. If agents.list does not exist, check top-level tools.alsoAllow:
 *    - If has any feishu tool → skip
 *    - Otherwise → add feishu tools
 */
export declare function ensureFeishuTools(): Promise<void>;
//# sourceMappingURL=tools.d.ts.map