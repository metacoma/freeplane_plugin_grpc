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
import org.freeplane.features.mode.ModeController;

import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.map.mindmapmode.MMapController;

import org.freeplane.features.attribute.mindmapmode.AttributeUtilities;
import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.link.mindmapmode.MLinkController;
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
import org.freeplane.features.ui.ViewController;
import org.freeplane.core.util.TextUtils;

import org.freeplane.api.Attributes;
import org.freeplane.core.util.Hyperlink;
//import org.freeplane.plugin.script.FormulaUtils;
//import org.freeplane.plugin.script.FormulaUtils;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.net.URI;
import java.net.InetSocketAddress;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


import org.json.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrpcRegistration {
        private Server server;

    	private String listenAddress;
    	private Integer bindPort;
    	private final Integer defaultPort = 50051;

	public GrpcRegistration(ModeController modeController) {
		listenAddress = System.getenv("GRPC_LISTEN_ADDR");
		String portStr = System.getenv("GRPC_LISTEN_PORT");

		bindPort = (portStr != null && portStr.isEmpty() == false) ? Integer.parseInt(portStr) : defaultPort;

  		if (listenAddress == null || listenAddress.isEmpty()) {
            		listenAddress = "0.0.0.0";
        	}

                try {
      			server = NettyServerBuilder.forAddress(new InetSocketAddress(listenAddress, bindPort)) .addService(new FreeplaneImpl()).build().start();
                  } catch(IOException e) {
                     System.out.println("exception");
                  }
                System.out.println("Freeplane grpc plugin loaded and listen on " + listenAddress + ":" + bindPort);
	}
 	static class FreeplaneImpl extends FreeplaneGrpc.FreeplaneImplBase {

    		@Override
    		public void createChild(CreateChildRequest req, StreamObserver<CreateChildResponse> responseObserver) {
                        //final MapController mapController = Controller.getCurrentModeController().getMapController();
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
                          NodeModel newNodeModel = mapController.newNode(newNodeName, rootNode.getMap());
                          newNodeModel.setSide(mapController.suggestNewChildSide(rootNode, NodeModel.Side.DEFAULT));
                          newNodeModel.createID();
                          mmapController.insertNode(newNodeModel, rootNode, false);
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
                        //final MLinkController mLinkController = (MLinkController) LinkController.getController();
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

  				NodeModel newNodeModel = mapController.newNode(resultElement.get(idx).toString(), rootNode.getMap());
				newNodeModel.setSide(mapController.suggestNewChildSide(rootNode, NodeModel.Side.DEFAULT));
				newNodeModel.createID();
  				mmapController.insertNode(newNodeModel, rootNode, false);

				for (int j = 0; j < resultElement.length(); j++) {
					Attribute newAttribute = new Attribute(header.getString(j), resultElement.get(j).toString());
					MAttributeController.getController().addAttribute(newNodeModel, newAttribute);
				}
			}



			TextFSMResponse reply = TextFSMResponse.newBuilder().setSuccess(success).build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		private static void recursiveJSONLoop(JSONObject jsonObject,NodeModel parentNode) {
	    	final MapController mapController = Controller.getCurrentModeController().getMapController();
		    final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
        final MTextController mTextController = (MTextController) TextController.getController();
        final MLinkController mLinkController = (MLinkController) LinkController.getController();
        final MNodeStyleController mNodeStyleController = (MNodeStyleController) NodeStyleController.getController();
		    final MNoteController mNoteController = MNoteController.getController();
		    for (String key : jsonObject.keySet()) {
			    Object value = jsonObject.get(key);

			    System.out.println("GRPC recursiveJSONLoop, key " + key);


          if (value instanceof JSONObject) {
              // Nested object, recursively iterate
              NodeModel newNodeModel = mapController.newNode(key, parentNode.getMap());
              newNodeModel.setSide(mapController.suggestNewChildSide(parentNode, NodeModel.Side.DEFAULT));
              newNodeModel.createID();
              mmapController.insertNode(newNodeModel, parentNode, false);
              recursiveJSONLoop((JSONObject) value, newNodeModel);
          } else if (value instanceof JSONArray) {
              // Array of objects, iterate over each object
              JSONArray jsonArray = (JSONArray) value;

              for (int i = 0; i < jsonArray.length(); i++) {
                  NodeModel newNodeModel = mapController.newNode(Integer.toString(i), parentNode.getMap());
                  newNodeModel.setSide(mapController.suggestNewChildSide(parentNode, NodeModel.Side.DEFAULT));
                  newNodeModel.createID();
                  mmapController.insertNode(newNodeModel, parentNode, false);
                  Object arrayElement = jsonArray.get(i);
                  if (arrayElement instanceof JSONObject) {
                      recursiveJSONLoop((JSONObject) arrayElement, newNodeModel);
                  }
              }
          } else {
              if (key.equals("detail")) {
                mTextController.setDetails(parentNode, value.toString());
              } else if (key.equals("link")) { 
                try {
                  URI uri = new URI(value.toString());
                  mLinkController.setLink(parentNode, new Hyperlink(uri));
                } catch(Exception e) {
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
                MAttributeController.getController().addAttribute(parentNode, newAttribute);
              }
          }
        }
		}

		@Override
		public void mindMapFromJSON(MindMapFromJSONRequest req, StreamObserver<MindMapFromJSONResponse> responseObserver) {
			final MapController mapController = Controller.getCurrentModeController().getMapController();
			final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
			boolean success = false;
			System.out.println("GRPC Freeplane::MindMapFromJSON()");

			// "Refresh" json canvas
			NodeModel rootNode = mapController.getRootNode();
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
			recursiveJSONLoop(obj, rootNode);

			MindMapFromJSONResponse reply = MindMapFromJSONResponse.newBuilder().setSuccess(success).build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}
		private static String bodyText(String html) {
        		// Parse HTML using Jsoup
        		Document document = Jsoup.parse(html);

        		// Extract printable text from HTML
        		Element bodyElement = document.body();
        		String printableText = bodyElement.text();

        		return printableText;
    		}
		private void nodeWalk(Map<String, Object> map, NodeModel node) {
              		NodeModel[] children = node.getChildren().toArray(new NodeModel[] {});
			if (children.length > 0) {
				Map<String, Object> childsMap = new HashMap<>();
                  		for (final NodeModel child : children) {
			  		System.out.println(node.getUserObject() + " -> " + child.getUserObject());
					Map<String, Object> childMap = new HashMap<>();
					childsMap.put(child.getUserObject().toString(), childMap);
                          		nodeWalk(childMap, child);

                  		}
				map.put("children", childsMap);
			}
			final AttributeUtilities atrUtil = new AttributeUtilities();
			if (atrUtil.hasAttributes(node)) {
				Map<String, Object> attr_map = new HashMap<>();
				map.put("attributes", attr_map);
				NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(node);
				for (int i = 0; i < natm.getRowCount(); i++) {
					Attribute attr = natm.getAttribute(i);
					attr_map.put(attr.getName(), attr.getValue().toString());
					System.out.println("Attribute " + attr.getName().toString());
				}
			}
			if (DetailModel.getDetail(node) != null) {
		  	   map.put("detail", bodyText(DetailModel.getDetailText(node)));
			}
			if (NoteModel.getNoteText(node) != null) {
		   	   map.put("note", bodyText(NoteModel.getNoteText(node)));
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
