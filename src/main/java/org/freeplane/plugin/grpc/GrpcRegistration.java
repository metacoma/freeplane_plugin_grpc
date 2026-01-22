//package org.freeplane.plugin.grpc;
package org.freeplane.plugin.grpc;
//import org.freeplane.plugin.grpc.HelloWorldServer;
//import io.grpc.examples.helloworld;
//
import java.awt.Color;

import com.google.gson.Gson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import org.freeplane.view.swing.features.FitToPage;
import org.freeplane.features.icon.IconClickedEvent;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.IconMouseListener;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.*;
import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.filter.Filter;

import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.map.mindmapmode.MMapController;

import org.freeplane.features.attribute.mindmapmode.AttributeUtilities;
import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.link.ConnectorModel;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.note.NoteModel;

import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.SharedNodeData;
import org.freeplane.features.map.NodeBuilder;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.ui.ViewController;
import org.freeplane.features.url.mindmapmode.MFileManager;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.TextUtils;

import org.freeplane.api.Attributes;
import org.freeplane.core.util.Hyperlink;
//import org.freeplane.plugin.script.FormulaUtils;
//import org.freeplane.plugin.script.FormulaUtils;
//
//
import java.io.File;
import java.net.URL;

import org.freeplane.features.attribute.AttributeMatchesCondition;



import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.net.URI;
import java.net.InetSocketAddress;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


import org.json.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

import java.nio.file.Files;
import java.nio.file.Path;

public class GrpcRegistration {
      private Server server;

    	private String listenAddress;
    	private Integer bindPort;
    	private final Integer defaultPort = 50051;
      public static NodeModel findNodeByAttributeUUID(List<NodeModel> nodes, String uuid) {
          for (NodeModel node : nodes) {
              NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(node);
              for (int i = 0; i < natm.getRowCount(); i++) {
                  Attribute attr = natm.getAttribute(i);
                  if ("_uuid".equals(attr.getName()) && uuid.equals(attr.getValue().toString())) {
                      return node;
                  }
              }
          }
          return null;
      }

