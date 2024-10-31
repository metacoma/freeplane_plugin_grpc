while clipnotify; do
  xclip -o 
  echo ---
  xclip -o | ~/freeplane_plugin_grpc/misc/chatgpt/text_2mindmap.sh
done

