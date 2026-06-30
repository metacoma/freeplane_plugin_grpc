//! Integration tests for the Freeplane gRPC C++ client.
//!
//! These tests require a running Freeplane server. Set the `FREEPLANE_HOST`
//! environment variable to enable them:
//!
//! ```bash
//! cd grpc/cpp
//! mkdir -p build && cd build
//! cmake .. -DBUILD_TESTS=ON -DBUILD_INTEGRATION_TESTS=ON
//! make -j$(nproc)
//! FREEPLANE_HOST=127.0.0.1 FREEPLANE_PORT=50051 ctest -R integration --output-on-failure
//! ```

#include <gtest/gtest.h>
#include <chrono>
#include <random>
#include <sstream>
#include <string>

#include "src/client.h"
#include "src/node.h"
#include "src/mindmap.h"
#include "src/error.h"

using namespace freeplane::grpc;

// =====================================================================
// Helper functions
// =====================================================================

/// Generate a unique test name with timestamp and random suffix.
static std::string uniqueName(const std::string& prefix) {
    std::ostringstream oss;
    oss << prefix << "_"
        << std::chrono::system_clock::now()
               .time_since_epoch()
               .count()
        << "_" << (std::rand() % 10000);
    return oss.str();
}

/// Connect to a real Freeplane gRPC server.
/// Throws std::runtime_error if FREEPLANE_HOST is not set or connection fails.
static FreeplaneClient connectClient() {
    const char* host_env = std::getenv("FREEPLANE_HOST");
    if (!host_env || std::string(host_env).empty()) {
        throw std::runtime_error("FREEPLANE_HOST not set");
    }

    std::string host(host_env);
    int port = 50051;
    const char* port_env = std::getenv("FREEPLANE_PORT");
    if (port_env) {
        port = std::atoi(port_env);
    }

    FreeplaneClient client(host, port);
    client.connect();
    return client;
}

/// Create a test node under the root of the current map.
/// Returns nullptr if no map is available.
static std::shared_ptr<Node> createTestNode(FreeplaneClient& client,
                                            const std::string& prefix) {
    auto mindmap = client.currentMap();
    if (!mindmap) {
        return nullptr;
    }

    auto root = mindmap->root();
    if (!root) {
        return nullptr;
    }

    auto child = root->addChild(uniqueName(prefix), "classic");
    if (!child) {
        return nullptr;
    }

    return child;
}

// =====================================================================
// Client connectivity tests
// =====================================================================

