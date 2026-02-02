#!/usr/bin/env python3
import sys
import re
import json
import yaml
from flatten_json import flatten
import subprocess
import argparse
import grpc
import freeplane_pb2_grpc
import freeplane_pb2
import os

def parse_args():
    parser = argparse.ArgumentParser(
        description="Process file, line, and optional metadata."
    )

    parser.add_argument(
        "--file",
        required=True,
        help="Path to the file."
    )

    parser.add_argument(
        "--line",
        required=True,
        help="Line number in the file."
    )

    parser.add_argument(
        "--title",
        help="Optional details text.",
        default=None
    )

    parser.add_argument(
        "--details",
        help="Optional details text.",
        default=None
    )

    parser.add_argument(
        "--notes",
        help="Optional notes text.",
        default=None
    )

    return parser.parse_args()

if __name__ == "__main__":
    args = parse_args()

    print(f"File: {args.file}")
    print(f"Line: {args.line}")
    print(f"Details: {args.details}")
    print(f"Notes: {args.notes}")



    channel = grpc.insecure_channel('localhost:50051')
    fp = freeplane_pb2_grpc.FreeplaneStub(channel)

    groovy_script = """
    // Get the current node
    def node = node
    def attributes = node.getAttributes()
    def parrentNode = node.parent
    // Get 'file' and 'line' attributes
    def file = attributes["file"]
    def line = attributes["line"]

    def path = parentNode['path']

    if (path && file && !file.startsWith("/")) {
      file = path + "/" + file
    }


    def home = System.getenv("HOME")
    if (!line) {
      line = 0
    }

    // Check if attributes are present
    if (file) {
        // Construct and print the command
        def command = "GNOME_TERMINAL_SCREEN='' gnome-terminal --zoom=0.9 -e 'nvim +${line} ${file}'"
        def proc = ["/bin/sh", "-c", command]

        proc.execute()
    } else {
        println "Missing 'file' or 'line' attribute in the node."
    }
    """

    title = os.path.basename(args.file)
    if (args.title):
        title = args.title



    currentNode = fp.GetCurrentNode(freeplane_pb2.GetCurrentNodeRequest())
    textFileNode = fp.CreateChild(freeplane_pb2.CreateChildRequest(name=title, parent_node_id = currentNode.node_id))
    fp.NodeAttributeAdd(freeplane_pb2.NodeAttributeAddRequest(node_id=textFileNode.node_id, attribute_name="script1", attribute_value=groovy_script))
    fp.NodeAttributeAdd(freeplane_pb2.NodeAttributeAddRequest(node_id=textFileNode.node_id, attribute_name="file", attribute_value=args.file))
    fp.NodeAttributeAdd(freeplane_pb2.NodeAttributeAddRequest(node_id=textFileNode.node_id, attribute_name="line", attribute_value=args.line))
    if (args.details):
        fp.NodeDetailsSet(freeplane_pb2.NodeDetailsSetRequest(node_id=textFileNode.node_id, details=args.details))
    #fp.NodeAddIcon(freeplane_pb2.NodeAddIconRequest(node_id=textFileNode.node_id, icon_name = "Generic"))
