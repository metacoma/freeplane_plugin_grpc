// Basic usage example for the Freeplane gRPC Go client.
//
// This example demonstrates how to connect to a Freeplane gRPC server,
// interact with mind maps, and manipulate nodes.
//
// To run this example against a live Freeplane server:
//
//	go run examples/basic_usage.go
//
// Note: This example requires a running Freeplane gRPC server at 127.0.0.1:50051.
package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/metacoma/freeplane_plugin_grpc/grpc/golang"
)

func main() {
	// Create a new client with default connection settings
	// Environment variables FREEPLANE_HOST and FREEPLANE_PORT can override defaults
	client, err := freeplane_grpc.NewClient("", 0)
	if err != nil {
		log.Fatalf("Failed to create client: %v", err)
	}

	// Connect to the Freeplane server
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := client.Connect(); err != nil {
		log.Fatalf("Failed to connect: %v", err)
	}
	defer client.Close()

	fmt.Println("Connected to Freeplane gRPC server")

	// Get the current mind map
	mindMap, err := client.CurrentMap(ctx)
	if err != nil {
		log.Printf("No map currently open: %v", err)
		os.Exit(0)
	}

	fmt.Printf("Current map: %s\n", mindMap)

	// Get the root node
	root, err := mindMap.Root()
	if err != nil {
		log.Fatalf("Failed to get root node: %v", err)
	}

	fmt.Printf("Root node: %s\n", root)

	// Get the root node text
	text, err := root.GetText()
	if err != nil {
		log.Printf("Failed to get root text: %v", err)
	} else {
		fmt.Printf("Root text: %s\n", text)
	}

	// Create a child node
	child, err := root.AddChild("New Child Node", "")
	if err != nil {
		log.Fatalf("Failed to create child: %v", err)
	}

	fmt.Printf("Created child node: %s\n", child)

	// Set the child node text
	if err := child.SetText("This is a new child node"); err != nil {
		log.Printf("Failed to set text: %v", err)
	} else {
		fmt.Println("Set child node text")
	}

	// Get children of the root
	children, err := root.Children()
	if err != nil {
		log.Printf("Failed to list children: %v", err)
	} else {
		fmt.Printf("Root has %d children\n", len(children))
	}

	// Set a tag on the child node
	if err := child.SetTags([]string{"tag1", "tag2"}); err != nil {
		log.Printf("Failed to set tags: %v", err)
	} else {
		fmt.Println("Set tags on child node")
	}

	// Set a note on the child node
	if err := child.SetNotes("This is a note on the child node"); err != nil {
		log.Printf("Failed to set note: %v", err)
	} else {
		fmt.Println("Set note on child node")
	}

	// Get the child node note
	note, hasNote, err := child.GetNotes()
	if err != nil {
		log.Printf("Failed to get note: %v", err)
	} else if hasNote {
		fmt.Printf("Child note: %s\n", note)
	}

	// Focus on the child node
	if err := client.FocusNodeByID(ctx, child.NodeID()); err != nil {
		log.Printf("Failed to focus node: %v", err)
	} else {
		fmt.Println("Focused on child node")
	}

	// Set status info
	if err := client.SetStatusInfoText(ctx, "Modified via Go gRPC client"); err != nil {
		log.Printf("Failed to set status info: %v", err)
	} else {
		fmt.Println("Set status info")
	}

	// Export the map to JSON
	jsonData, err := client.GetMapToJSON(ctx)
	if err != nil {
		log.Printf("Failed to export map: %v", err)
	} else {
		fmt.Printf("Map JSON (first 100 chars): %.100s...\n", jsonData)
	}

	fmt.Println("\nExample completed successfully!")
}
