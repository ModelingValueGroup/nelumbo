#!/usr/bin/env bash

set -ue

from=~tom/projects/mvg-nelumbo/nelumbo-lsp/plugins/neovim
  to=~tom/.config/nvim

cp $from/plugin/*      $to/plugin/
cp $from/lua/nelumbo/* $to/lua/nelumbo/
cp $from/../../distributions/*server*.jar $to/lua/nelumbo/server.jar
