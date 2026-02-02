#!/usr/bin/env python3
import sys
import re
import json
import yaml
from flatten_json import flatten
import subprocess

import grpc
import freeplane_pb2_grpc
import freeplane_pb2

channel = grpc.insecure_channel('localhost:50051')
fp = freeplane_pb2_grpc.FreeplaneStub(channel)

def is_url(text):
    return bool(re.fullmatch(r'https?://\S+', text.strip()))


# fp.OpenMap(freeplane_pb2.OpenMapRequest(file_path = "org.mm"))
#
# srcNode = fp.GetCurrentNode(freeplane_pb2.GetCurrentNodeRequest())
# dstNode = fp.GetCurrentNode(freeplane_pb2.GetCurrentNodeRequest())
# print(srcNode)
# print(dstNode)
#
# return 1
#
#
# fp.NodeConnect(freeplane_pb2.NodeConnectRequest(source_node_id = srcNode.node_id, target_node_id = dstNode.node_id, relationship = "XXX"))
#
# fp.NodeAddIcon(freeplane_pb2.NodeAddIconRequest(node_id = dstNode.node_id, icon_name = "stop"))

def mindmap_shell_history(input_cmd, output):
    channel = grpc.insecure_channel('localhost:50051')
    fp = freeplane_pb2_grpc.FreeplaneStub(channel)
    currentNode = fp.GetCurrentNode(freeplane_pb2.GetCurrentNodeRequest())
    print(currentNode)


    lines = output.splitlines()[:5]
    first_five = "\n".join(lines)

    mindmap_shell_history = fp.CreateChild(freeplane_pb2.CreateChildRequest(name=input_cmd, parent_node_id = currentNode.node_id))

    fp.NodeDetailsSet(freeplane_pb2.NodeDetailsSetRequest(node_id=mindmap_shell_history.node_id, details=first_five))
    fp.NodeNoteSet(freeplane_pb2.NodeNoteSetRequest(node_id=mindmap_shell_history.node_id, note=output))
    fp.NodeTagSet(freeplane_pb2.NodeTagSetRequest(node_id=mindmap_shell_history.node_id, tags=["history"]))
    return mindmap_shell_history

def mindmap_add_link(clipboard):
    link = clipboard
    currentNode = fp.GetCurrentNode(freeplane_pb2.GetCurrentNodeRequest())
    fp.NodeLinkSet(freeplane_pb2.NodeLinkSetRequest(node_id=currentNode.node_id, link=link))


def mindmap_node_attr(node, attributes):
    for key, value in attributes.items():
        print(f"KV '{key}' => '{value}'")
        fp.NodeAttributeAdd(freeplane_pb2.NodeAttributeAddRequest(
            node_id=node.node_id,
            attribute_name=key,
            attribute_value=str(value)
        ))
    #currentNode = fp.GetCurrentNode(freeplane_pb2.GetCurrentNodeRequest())
    #            fp.NodeAttributeAdd(freeplane_pb2.NodeAttributeAddRequest(node_id=mindmap_shell_history_node.node_id, attribute_name=key, attribute_value=str(value)))



def mindmap_node_info(clipboard):
    currentNode = fp.GetCurrentNode(freeplane_pb2.GetCurrentNodeRequest())
    lines = clipboard.splitlines()[:5]
    first_five = "\n".join(lines)
    fp.NodeDetailsSet(freeplane_pb2.NodeDetailsSetRequest(node_id=currentNode.node_id, details=first_five))
    fp.NodeNoteSet(freeplane_pb2.NodeNoteSetRequest(node_id=currentNode.node_id, note=clipboard))

def parse_ps1_style(text):
    lines = text.strip().splitlines()
    if not lines:
        return None, None

    first_line = lines[0]
    # Basic match for common bash/zsh prompt style: user@host:path$
    if re.match(r'^\S+@[^:]+:[^$]+\$\s+', first_line):
        command = first_line.strip()
        stdout = "\n".join(lines[1:]).strip()
        return command, stdout
    return None, None

def is_json(text):
    try:
        json.loads(text)
        return True
    except json.JSONDecodeError:
        return False

def is_yaml(text):
    try:
        data = yaml.safe_load(text)
        # Must be a dict with at least one key whose value is a nested dict
        if isinstance(data, dict):
            return any(isinstance(v, dict) for v in data.values())
        return False
    except yaml.YAMLError:
        return False

def extract_key_value_pairs(text):
    patterns = [r'''
        (?P<key>[a-zA-Z0-9_.-]+)     # key: word characters, dot, dash, underscore
        \s*                          # optional whitespace
        ([:=]|=>|->)                 # delimiter
        \s*                          # optional whitespace
        ['"]?                        # optional quote
        (?P<value>[^\s'"]+)          # value: until space or quote
        ['"]?                        # optional closing quote
    ''']


    kv_pairs = {}
    for pattern in patterns:
        for match in re.finditer(pattern, text, re.IGNORECASE):
            key = match.group('key').strip().lower()
            value = match.group('value').strip()
            kv_pairs[key] = value
    return kv_pairs

def main():
    clipboard = sys.stdin.read()

    print("Analyzing clipboard content...\n")




    if is_url(clipboard):
        if "youtube.com" in clipboard or "youtu.be" in clipboard:
            print("Detected: YouTube link")
            subprocess.Popen(["mpv", clipboard])
            return
        print("Detected: Only a URL")
        mindmap_add_link(clipboard)
        return

    currentNode = fp.GetCurrentNode(freeplane_pb2.GetCurrentNodeRequest())

    command, stdout = parse_ps1_style(clipboard)
    if command:
        stdout_is_json = 0
        stdout_is_yaml = 0
        flat_data = None
        mindmap_shell_history_node = mindmap_shell_history(command, stdout)
        print(f"Input Command: {command}")
        if is_json(stdout):
            stdout_is_json = 1
            print("Stdout is valid JSON")
            fp.NodeTagAdd(freeplane_pb2.NodeTagSetRequest(node_id=mindmap_shell_history_node.node_id, tags=["JSON"]))
            flat_data = flatten(json.loads(stdout))
        elif is_yaml(stdout):
            stdout_is_yaml = 1
            print("Stdout is valid YAML")
            fp.NodeTagAdd(freeplane_pb2.NodeTagSetRequest(node_id=mindmap_shell_history_node.node_id, tags=["YAML"]))
            flat_data = flatten(yaml.safe_load(stdout))
        else:
            print("Stdout is neither valid JSON nor YAML")

        if (flat_data):
            for key, value in flat_data.items():
                print(f"flatten {key} => {value}")
                fp.NodeAttributeAdd(freeplane_pb2.NodeAttributeAddRequest(node_id=mindmap_shell_history_node.node_id, attribute_name=key, attribute_value=str(value)))
        return

    attributes = extract_key_value_pairs(clipboard)
    if attributes:
        print("kv")
        mindmap_node_attr(currentNode, attributes)
        return

    mindmap_node_info(clipboard)

if __name__ == "__main__":
    main()
