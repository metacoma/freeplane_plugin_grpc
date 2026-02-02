local M = {}

local function base_cmd(file, line, title, details)
  local home = os.getenv("HOME")
  local cmd = {
    home .. "/bin/freeplane/freeplane_python.sh",
    "./add_file_note.py",
    "--file=" .. file,
    "--title=" .. title,
    string.format("--line=%d", line),
  }

  if details and details ~= "" then
    table.insert(cmd, "--details=" .. details)
  end

  return cmd
end

M.run = function()
  local file = vim.api.nvim_buf_get_name(0)
  local line = vim.api.nvim_win_get_cursor(0)[1]

  local cmd = base_cmd(file, line, nil)
  vim.fn.jobstart(cmd, { detach = true })
end

M.run_with_range = function(line1, line2)
  local file = vim.api.nvim_buf_get_name(0)
  local title = vim.fn.getline("'<")

  local lines = vim.api.nvim_buf_get_lines(0, line1 - 1, line2, false)
  local details = table.concat(lines, "\n")

  local cmd = base_cmd(file, line1, title, details)
  vim.fn.jobstart(cmd, { detach = true })
end

return M
