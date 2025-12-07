---@meta

-- Editor-only types for the Neovim `vim` global.
-- Do NOT require this file at runtime; it exists only for indexing and type hints.

---@class VimOptItem
---@field prepend fun(self: VimOptItem, path: string)

---@class VimOpt: table
---@field rtp VimOptItem

---@class VimCmd: table
---@field colorscheme fun(name: string)

---@class VimFiletype: table
---@field add fun(spec: { extension?: table<string, string>, filename?: table<string, string>, pattern?: table<string, string> })

---@class VimFn: table
---@field stdpath fun(what: "config" | "data" | "cache" | "state" | "log"): string
---@field system fun(cmd: string[] | string): string | nil

---@class VimUtil: table
-- (extend as needed)

---@class VimGlobal: table
---@field g NelumboG
---@field fn VimFn
---@field filetype VimFiletype
---@field opt VimOpt
---@field loop table
---@field cmd VimCmd
---@field util VimUtil

-- Tell the language server/IDE that `vim` exists and has these fields.
---@diagnostic disable: undefined-global, lowercase-global
vim = vim ---@type VimGlobal
