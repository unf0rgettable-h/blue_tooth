"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.FEISHU_TOOLS = void 0;
exports.ensureFeishuTools = ensureFeishuTools;
const config_1 = require("./config");
/**
 * Feishu plugin tools that must be allowed in openclaw.json for the plugin to function.
 */
exports.FEISHU_TOOLS = [
    'feishu_bitable_app',
    'feishu_bitable_app_table',
    'feishu_bitable_app_table_field',
    'feishu_bitable_app_table_record',
    'feishu_bitable_app_table_view',
    'feishu_calendar_calendar',
    'feishu_calendar_event',
    'feishu_calendar_event_attendee',
    'feishu_calendar_freebusy',
    'feishu_chat',
    'feishu_chat_members',
    'feishu_create_doc',
    'feishu_doc_comments',
    'feishu_doc_media',
    'feishu_drive_file',
    'feishu_fetch_doc',
    'feishu_get_user',
    'feishu_im_bot_image',
    'feishu_im_user_fetch_resource',
    'feishu_im_user_get_messages',
    'feishu_im_user_get_thread_messages',
    'feishu_im_user_message',
    'feishu_im_user_search_messages',
    'feishu_oauth',
    'feishu_oauth_batch_auth',
    'feishu_search_doc_wiki',
    'feishu_search_user',
    'feishu_sheet',
    'feishu_task_comment',
    'feishu_task_subtask',
    'feishu_task_task',
    'feishu_task_tasklist',
    'feishu_update_doc',
    'feishu_wiki_space',
    'feishu_wiki_space_node',
];
/**
 * Check if an alsoAllow array already contains any feishu tool.
 */
function shouldSkip(alsoAllow) {
    return alsoAllow.includes('*') || alsoAllow.some(t => exports.FEISHU_TOOLS.includes(t));
}
/**
 * Merge feishu tools into an existing alsoAllow array, avoiding duplicates.
 */
function mergeFeishuTools(existing) {
    const set = new Set(existing);
    for (const tool of exports.FEISHU_TOOLS) {
        set.add(tool);
    }
    return Array.from(set);
}
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
async function ensureFeishuTools() {
    const config = await (0, config_1.readConfig)();
    let modified = false;
    if (config.agents?.list && Array.isArray(config.agents.list) && config.agents.list.length > 0) {
        for (const agent of config.agents.list) {
            if (!agent.tools)
                agent.tools = {};
            const alsoAllow = agent.tools.alsoAllow;
            if (Array.isArray(alsoAllow) && shouldSkip(alsoAllow)) {
                continue;
            }
            agent.tools.alsoAllow = mergeFeishuTools(alsoAllow ?? []);
            modified = true;
        }
    }
    else {
        if (!config.tools)
            config.tools = {};
        const alsoAllow = config.tools.alsoAllow;
        if (Array.isArray(alsoAllow) && shouldSkip(alsoAllow)) {
            // skip
        }
        else {
            config.tools.alsoAllow = mergeFeishuTools(alsoAllow ?? []);
            modified = true;
        }
    }
    if (modified) {
        await (0, config_1.writeConfig)(config);
    }
}
//# sourceMappingURL=tools.js.map