vim.api.nvim_create_user_command(
  "Note",
  function(opts)
    local note = require("freeplane.note")

    if opts.range == 0 then
      note.run()
    else
      note.run_with_range(opts.line1, opts.line2)
    end
  end,
  { range = true }  -- <-- allow :'<,'>Note
)
