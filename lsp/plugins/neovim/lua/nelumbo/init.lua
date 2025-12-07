local NELUMBO = {}

-- Map LSP semantic token types/modifiers to highlight groups
local function setup_semantic_token_highlights()
    local links = {
        -- Token types (from the server legend)
        ["@lsp.type.variable"]           = "Identifier",
        ["@lsp.type.number"]             = "Number",
        ["@lsp.type.string"]             = "String",
        ["@lsp.type.comment"]            = "Comment",
        ["@lsp.type.operator"]           = "Operator",
        ["@lsp.type.type"]               = "Type",

        -- Token modifiers (apply to any type)
        ["@lsp.mod.static"]              = "Constant",

        -- Optional: combined type+modifier groups (if your server uses them)
        -- Example: a static variable token
        ["@lsp.typemod.variable.static"] = "Constant",
    }

    for group, link in pairs(links) do
        -- Use pcall to avoid errors if a group name isn’t supported on your version
        pcall(vim.api.nvim_set_hl, 0, group, { link = link, default = false })
    end
end

function NELUMBO.setup(user_config)
    vim.lsp.set_log_level("debug")
    print("+++ nelumbo trace enabled, LSP log: " .. vim.lsp.get_log_path())

    local config_dir = vim.fn.stdpath("config")
    --print("Config dir: " .. config_dir)

    vim.filetype.add({ extension = { nl = "nelumbo" } })

    local lsp     = require('lspconfig')
    local configs = require('lspconfig.configs')
    local util    = require('lspconfig.util')

    if not configs.nelumbo then
        configs.nelumbo = {
            default_config = {
                cmd = {
                    'java', '-jar', config_dir .. '/plugin/nelumbo/server.jar',
                },
                filetypes = {
                    'nelumbo'
                },
                root_dir = function(fname)
                    return util.find_git_ancestor(fname) or util.path.dirname(fname)
                end,
            }
        }
    end

    configs.nelumbo.setup({
        on_attach = function(client, bufnr)
            -- Ensure semantic tokens are started and refreshed
            if client.server_capabilities.semanticTokensProvider then
                pcall(vim.lsp.semantic_tokens.start, bufnr, client.id)
                pcall(vim.lsp.semantic_tokens.refresh, bufnr)
            end
        end,

    })

    -- Apply once now (important if your colorscheme is already set)
    setup_semantic_token_highlights()

    -- Re-apply after colorscheme changes so links aren’t lost
    vim.api.nvim_create_autocmd("ColorScheme", {
        callback = setup_semantic_token_highlights,
    })
end

return NELUMBO
