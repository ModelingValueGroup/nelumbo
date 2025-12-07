# nelumbo LSP Neovim

> This is a work in progress. It is not yet ready for use.

A Neovim plugin providing Language Server Protocol (LSP) support for the nelumbo language.

## Install

For now this is a bit clunky, we will streamline it in future releases.

- clone this repo somewhere on your disk (we denote that dir as `~/<PROJECT>`)
- build the server jar (generated in `~/<PROJECT>/server/build/libs/nelumbo-lsp-server-*.jar`)
- make a file: `~/.config/nvim/init.lua` (NB: fill in `<PROJECT>`) either containing or merged with your existing
  config:

```lua
-- bootstrap lazy.nvim
local lazypath = vim.fn.stdpath("data") .. "/lazy/lazy.nvim"
if not vim.loop.fs_stat(lazypath) then
    vim.fn.system({
        "git", "clone", "--filter=blob:none",
        "https://github.com/folke/lazy.nvim.git", "--branch=stable", lazypath
    })
end
vim.opt.rtp:prepend(lazypath)

vim.g.nelumbo_project = vim.env.HOME .. '<PROJECT>'

require("lazy").setup({
    { "neovim/nvim-lspconfig" },
    {
        dir = vim.g.nelumbo_project .. "/plugins/neovim",
        lazy = false,
        dependencies = { "neovim/nvim-lspconfig" },
    },
})

vim.opt.termguicolors = true
vim.cmd.colorscheme('industry')
```

- open Neovim:

```bash
nvim test.nl
```

## Tips

Enable debug logging:

```neovim
:lua vim.lsp.set_log_level("debug")
```

Tail the log:

```bash
tail -f ~/.local/state/nvim/lsp.log
```

```neovim
:checkhealth vim.lsp
:echo &filetype
:LspInfo
:echo executable('java')
:lua print(vim.fn.filereadable(vim.fn.stdpath('config') .. '/plugin/nelumbo/server.jar'))
:lua print((require('lspconfig.util').find_git_ancestor(vim.api.nvim_buf_get_name(0))))
:hi @lsp.type.variable
:lua vim.lsp.semantic_tokens.refresh(0)
```

```bash
pgrep -fal 'java.*server.jar'
```