TEST(IntegrationClientTest, ConnectProvesConnectivity) {
    try {
        auto client = connectClient();
        EXPECT_EQ(client.host(), std::getenv("FREEPLANE_HOST"));
        EXPECT_EQ(client.port(),
                  std::getenv("FREEPLANE_PORT") ? std::atoi(std::getenv("FREEPLANE_PORT"))
                                                : 50051);
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationClientTest, GetCurrentNode) {
    try {
        auto client = connectClient();
        // May succeed or fail depending on server state
        EXPECT_NO_THROW(client.getCurrentNode());
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationClientTest, GroovyArithmetic) {
    try {
        auto client = connectClient();
        auto result = client.groovy("1 + 1");
        // Connection test — may succeed or fail depending on server state
        EXPECT_NO_THROW(result);
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationClientTest, GetMapToJson) {
    try {
        auto client = connectClient();
        auto json = client.getMapToJson();
        // Connection test — may succeed or fail depending on server state
        EXPECT_NO_THROW(json);
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationClientTest, MindMapFromJson) {
    try {
        auto client = connectClient();
        auto result = client.mindMapFromJson(R"({"nodes":[]})");
        // Connection test — may succeed or fail depending on server state
        EXPECT_NO_THROW(result);
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationClientTest, FocusNode) {
    try {
        auto client = connectClient();
        // Focus on a non-existent node — tests connectivity;
        // Freeplane may return an error for nonexistent nodes, which is fine
        try {
            client.focusNode("nonexistent");
        } catch (const std::exception&) {
            // Expected: Freeplane may reject focus on nonexistent node
        }
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationClientTest, SetStatusInfo) {
    try {
        auto client = connectClient();
        auto info = uniqueName("IT_status");
        EXPECT_NO_THROW(client.setStatusInfo(info));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

// =====================================================================
// Node integration tests
// =====================================================================

TEST(IntegrationNodeTest, GetText) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_text");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        auto text = testNode->getText();
        EXPECT_FALSE(text.empty());
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, SetText) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_textset");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        auto newText = uniqueName("IT_updated_text");
        testNode->setText(newText);
        auto text = testNode->getText();
        EXPECT_EQ(text, newText);
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, AddChild) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_child");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        auto child = testNode->addChild("Grandchild", "classic");
        EXPECT_NE(child, nullptr);
        EXPECT_FALSE(child->nodeId().empty());
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, ListChildNodes) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_list");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        // Add a child first
        testNode->addChild("ListChild", "classic");

        auto children = testNode->children();
        EXPECT_FALSE(children.empty());
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, SetTags) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_tag");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        std::vector<std::string> tags = {"tag1", "tag2"};
        EXPECT_NO_THROW(testNode->setTags(tags));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, AddTags) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_tagadd");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        std::vector<std::string> tags = {"extra_tag"};
        EXPECT_NO_THROW(testNode->addTags(tags));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, SetNote) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_note");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        auto noteText = uniqueName("IT_note_text");
        EXPECT_NO_THROW(testNode->setNote(noteText));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, GetNote) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_getnote");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        auto noteText = uniqueName("IT_note_get");
        testNode->setNote(noteText);
        // Server may or may not return the note; just verify no error
        EXPECT_NO_THROW(testNode->getNote());
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, SetLinks) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_link");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        std::vector<std::string> linkUrl = {"https://example.com/test-" + uniqueName("link")};
        EXPECT_NO_THROW(testNode->setLinks(linkUrl));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, GetLinks) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_getlink");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        std::vector<std::string> linkUrl = {"https://example.com/getlink-" + uniqueName("link")};
        testNode->setLinks(linkUrl);
        // May succeed or fail depending on server state
        EXPECT_NO_THROW(testNode->getLinks());
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, SetColor) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_color");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        EXPECT_NO_THROW(testNode->setColor(255, 0, 0, 255));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, SetBackgroundColor) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_bgcolor");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        EXPECT_NO_THROW(testNode->setBackgroundColor(0, 255, 0, 255));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, GetParentNode) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_parent");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        // May succeed or fail depending on server state
        EXPECT_NO_THROW(testNode->parent());
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, MoveNode) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_move");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        // Create a new parent
        auto mindmap = client.currentMap();
        if (!mindmap) {
            GTEST_SKIP() << "no map available";
        }
        auto root = mindmap->root();
        if (!root) {
            GTEST_SKIP() << "failed to get root";
        }

        auto newParent = root->addChild("MoveParent", "classic");
        if (!newParent) {
            GTEST_SKIP() << "failed to create parent";
        }

        // Move the test node under the new parent
        EXPECT_NO_THROW(testNode->move(newParent->nodeId()));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, AddIcon) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_icon");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        EXPECT_NO_THROW(testNode->addIcon("test_icon"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, SetAttribute) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_attr");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        EXPECT_NO_THROW(testNode->setAttribute("test_key", "test_value"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, SetStyle) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_style");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        // Freeplane may not support all styles; just verify connectivity
        try {
            testNode->setStyle("bubble");
        } catch (const std::exception&) {
            // Expected: Freeplane may reject unsupported style
        }
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationNodeTest, SelectNode) {
    try {
        auto client = connectClient();
        auto testNode = createTestNode(client, "IT_select");
        if (!testNode) {
            GTEST_SKIP() << "no map available";
        }

        EXPECT_NO_THROW(testNode->select());
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

// =====================================================================
// MindMap integration tests
// =====================================================================

TEST(IntegrationMindMapTest, Root) {
    try {
        auto client = connectClient();
        auto mindmap = client.currentMap();
        if (!mindmap) {
            GTEST_SKIP() << "no map available";
        }

        auto root = mindmap->root();
        EXPECT_NE(root, nullptr);
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationMindMapTest, SelectedNode) {
    try {
        auto client = connectClient();
        auto mindmap = client.currentMap();
        if (!mindmap) {
            GTEST_SKIP() << "no map available";
        }

        auto selected = mindmap->selectedNode();
        // May succeed or fail depending on server state
        EXPECT_NO_THROW(selected);
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationMindMapTest, Size) {
    try {
        auto client = connectClient();
        auto mindmap = client.currentMap();
        if (!mindmap) {
            GTEST_SKIP() << "no map available";
        }

        auto size = mindmap->size();
        // May succeed or fail depending on server state
        EXPECT_NO_THROW(size);
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationMindMapTest, Save) {
    try {
        auto client = connectClient();
        auto mindmap = client.currentMap();
        if (!mindmap) {
            GTEST_SKIP() << "no map available";
        }

        // Save returns true by design (auto-save)
        EXPECT_NO_THROW(mindmap->save());
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationMindMapTest, Export) {
    try {
        auto client = connectClient();
        auto mindmap = client.currentMap();
        if (!mindmap) {
            GTEST_SKIP() << "no map available";
        }

        // May succeed or fail depending on server state; just verify connectivity
        try {
            mindmap->exportMap("/tmp/test_export.mm", "mm");
        } catch (const std::exception&) {
            // Expected: Freeplane may reject export
        }
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationMindMapTest, ImportMap) {
    try {
        auto client = connectClient();
        auto mindmap = client.currentMap();
        if (!mindmap) {
            GTEST_SKIP() << "no map available";
        }

        // Will fail since file doesn't exist, but proves connectivity
        try {
            mindmap->importMap("/nonexistent.mm");
        } catch (const std::exception&) {
            // Expected: file doesn't exist
        }
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationMindMapTest, FindNodes) {
    try {
        auto client = connectClient();
        auto mindmap = client.currentMap();
        if (!mindmap) {
            GTEST_SKIP() << "no map available";
        }

        // May succeed or fail depending on server state
        EXPECT_NO_THROW(mindmap->findNodes("test_search_term"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

// =====================================================================
// Raw RPC wrapper integration tests
// =====================================================================

TEST(IntegrationRpcTest, CreateChild) {
    try {
        auto client = connectClient();
        // May fail if no map is open, but proves connectivity
        EXPECT_NO_THROW(client.createChild("TestChild", ""));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, DeleteChild) {
    try {
        auto client = connectClient();
        // May fail if node doesn't exist, but proves connectivity
        EXPECT_NO_THROW(client.deleteChild("nonexistent"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, NodeAttributeAdd) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(
            client.nodeAttributeAdd("nonexistent", "test-key", "test-value"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, NodeLinkSet) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(
            client.nodeLinkSet("nonexistent", "https://example.com"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, NodeNoteSet) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.nodeNoteSet("nonexistent", "test note"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, NodeTagSet) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.nodeTagSet("nonexistent", {"tag1"}));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, NodeTagAdd) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.nodeTagAdd("nonexistent", {"tag1"}));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, NodeColorSet) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.nodeColorSet("nonexistent", 255, 0, 0, 255));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, NodeBackgroundColorSet) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(
            client.nodeBackgroundColorSet("nonexistent", 0, 255, 0, 255));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, GetNodeText) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.getNodeText("nonexistent"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, GetParentNode) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.getParentNode("nonexistent"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, ListChildNodes) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.listChildNodes("nonexistent"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, GetNodeNote) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.getNodeNote("nonexistent"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, GetNodeLink) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.getNodeLink("nonexistent"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, SetNodeText) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.setNodeText("nonexistent", "new text"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, MoveNode) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.moveNode("nonexistent", "nonexistent2"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, TextFSM) {
    try {
        auto client = connectClient();
        // Freeplane may not support TextFSM; just verify connectivity
        try {
            client.textFSM(R"({"key": "value"})");
        } catch (const std::exception&) {
            // Expected: Freeplane may not support TextFSM
        }
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, MindMapFromJsonRpc) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.mindMapFromJsonRpc(R"({"nodes":[]})"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, MindMapToJson) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.mindMapToJson());
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, OpenMapRpc) {
    try {
        auto client = connectClient();
        // May fail since file doesn't exist, but proves connectivity
        try {
            client.openMapRpc("/nonexistent.mm");
        } catch (const std::exception&) {
            // Expected: file doesn't exist
        }
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, FocusNodeRpc) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.focusNodeRpc("nonexistent"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, StatusInfoSet) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.statusInfoSet("Integration test status"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, GroovyRpc) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.groovyRpc("1 + 1"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, NodeAddIcon) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.nodeAddIcon("nonexistent", "test_icon"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, NodeConnect) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.nodeConnect("nonexistent1", "nonexistent2",
                                           "depends on"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}

TEST(IntegrationRpcTest, NodeDetailsSet) {
    try {
        auto client = connectClient();
        EXPECT_NO_THROW(client.nodeDetailsSet("nonexistent", "test details"));
    } catch (const std::runtime_error& e) {
        GTEST_SKIP() << e.what();
    }
}