      public static List<Map.Entry<String, String>> parseRelationships(String relationshipValue) {
            List<Map.Entry<String, String>> relationships = new ArrayList<>();
            if (relationshipValue == null || relationshipValue.isEmpty()) {
                return relationships;
            }

            String[] entries = relationshipValue.split(",");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    String uuid = parts[0].trim();
                    String type = parts[1].trim();
                    relationships.add(new AbstractMap.SimpleEntry<>(uuid, type));
                }
            }

            return relationships;
        }

      public static List<NodeModel> collectSubtreeNodes(NodeModel root) {
          List<NodeModel> result = new ArrayList<>();
          collectSubtreeNodesRecursive(root, result);
          return result;
      }

      private static void collectSubtreeNodesRecursive(NodeModel node, List<NodeModel> result) {
          if (node == null) {
              return;
          }
          result.add(node);
          for (NodeModel child : node.getChildren()) {
              collectSubtreeNodesRecursive(child, result);
          }
      }

	    public GrpcRegistration(ModeController modeController) {
          listenAddress = System.getenv("GRPC_LISTEN_ADDR");
          String portStr = System.getenv("GRPC_LISTEN_PORT");

          bindPort = (portStr != null && portStr.isEmpty() == false) ? Integer.parseInt(portStr) : defaultPort;

          if (listenAddress == null || listenAddress.isEmpty()) {
              listenAddress = "0.0.0.0";
          }

          try {
      		    server = NettyServerBuilder.forAddress(new InetSocketAddress(listenAddress, bindPort)).addService(new FreeplaneImpl()).build().start();
          } catch(IOException e) {
              System.out.println("Failed to listen socket exception");
          }
          System.out.println("Freeplane grpc plugin loaded and listen on " + listenAddress + ":" + bindPort);
	    }
 	    static class FreeplaneImpl extends FreeplaneGrpc.FreeplaneImplBase {
    	    @Override
    		  public void createChild(CreateChildRequest req, StreamObserver<CreateChildResponse> responseObserver) {
              final MapController mapController = Controller.getCurrentModeController().getMapController();
              final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
              final MapModel map = Controller.getCurrentController().getMap();
              final String parentNodeId = req.getParentNodeId();
              final String newNodeName = req.getName();
              CreateChildResponse reply;

              System.out.println("GRPC Freeplane::createChild(name: " + req.getName() + ", parent_node_id: " + req.getParentNodeId() + ")");
              NodeModel rootNode = (parentNodeId != null && parentNodeId.length() > 0) ? map.getNodeForID(parentNodeId) : mapController.getRootNode();
              if (parentNodeId != null && parentNodeId.length() > 0) {
                  System.out.println("parentNodeId is not null");
              } else {
                  System.out.println("parentNodeId == null");
              }
              if (rootNode != null) {
                  NodeModel newNodeModel = mmapController.newNode(newNodeName, rootNode.getMap());
                  newNodeModel.setSide(mapController.suggestNewChildSide(rootNode, NodeModel.Side.DEFAULT));
                  newNodeModel.createID();
                  mmapController.insertNode(newNodeModel, rootNode, 0);
                  reply = CreateChildResponse.newBuilder().setNodeId(newNodeModel.getID()).setNodeText(newNodeModel.getText()).build();
              } else {
                  System.out.println("GRPC Freeplane::createChild(name: " + req.getName() + ", parent_node_id: " + req.getParentNodeId() + ") root node not found");
                  reply = CreateChildResponse.newBuilder().setNodeId("-1").setNodeText("").build();
              }


      			  responseObserver.onNext(reply);
      			  responseObserver.onCompleted();
    		}
    		@Override
    		public void deleteChild(DeleteChildRequest req, StreamObserver<DeleteChildResponse> responseObserver) {
            System.out.println("GRPC Freeplane::deleteChild(" + req.getNodeId() + ")");
            final MapController mapController = Controller.getCurrentModeController().getMapController();
            final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
            final MapModel map = Controller.getCurrentController().getMap();
            boolean success = false;

            NodeModel nodeToDelete = map.getNodeForID(req.getNodeId());
            if (nodeToDelete != null) {
                success = true;
                mmapController.deleteNode(nodeToDelete);
            }

            DeleteChildResponse reply = DeleteChildResponse.newBuilder().setSuccess(success).build();
      			responseObserver.onNext(reply);
      			responseObserver.onCompleted();
    		}
        @Override
    		public void nodeAttributeAdd(NodeAttributeAddRequest req, StreamObserver<NodeAttributeAddResponse> responseObserver) {
            System.out.println("GRPC Freeplane::nodeAttributeAdd(node_id: " + req.getNodeId() + ", name:" + req.getAttributeName() + ", value: " + req.getAttributeValue() + ")");
            final MapModel map = Controller.getCurrentController().getMap();
            boolean success = false;

            final NodeModel targetNode = map.getNodeForID(req.getNodeId());
            if (targetNode != null) {
                success = true;
                final Attribute newAttribute = new Attribute(req.getAttributeName(), req.getAttributeValue());
                MAttributeController.getController().addAttribute(targetNode, newAttribute);
            }

            NodeAttributeAddResponse reply = NodeAttributeAddResponse.newBuilder().setSuccess(success).build();
      			responseObserver.onNext(reply);
      			responseObserver.onCompleted();
        }
        @Override
    		public void nodeLinkSet(NodeLinkSetRequest req, StreamObserver<NodeLinkSetResponse> responseObserver) {
            boolean success = false;
            final MLinkController mLinkController = (MLinkController) LinkController.getController();
            final MapModel map = Controller.getCurrentController().getMap();
            System.out.println("GRPC Freeplane::nodeLinkSet(node_id: " + req.getNodeId() + ", link:" + req.getLink() + ")");

            NodeModel targetNode = map.getNodeForID(req.getNodeId());
            if (targetNode != null) {
                try {
                    URI uri = new URI(req.getLink());
                    mLinkController.setLink(targetNode, new Hyperlink(uri));
                    success = true;
                } catch(Exception e) {
                    success = false;
                }
            }

            NodeLinkSetResponse reply = NodeLinkSetResponse.newBuilder().setSuccess(success).build();
      			responseObserver.onNext(reply);
      			responseObserver.onCompleted();
       }

        @Override
    		public void nodeDetailsSet(NodeDetailsSetRequest req, StreamObserver<NodeDetailsSetResponse> responseObserver) {
            boolean success = false;
            final MapModel map = Controller.getCurrentController().getMap();
            final MTextController mTextController = (MTextController) TextController.getController();
            System.out.println("GRPC Freeplane::nodeDetailsSet(node_id: " + req.getNodeId() + ", details:" + req.getDetails() + ")");

            NodeModel targetNode = map.getNodeForID(req.getNodeId());
            if (targetNode != null) {
                success = true;
                mTextController.setDetails(targetNode, req.getDetails());
            }

            NodeDetailsSetResponse reply = NodeDetailsSetResponse.newBuilder().setSuccess(success).build();
      			responseObserver.onNext(reply);
      			responseObserver.onCompleted();
        }

        @Override
        public void nodeNoteSet(NodeNoteSetRequest req, StreamObserver<NodeNoteSetResponse> responseObserver) {
            boolean success = false;
            final MNoteController mNoteController = MNoteController.getController();
            final MapModel map = Controller.getCurrentController().getMap();
            final MTextController mTextController = (MTextController) TextController.getController();
            System.out.println("GRPC Freeplane::nodeNoteSet(node_id: " + req.getNodeId() + ", details:" + req.getNote() + ")");

            NodeModel targetNode = map.getNodeForID(req.getNodeId());
            if (targetNode != null) {
                success = true;
                mNoteController.setNoteText(targetNode, req.getNote());
            }

            NodeNoteSetResponse reply = NodeNoteSetResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void nodeTagSet(NodeTagSetRequest req, StreamObserver<NodeTagSetResponse> responseObserver) {
            boolean success = false;
            final MapModel map = Controller.getCurrentController().getMap();
            final MIconController mIconController = (MIconController) IconController.getController();
            //System.out.println("GRPC Freeplane::nodeTagSet(node_id: " + req.getNodeId() + ", details:" + req.getTags() + ")");

            NodeModel targetNode = map.getNodeForID(req.getNodeId());
            if (targetNode != null) {
                List<Tag> tagList = req.getTagsList()
                  .stream()
                  .map(Tag::new)
                  .collect(Collectors.toList());

                mIconController.setTags(targetNode, tagList, false);
                success = true;
            }

            NodeTagSetResponse reply = NodeTagSetResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void nodeTagAdd(NodeTagAddRequest req, StreamObserver<NodeTagAddResponse> responseObserver) {
            boolean success = false;
            final MapModel map = Controller.getCurrentController().getMap();
            final MIconController mIconController = (MIconController) IconController.getController();
            System.out.println("GRPC Freeplane::nodeTagAdd(node_id: " + req.getNodeId() + ", new tags: " + req.getTagsList() + ")");

            NodeModel targetNode = map.getNodeForID(req.getNodeId());

            if (targetNode != null) {
                // Get current tags
                List<Tag> currentTags = mIconController.getTags(targetNode);

                // Convert current tag contents to a set for uniqueness
                Set<String> tagContentSet = currentTags.stream()
                    .map(Tag::getContent)
                    .collect(Collectors.toSet());

                // Add new tags, avoiding duplicates
                for (String newTagContent : req.getTagsList()) {
                    tagContentSet.add(newTagContent);
                }

                // Convert back to List<Tag>
                List<Tag> mergedTags = tagContentSet.stream()
                    .map(Tag::new)
                    .collect(Collectors.toList());

                // Set merged tags
                mIconController.setTags(targetNode, mergedTags, true);

                success = true;
            }

            NodeTagAddResponse reply = NodeTagAddResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void nodeConnect(NodeConnectRequest req, StreamObserver<NodeConnectResponse> responseObserver) {
            boolean success = false;
            final MLinkController mLinkController = (MLinkController) LinkController.getController();
            final MapModel map = Controller.getCurrentController().getMap();
            final NodeModel sourceNode = map.getNodeForID(req.getSourceNodeId());
            final NodeModel targetNode = map.getNodeForID(req.getTargetNodeId());
            final String relationship = req.getRelationship().toString();

            System.out.println("GRPC Freeplane::nodeConnect(source_node_id: " + req.getSourceNodeId() + ", target_node_id: " + req.getTargetNodeId() + ", relationship: " + relationship + ")");

            if (sourceNode != null && targetNode != null) {
                ConnectorModel conn = mLinkController.addConnector(sourceNode, targetNode);
                if (conn != null) {
                  if (relationship != null) {
                      conn.setMiddleLabel(relationship);
                  }
                  success = true;
                } else {
                  System.out.println("GRPC Freeplane::nodeConnect(source_node_id: " + req.getSourceNodeId() + ", target_node_id: " + req.getTargetNodeId() + ", relationship: " + relationship + ") failed, conn = null");
                }
            }
            NodeConnectResponse reply = NodeConnectResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }


        @Override
        public void nodeAddIcon(NodeAddIconRequest req, StreamObserver<NodeAddIconResponse> responseObserver) {
            boolean success = false;
            final MLinkController mLinkController = (MLinkController) LinkController.getController();
            final MapModel map = Controller.getCurrentController().getMap();
            final NodeModel targetNode = map.getNodeForID(req.getNodeId());
            final String iconName = req.getIconName().toString();

            System.out.println("GRPC Freeplane::nodeConnect(node_id: " + req.getNodeId() + ", icon_name: " + req.getIconName() + ")");

            if (targetNode != null) {
                MindIcon icon = IconStoreFactory.ICON_STORE.getMindIcon(iconName);
                if (icon != null) {
                  targetNode.addIcon(icon);
                  success = true;
                } else {
                  System.out.println("GRPC Freeplane::nodeConnect(node_id: " + req.getNodeId() + ", icon_name: " + req.getIconName() + ") failed, icon == null");
                }

            }
            NodeAddIconResponse reply = NodeAddIconResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }


        @Override
    		public void groovy(GroovyRequest req, StreamObserver<GroovyResponse> responseObserver) {
            boolean success = true;
            String groovyCode = new StringBuilder()
                 .append("import org.freeplane.plugin.script.proxy.ScriptUtils;")
                 .append("def c = ScriptUtils.c();")
                 .append("def node = ScriptUtils.node();")
                 .append(req.getGroovyCode())
                 .toString();
            // TODO(@metacoma) eval groovy script
            System.out.println("GRPC Freeplane::groovy(" + groovyCode + ")");
            GroovyResponse reply = GroovyResponse.newBuilder().setSuccess(success).build();
      			responseObserver.onNext(reply);
      			responseObserver.onCompleted();
        }

        @Override
    		public void nodeColorSet(NodeColorSetRequest req, StreamObserver<NodeColorSetResponse> responseObserver) {
            boolean success = false;
            //final MLinkController mLinkController = (MLinkController) LinkController.getController();
            final MNodeStyleController mNodeStyleController = (MNodeStyleController) NodeStyleController.getController();
            final MapModel map = Controller.getCurrentController().getMap();
            final Integer red = req.getRed();
            final Integer green = req.getGreen();
            final Integer blue = req.getBlue();
            final Integer alpha = req.getAlpha();

            System.out.println("GRPC Freeplane::nodeColorSet(node_id: " + req.getNodeId() + ", color:" + red.toString() + " " + green.toString() + " " + blue.toString() + " " + alpha.toString() + ")");


            NodeModel targetNode = map.getNodeForID(req.getNodeId());
            if (targetNode != null) {
                try {
                    //mLinkController.setLink(targetNode, new Hyperlink(uri));
                    success = true;
                    mNodeStyleController.setColor(targetNode, new Color(red, green, blue, alpha));
                } catch(Exception e) {
                    //TODO(@metacoma) handle exception
                    success = false;
                }
            }

            NodeColorSetResponse reply = NodeColorSetResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

         @Override
    		public void nodeBackgroundColorSet(NodeBackgroundColorSetRequest req, StreamObserver<NodeBackgroundColorSetResponse> responseObserver) {
            boolean success = false;
            final MNodeStyleController mNodeStyleController = (MNodeStyleController) NodeStyleController.getController();
            final MapModel map = Controller.getCurrentController().getMap();
            final Integer red = req.getRed();
            final Integer green = req.getGreen();
            final Integer blue = req.getBlue();
            final Integer alpha = req.getAlpha();

            System.out.println("GRPC Freeplane::nodeBackgroundColorSet(node_id: " + req.getNodeId() + ", color:" + red.toString() + " " + green.toString() + " " + blue.toString() + " " + alpha.toString() + ")");

            NodeModel targetNode = map.getNodeForID(req.getNodeId());
            if (targetNode != null) {
                try {
                    success = true;
                    //mNodeStyleController.setBackgroundColor(targetNode, new Color(red, green, blue, alpha));
                    mNodeStyleController.setBackgroundColor(targetNode, new Color(red, green, blue, alpha));
                } catch(Exception e) {
                    //TODO(@metacoma) handle exception
                    success = false;
                }
            }

            NodeBackgroundColorSetResponse reply = NodeBackgroundColorSetResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
        @Override
    		public void statusInfoSet(StatusInfoSetRequest req, StreamObserver<StatusInfoSetResponse> responseObserver) {
            boolean success = false;
            final MapController mapController = Controller.getCurrentModeController().getMapController();
            final String statusInfo = req.getStatusInfo();

            if (statusInfo != null && statusInfo.length() > 0) {
                success = true;
                final ViewController viewController = Controller.getCurrentController().getViewController();
                viewController.out(statusInfo);
            }

            StatusInfoSetResponse reply = StatusInfoSetResponse.newBuilder().setSuccess(success).build();
      			responseObserver.onNext(reply);
      			responseObserver.onCompleted();
        }
        @Override
    		public void getCurrentNode(GetCurrentNodeRequest req, StreamObserver<GetCurrentNodeResponse> responseObserver) {
            boolean success = false;
            final IMapSelection selection = Controller.getCurrentController().getSelection();
            final MapModel map = Controller.getCurrentController().getMap();
            NodeModel currentNode = selection.getSelected();
            GetCurrentNodeResponse reply  = null;
            if (currentNode != null) {
              reply = GetCurrentNodeResponse.newBuilder().setNodeId(currentNode.getID()).setMapId(map.getTitle()).setSuccess(true).build();
            } else {
              reply = GetCurrentNodeResponse.newBuilder().setNodeId("-1").setMapId("-1").setSuccess(false).build();
            }

      			responseObserver.onNext(reply);
      			responseObserver.onCompleted();
        }
        @Override
    		public void openMap(OpenMapRequest req, StreamObserver<OpenMapResponse> responseObserver) {
				    final Controller controller = Controller.getCurrentController();
					  final ModeController modeController = controller.getModeController(MModeController.MODENAME);
            final MapController mapController = Controller.getCurrentModeController().getMapController();
					  final MFileManager fileManager = MFileManager.getController(modeController);
            boolean success = false;
            String mapFilePath = req.getFilePath();
            URL mindmapURL = null;

            System.out.println("GRPC Freeplane::openMap(mapFilePath: " + mapFilePath + ")");

            if (mapFilePath != null) {

              String path = System.getProperty("user.home") + "/mindmaps/" + mapFilePath;
              File file = new File(path);

              try {
                  mindmapURL = file.toURI().toURL();
                  mapController.openMap(mindmapURL);
                  System.out.println("GRPC Freeplane::openMap(URI: " + path + ")");
                  success = true;

              } catch (Exception e) {
                  System.err.println("XXXX file not found, create new map " + mapFilePath);
                  Path dirPath = file.getParentFile().toPath();

                  try {
                      Files.createDirectories(dirPath);

                      MapModel newMapModel = mapController.newMap();
                      newMapModel.setURL(mindmapURL);
                      NodeModel rootNode = newMapModel.getRootNode();

                      rootNode.setText(mapFilePath);
                      fileManager.save(newMapModel);
                      success = true;
                  } catch (IOException e2) {
                      e2.printStackTrace();
                      success = false;
                  }

              }
            }

      			responseObserver.onNext(OpenMapResponse.newBuilder().setSuccess(success).build());
      			responseObserver.onCompleted();
        }
		    @Override
        public void textFSM(TextFSMRequest req, StreamObserver<TextFSMResponse> responseObserver) {
            final MapController mapController = Controller.getCurrentModeController().getMapController();
            final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
            boolean success = false;
            int idx = 0;
            System.out.println("GRPC Freeplane::TextFSM()");

            JSONObject obj = new JSONObject(req.getJson());

            String indexName = obj.getString("index");

            JSONArray header = obj.getJSONArray("header");
            System.out.println("Header: " + header.toString());

            for (int i = 0; i < header.length(); i++) {
                String headerElement = header.getString(i);
                if (headerElement.equals(indexName)) {
                    idx = i;
                    System.out.println("Match found at index " + i);
                    break;
                }
            }


            JSONArray result = obj.getJSONArray("result");
            System.out.println("Result: " + result.toString());

            NodeModel rootNode = mapController.getRootNode();

            for (int i = 0; i < result.length(); i++) {
                JSONArray resultElement = result.getJSONArray(i);

                NodeModel newNodeModel = mmapController.newNode(resultElement.get(idx).toString(), rootNode.getMap());
                newNodeModel.setSide(mapController.suggestNewChildSide(rootNode, NodeModel.Side.DEFAULT));
                newNodeModel.createID();
                mmapController.insertNode(newNodeModel, rootNode, 0);

                for (int j = 0; j < resultElement.length(); j++) {
                    Attribute newAttribute = new Attribute(header.getString(j), resultElement.get(j).toString());
                    try {
                        MAttributeController.getController().addAttribute(newNodeModel, newAttribute);
                    } catch(Exception e) {
                        // sometimes this  happens
                        // https://github.com/metacoma/freeplane_plugin_grpc/issues/1
                    }
                }
            }



            TextFSMResponse reply = TextFSMResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
		    }

        private static void recursiveJSONLoop(JSONObject jsonObject,NodeModel parentNode) {
            final ResourceController resourceController = ResourceController.getResourceController();
            final MapController mapController = Controller.getCurrentModeController().getMapController();
            final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
            final MTextController mTextController = (MTextController) TextController.getController();
            final MIconController mIconController = (MIconController) IconController.getController();
            final MLinkController mLinkController = (MLinkController) LinkController.getController();
            final MNodeStyleController mNodeStyleController = (MNodeStyleController) NodeStyleController.getController();
            final MNoteController mNoteController = MNoteController.getController();
            final MAttributeController mAttributeController = MAttributeController.getController();

            for (String key : jsonObject.keySet()) {
                Object value = jsonObject.get(key);

                System.out.println("GRPC recursiveJSONLoop, key " + key);


                if (value instanceof JSONObject) {
                    // Nested object, recursively iterate
                    NodeModel newNodeModel = mmapController.newNode(key, parentNode.getMap());
                    newNodeModel.setSide(mapController.suggestNewChildSide(parentNode, NodeModel.Side.DEFAULT));
                    newNodeModel.createID();
                    mmapController.insertNode(newNodeModel, parentNode, 0);
                    recursiveJSONLoop((JSONObject) value, newNodeModel);
                } else if (value instanceof JSONArray) {
                    // Array of objects, iterate over each object
                    JSONArray jsonArray = (JSONArray) value;

                    for (int i = 0; i < jsonArray.length(); i++) {
                        NodeModel newNodeModel = mmapController.newNode(Integer.toString(i), parentNode.getMap());
                        newNodeModel.setSide(mapController.suggestNewChildSide(parentNode, NodeModel.Side.DEFAULT));
                        newNodeModel.createID();
                        mmapController.insertNode(newNodeModel, parentNode, 0);
                        Object arrayElement = jsonArray.get(i);
                        if (arrayElement instanceof JSONObject) {
                            recursiveJSONLoop((JSONObject) arrayElement, newNodeModel);
                        }
                    }
                } else {
                    if (key.equals("icons")) {
                       List<String> iconList = Arrays.stream(value.toString().split(","))
                                  .map(String::trim)
                                  .collect(Collectors.toList());

                       for (String iconName : iconList) {
                          MindIcon icon = IconStoreFactory.ICON_STORE.getMindIcon(iconName);
                          if (icon != null) {
                            parentNode.addIcon(icon);
                          }
                       }
                    }
                    if (key.equals("tags")) {
                      List<Tag> tagList = Arrays.stream(value.toString().split(","))
                          .map(String::trim)
                          .filter(s -> !s.isEmpty())
                          .map(Tag::new)
                          .collect(Collectors.toList());

                      mIconController.setTags(parentNode, tagList, false);
                    } else if (key.equals("detail")) {
                        mTextController.setDetails(parentNode, value.toString());
                    } else if (key.equals("link")) {
                        try {
                            URI uri = new URI(value.toString());
                            mLinkController.setLink(parentNode, new Hyperlink(uri));
                        } catch(Exception e) {
                            //TODO(@metacoma) handle exception
                        }
                    } else if (key.equals("note")) {
                        mNoteController.setNoteText(parentNode, value.toString());
                    } else if (key.equals("color")) {
                        int[] rgba = new int[4];
                        Pattern pattern = Pattern.compile("(\\d+)");
                        Matcher matcher = pattern.matcher(value.toString());

                        int index = 0;
                        while (matcher.find() && index < 4) {
                            rgba[index++] = Integer.parseInt(matcher.group());
                        }

                        if (index < 4) {
                            throw new IllegalArgumentException("Input string does not contain exactly four numeric values.");
                        }
                        mNodeStyleController.setBackgroundColor(parentNode, new Color(rgba[0], rgba[1], rgba[2], rgba[3]));
                    } else {
                        Attribute newAttribute = new Attribute(key, value);
                        try {
                            mAttributeController.addAttribute(parentNode, newAttribute);
                        } catch(Exception e) {
                            // sometimes this  happens
                            // https://github.com/metacoma/freeplane_plugin_grpc/issues/1
                            System.out.println("addAttribute fails");
                        }
                    }
                }
            }
        }

        @Override
        public void mindMapFromJSON(MindMapFromJSONRequest req, StreamObserver<MindMapFromJSONResponse> responseObserver) {
            final MapController mapController = Controller.getCurrentModeController().getMapController();
            final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
            final MLinkController mLinkController = (MLinkController) LinkController.getController();
            final MapModel map = Controller.getCurrentController().getMap();
            boolean success = false;
            final AttributeController attributeController = AttributeController.getController();

            System.out.println("GRPC Freeplane::MindMapFromJSON()");
            final String insert_mode_key = "_fp_import_root_node";

            final IMapSelection selection = Controller.getCurrentController().getSelection();
            NodeModel rootNode = selection.getSelected();

            // "Refresh" json canvas
            //mmapController.deleteNodes(rootNode.getChildren());


            final AttributeUtilities atrUtil = new AttributeUtilities();
            if (atrUtil.hasAttributes(rootNode)) {
                  final NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(rootNode);
                  final int j = natm.getRowCount();
                  for (int i = 0; i < j; i++) {
                      AttributeController.getController().performRemoveRow(rootNode, natm, 0);
                  }
            }


            JSONObject obj = new JSONObject(req.getJson());
            if (obj.has(insert_mode_key)) {
                String mode = obj.getString(insert_mode_key);

                if ("root".equals(mode)) {
                    rootNode = mapController.getRootNode();
                }

                if (mode.startsWith("ID_")) {
                    NodeModel pickNode = map.getNodeForID(mode);
                    if (pickNode != null) {
                      rootNode = pickNode;
                    }
                }

                obj.remove(insert_mode_key);
            }


            recursiveJSONLoop(obj, rootNode);

            List<NodeModel> newNodes = collectSubtreeNodes(rootNode);

            System.out.println("====");
            System.out.println("childrenCount: " + rootNode.getChildCount());
            System.out.println("Total nodes: " + newNodes.size());


            for (NodeModel node : newNodes) {

              if (atrUtil.hasAttributes(node)) {

                  NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(node);
                  System.out.println("node has attributes " + natm.getRowCount());
                  for (int i = 0; i < natm.getRowCount(); i++) {
                      Attribute attr = natm.getAttribute(i);
                      //System.out.println("node has attribute " + attr.getName().toString() + " -> " + attr.getValue().toString());
                      if (attr.getName().equals("_relationship")) {
                        NodeModel sourceNode = node;
                        String relValue = attr.getValue().toString();
                        List<Map.Entry<String, String>> pairs = parseRelationships(relValue);

                        for (Map.Entry<String, String> pair : pairs) {
                            String uuid = pair.getKey();
                            String relType = pair.getValue();
                            NodeModel targetNode = findNodeByAttributeUUID(newNodes, uuid);
                            if (targetNode != null) {
                                System.out.println("Relationship: " + node + " --[" + relType + "]--> " + targetNode);
                                ConnectorModel conn = mLinkController.addConnector(sourceNode, targetNode);
                                conn.setMiddleLabel(relType);
                            } else {
                                System.out.println("UUID not found: " + uuid);
                            }
                        }
                     }
                  }
              }
            }
            /*
            for (NodeModel node : newNodes) {
                if (atrUtil.hasAttributes(node)) {
                  final NodeAttributeTableModel attributes = NodeAttributeTableModel.getModel(node);
                  for (int i = attributes.getRowCount() - 1; i >= 0; i--) {
                    if (attributes.getAttribute(i).getName().equals("uuid")) {
                      attributeController.performRemoveRow(node, attributes, i);
                    }
                  }
                }
            }
            */
            System.out.println("====");

            MindMapFromJSONResponse reply = MindMapFromJSONResponse.newBuilder().setSuccess(success).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
        private static String bodyText(String html) {
            String printableText = "";
            try {
                Document document = Jsoup.parse(html);
                Element bodyElement = document.body();
                printableText = bodyElement.text();
            } catch(java.lang.NullPointerException e) {
                // known issue https://github.com/metacoma/freeplane_plugin_grpc/issues/10
                System.out.println("BodyText exception");
            }

            return printableText;
        }

        private void nodeWalk(Map<String, Object> map, NodeModel node) {
            final MIconController mIconController =
                (MIconController) IconController.getController();

            map.put("text", node.getUserObject().toString());

            // ---------- children ----------
            NodeModel[] children = node.getChildren().toArray(new NodeModel[] {});
            if (children.length > 0) {
                List<Map<String, Object>> childrenList = new ArrayList<>();

                for (NodeModel child : children) {
                    Map<String, Object> childMap = new HashMap<>();
                    nodeWalk(childMap, child);
                    childrenList.add(childMap);
                }

                map.put("children", childrenList);
            }

            // ---------- attributes ----------
            final AttributeUtilities atrUtil = new AttributeUtilities();
            if (atrUtil.hasAttributes(node)) {
                Map<String, Object> attributes = new HashMap<>();

                NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(node);
                for (int i = 0; i < natm.getRowCount(); i++) {
                    Attribute attr = natm.getAttribute(i);
                    attributes.put(attr.getName(), String.valueOf(attr.getValue()));
                }

                if (!attributes.isEmpty()) {
                    map.put("attributes", attributes);
                }
            }

            // ---------- detail / note ----------
            if (DetailModel.getDetail(node) != null) {
                map.put("detail", bodyText(DetailModel.getDetailText(node)));
            }

            if (NoteModel.getNoteText(node) != null) {
                map.put("note", bodyText(NoteModel.getNoteText(node)));
            }

            // ---------- tags ----------
            List<Tag> nodeTags = mIconController.getTags(node);
            if (nodeTags != null && !nodeTags.isEmpty()) {
                List<String> tags = nodeTags.stream()
                    .map(Tag::getContent)
                    .collect(Collectors.toList());

                map.put("tags", tags);
            }
        }




        @Override
        public void mindMapToJSON(MindMapToJSONRequest req, StreamObserver<MindMapToJSONResponse> responseObserver) /*throws JsonProcessingException*/ {
            final MapController mapController = Controller.getCurrentModeController().getMapController();
            boolean success = false;
            final NodeModel rootNode = mapController.getRootNode();
            Map<String, Object> result = new HashMap<>();

            final Gson gson = new Gson();

            nodeWalk(result, rootNode);
            System.out.println("GRPC Freeplane::MindMapToJSON()");
            MindMapToJSONResponse reply = MindMapToJSONResponse.newBuilder().setSuccess(success).setJson(gson.toJson(result)).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
