package freeplane_grpc

import (
	"context"
	"fmt"
	"math/rand"
	"os"
	"testing"
	"time"
)

// getRealClient returns a connected FreeplaneClient for integration tests.
// It skips the test if FREEPLANE_HOST is not set.
func getRealClient(t *testing.T) *FreeplaneClient {
	host := os.Getenv("FREEPLANE_HOST")
	if host == "" {
		t.Skip("skipping integration test: FREEPLANE_HOST not set")
	}

	port := 50051
	if p := os.Getenv("FREEPLANE_PORT"); p != "" {
		fmt.Sscanf(p, "%d", &port)
	}

	client, err := NewClient(host, port)
	if err != nil {
		t.Fatalf("failed to create client: %v", err)
	}

	err = client.Connect()
	if err != nil {
		t.Fatalf("failed to connect: %v", err)
	}

	t.Cleanup(func() {
		_ = client.Close()
	})

	return client
}

// createTestNode creates a unique test node under the root of the current map.
// Returns nil if no map is available.
func createTestNode(t *testing.T, client *FreeplaneClient, prefix string) *Node {
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	mm, err := client.CurrentMap(ctx)
	if err != nil {
		t.Logf("no map available: %v", err)
		return nil
	}

	root, err := mm.Root()
	if err != nil {
		t.Logf("failed to get root: %v", err)
		return nil
	}

	name := uniqueName(prefix)
	child, err := root.AddChild(name, "")
	if err != nil {
		t.Logf("failed to create test node: %v", err)
		return nil
	}

	t.Cleanup(func() {
		if child != nil {
			_ = child.Delete()
		}
	})

	return child
}

// uniqueName generates a unique name with a timestamp and random suffix.
func uniqueName(prefix string) string {
	return fmt.Sprintf("%s_%d_%d", prefix, time.Now().UnixNano(), rand.Intn(10000))
}
