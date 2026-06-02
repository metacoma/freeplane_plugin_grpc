package org.freeplane.plugin.grpc;

import com.google.gson.Gson;
import io.grpc.stub.StreamObserver;
import org.freeplane.core.util.Hyperlink;
import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.attribute.mindmapmode.AttributeUtilities;
import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.link.ConnectorModel;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.features.url.mindmapmode.MFileManager;
import org.freeplane.features.ui.ViewController;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * gRPC service implementation that exposes Freeplane mindmap operations.
 * Extracted from the original GrpcRegistration.FreeplaneImpl inner class.
 */
class FreeplaneGrpcService extends FreeplaneGrpc.FreeplaneImplBase {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(FreeplaneGrpcService.class.getName());

    private final JsonHelper jsonHelper = new JsonHelper();

    @Override
    public void createChild(final CreateChildRequest req, final StreamObserver<CreateChildResponse> responseObserver) {
        final MapController mapController = Controller.getCurrentModeController().getMapController();
        final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
        final MapModel map = Controller.getCurrentController().getMap();
        final String parentNodeId = req.getParentNodeId();
        final String newNodeName = req.getName();
        CreateChildResponse reply;

        LOG.info("GRPC Freeplane::createChild(name: " + req.getName() + ", parent_node_id: " + req.getParentNodeId() + ")");
        final NodeModel rootNode = (parentNodeId != null && parentNodeId.length() > 0)
            ? map.getNodeForID(parentNodeId)
            : mapController.getRootNode();
        if (parentNodeId != null && parentNodeId.length() > 0) {
            LOG.info("parentNodeId is not null");
        } else {
            LOG.info("parentNodeId == null");
        }
        if (rootNode != null) {
            final NodeModel newNodeModel = mmapController.newNode(newNodeName, rootNode.getMap());
            newNodeModel.setSide(mapController.suggestNewChildSide(rootNode, NodeModel.Side.DEFAULT));
            newNodeModel.createID();
            mmapController.insertNode(newNodeModel, rootNode, 0);
            reply = CreateChildResponse.newBuilder()
                .setNodeId(newNodeModel.getID())
                .setNodeText(newNodeModel.getText())
                .build();
        } else {
            LOG.warning("GRPC Freeplane::createChild root node not found for parent: " + req.getParentNodeId());
            reply = CreateChildResponse.newBuilder().setNodeId("-1").setNodeText("").build();
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteChild(final DeleteChildRequest req, final StreamObserver<DeleteChildResponse> responseObserver) {
        LOG.info("GRPC Freeplane::deleteChild(" + req.getNodeId() + ")");
        final MapController mapController = Controller.getCurrentModeController().getMapController();
        final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
        final MapModel map = Controller.getCurrentController().getMap();
        boolean success = false;

        final NodeModel nodeToDelete = map.getNodeForID(req.getNodeId());
        if (nodeToDelete != null) {
            success = true;
            mmapController.deleteNode(nodeToDelete);
        }

        final DeleteChildResponse reply = DeleteChildResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void nodeAttributeAdd(final NodeAttributeAddRequest req,
                                 final StreamObserver<NodeAttributeAddResponse> responseObserver) {
        LOG.info("GRPC Freeplane::nodeAttributeAdd(node_id: " + req.getNodeId()
            + ", name: " + req.getAttributeName()
            + ", value: " + req.getAttributeValue() + ")");
        final MapModel map = Controller.getCurrentController().getMap();
        boolean success = false;

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        if (targetNode != null) {
            success = true;
            final Attribute newAttribute = new Attribute(req.getAttributeName(), req.getAttributeValue());
            MAttributeController.getController().addAttribute(targetNode, newAttribute);
        }

        final NodeAttributeAddResponse reply = NodeAttributeAddResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void nodeLinkSet(final NodeLinkSetRequest req, final StreamObserver<NodeLinkSetResponse> responseObserver) {
        boolean success = false;
        final MLinkController mLinkController = (MLinkController) LinkController.getController();
        final MapModel map = Controller.getCurrentController().getMap();
        LOG.info("GRPC Freeplane::nodeLinkSet(node_id: " + req.getNodeId() + ", link: " + req.getLink() + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        if (targetNode != null) {
            try {
                final URI uri = new URI(req.getLink());
                mLinkController.setLink(targetNode, new Hyperlink(uri));
                success = true;
            } catch (final Exception e) {
                LOG.warning("GRPC Freeplane::nodeLinkSet failed: " + e.getMessage());
            }
        }

        final NodeLinkSetResponse reply = NodeLinkSetResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void nodeDetailsSet(final NodeDetailsSetRequest req,
                               final StreamObserver<NodeDetailsSetResponse> responseObserver) {
        boolean success = false;
        final MapModel map = Controller.getCurrentController().getMap();
        final MTextController mTextController = (MTextController) TextController.getController();
        LOG.info("GRPC Freeplane::nodeDetailsSet(node_id: " + req.getNodeId() + ", details: " + req.getDetails() + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        if (targetNode != null) {
            success = true;
            mTextController.setDetails(targetNode, req.getDetails());
        }

        final NodeDetailsSetResponse reply = NodeDetailsSetResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void nodeNoteSet(final NodeNoteSetRequest req,
                            final StreamObserver<NodeNoteSetResponse> responseObserver) {
        boolean success = false;
        final MNoteController mNoteController = MNoteController.getController();
        final MapModel map = Controller.getCurrentController().getMap();
        final MTextController mTextController = (MTextController) TextController.getController();
        LOG.info("GRPC Freeplane::nodeNoteSet(node_id: " + req.getNodeId() + ", note: " + req.getNote() + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        if (targetNode != null) {
            success = true;
            mNoteController.setNoteText(targetNode, req.getNote());
        }

        final NodeNoteSetResponse reply = NodeNoteSetResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void nodeTagSet(final NodeTagSetRequest req,
                           final StreamObserver<NodeTagSetResponse> responseObserver) {
        boolean success = false;
        final MapModel map = Controller.getCurrentController().getMap();
        final MIconController mIconController = (MIconController) IconController.getController();

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        if (targetNode != null) {
            final List<Tag> tagList = req.getTagsList()
                .stream()
                .map(Tag::new)
                .collect(java.util.stream.Collectors.toList());

            mIconController.setTags(targetNode, tagList, false);
            success = true;
        }

        final NodeTagSetResponse reply = NodeTagSetResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void nodeTagAdd(final NodeTagAddRequest req,
                           final StreamObserver<NodeTagAddResponse> responseObserver) {
        boolean success = false;
        final MapModel map = Controller.getCurrentController().getMap();
        final MIconController mIconController = (MIconController) IconController.getController();
        LOG.info("GRPC Freeplane::nodeTagAdd(node_id: " + req.getNodeId() + ", new tags: " + req.getTagsList() + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());

        if (targetNode != null) {
            // Get current tags
            final List<Tag> currentTags = mIconController.getTags(targetNode);

            // Convert current tag contents to a set for uniqueness
            final java.util.Set<String> tagContentSet = currentTags.stream()
                .map(Tag::getContent)
                .collect(java.util.stream.Collectors.toSet());

            // Add new tags, avoiding duplicates
            for (final String newTagContent : req.getTagsList()) {
                tagContentSet.add(newTagContent);
            }

            // Convert back to List<Tag>
            final List<Tag> mergedTags = tagContentSet.stream()
                .map(Tag::new)
                .collect(java.util.stream.Collectors.toList());

            // Set merged tags
            mIconController.setTags(targetNode, mergedTags, true);

            success = true;
        }

        final NodeTagAddResponse reply = NodeTagAddResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void nodeConnect(final NodeConnectRequest req,
                            final StreamObserver<NodeConnectResponse> responseObserver) {
        boolean success = false;
        final MLinkController mLinkController = (MLinkController) LinkController.getController();
        final MapModel map = Controller.getCurrentController().getMap();
        final NodeModel sourceNode = map.getNodeForID(req.getSourceNodeId());
        final NodeModel targetNode = map.getNodeForID(req.getTargetNodeId());
        final String relationship = req.getRelationship().toString();

        LOG.info("GRPC Freeplane::nodeConnect(source_node_id: " + req.getSourceNodeId()
            + ", target_node_id: " + req.getTargetNodeId()
            + ", relationship: " + relationship + ")");

        if (sourceNode != null && targetNode != null) {
            final ConnectorModel conn = mLinkController.addConnector(sourceNode, targetNode);
            if (conn != null) {
                if (relationship != null) {
                    conn.setMiddleLabel(relationship);
                }
                success = true;
            } else {
                LOG.warning("GRPC Freeplane::nodeConnect failed, connector is null");
            }
        }
        final NodeConnectResponse reply = NodeConnectResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void nodeAddIcon(final NodeAddIconRequest req,
                            final StreamObserver<NodeAddIconResponse> responseObserver) {
        boolean success = false;
        final MLinkController mLinkController = (MLinkController) LinkController.getController();
        final MapModel map = Controller.getCurrentController().getMap();
        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        final String iconName = req.getIconName().toString();

        LOG.info("GRPC Freeplane::nodeAddIcon(node_id: " + req.getNodeId() + ", icon_name: " + iconName + ")");

        if (targetNode != null) {
            final MindIcon icon = IconStoreFactory.ICON_STORE.getMindIcon(iconName);
            if (icon != null) {
                targetNode.addIcon(icon);
                success = true;
            } else {
                LOG.warning("GRPC Freeplane::nodeAddIcon failed, icon not found: " + iconName);
            }
        }
        final NodeAddIconResponse reply = NodeAddIconResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void groovy(final GroovyRequest req, final StreamObserver<GroovyResponse> responseObserver) {
        final String groovyCode = req.getGroovyCode();
        if (groovyCode == null || groovyCode.isEmpty()) {
            final GroovyResponse emptyReply = GroovyResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage("Groovy code request is empty")
                .build();
            responseObserver.onNext(emptyReply);
            responseObserver.onCompleted();
            return;
        }

        try {
            // Get current controller and selected node
            final Controller controller = Controller.getCurrentController();
            final org.freeplane.features.map.NodeModel node =
                controller != null
                    ? controller.getSelection().getSelected()
                    : null;

            if (node == null) {
                final GroovyResponse noNodeReply = GroovyResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage("No active Freeplane node selected")
                    .build();
                responseObserver.onNext(noNodeReply);
                responseObserver.onCompleted();
                return;
            }

            // Use permissive scripting permissions.
            // Rationale: gRPC worker threads cannot show UI permission dialogs.
            // gRPC callers are assumed trusted (local network or authenticated).
            final org.freeplane.plugin.script.ScriptingPermissions permissions =
                org.freeplane.plugin.script.ScriptingPermissions.getPermissiveScriptingPermissions();

            // Execute the script via Freeplane's ScriptingEngine.
            // This creates a GroovyScript, wraps it in ScriptRunner, and executes it.
            final Object result = org.freeplane.plugin.script.ScriptingEngine.executeScript(
                node, groovyCode, permissions);

            final GroovyResponse reply = GroovyResponse.newBuilder()
                .setSuccess(true)
                .setResult(result != null ? result.toString() : "")
                .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

        } catch (final org.freeplane.plugin.script.ExecuteScriptException e) {
            // Script execution failed — extract cause message
            final Throwable cause = e.getCause();
            final String errorMsg = cause != null ? cause.getClass().getName() + ": " + cause.getMessage()
                                                   : e.getMessage();
            LOG.warning("GRPC Freeplane::groovy execution failed: " + (errorMsg != null ? errorMsg : "Script execution failed"));
            final GroovyResponse errorReply = GroovyResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(errorMsg != null ? errorMsg : "Script execution failed")
                .build();
            responseObserver.onNext(errorReply);
            responseObserver.onCompleted();

        } catch (final Exception e) {
            // Unexpected error (e.g., no controller, threading issues)
            LOG.severe("GRPC Freeplane::groovy unexpected error: " + e.getMessage());
            final GroovyResponse errorReply = GroovyResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getClass().getName() + ": " + e.getMessage())
                .build();
            responseObserver.onNext(errorReply);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void nodeColorSet(final NodeColorSetRequest req,
                             final StreamObserver<NodeColorSetResponse> responseObserver) {
        boolean success = false;
        final MNodeStyleController mNodeStyleController = (MNodeStyleController) NodeStyleController.getController();
        final MapModel map = Controller.getCurrentController().getMap();
        final Integer red = req.getRed();
        final Integer green = req.getGreen();
        final Integer blue = req.getBlue();
        final Integer alpha = req.getAlpha();

        LOG.info("GRPC Freeplane::nodeColorSet(node_id: " + req.getNodeId()
            + ", color: " + red + " " + green + " " + blue + " " + alpha + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        if (targetNode != null) {
            try {
                success = true;
                mNodeStyleController.setColor(targetNode, new Color(red, green, blue, alpha));
            } catch (final Exception e) {
                LOG.warning("GRPC Freeplane::nodeColorSet failed: " + e.getMessage());
                success = false;
            }
        }

        final NodeColorSetResponse reply = NodeColorSetResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void nodeBackgroundColorSet(final NodeBackgroundColorSetRequest req,
                                       final StreamObserver<NodeBackgroundColorSetResponse> responseObserver) {
        boolean success = false;
        final MNodeStyleController mNodeStyleController = (MNodeStyleController) NodeStyleController.getController();
        final MapModel map = Controller.getCurrentController().getMap();
        final Integer red = req.getRed();
        final Integer green = req.getGreen();
        final Integer blue = req.getBlue();
        final Integer alpha = req.getAlpha();

        LOG.info("GRPC Freeplane::nodeBackgroundColorSet(node_id: " + req.getNodeId()
            + ", color: " + red + " " + green + " " + blue + " " + alpha + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        if (targetNode != null) {
            try {
                success = true;
                mNodeStyleController.setBackgroundColor(targetNode, new Color(red, green, blue, alpha));
            } catch (final Exception e) {
                LOG.warning("GRPC Freeplane::nodeBackgroundColorSet failed: " + e.getMessage());
                success = false;
            }
        }

        final NodeBackgroundColorSetResponse reply = NodeBackgroundColorSetResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void statusInfoSet(final StatusInfoSetRequest req,
                              final StreamObserver<StatusInfoSetResponse> responseObserver) {
        boolean success = false;
        final String statusInfo = req.getStatusInfo();

        if (statusInfo != null && !statusInfo.isEmpty()) {
            success = true;
            final ViewController viewController = Controller.getCurrentController().getViewController();
            viewController.out(statusInfo);
        }

        final StatusInfoSetResponse reply = StatusInfoSetResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void getCurrentNode(final GetCurrentNodeRequest req,
                               final StreamObserver<GetCurrentNodeResponse> responseObserver) {
        final IMapSelection selection = Controller.getCurrentController().getSelection();
        final MapModel map = Controller.getCurrentController().getMap();
        final NodeModel currentNode = selection.getSelected();
        final GetCurrentNodeResponse reply;
        if (currentNode != null) {
            reply = GetCurrentNodeResponse.newBuilder()
                .setNodeId(currentNode.getID())
                .setMapId(map.getTitle())
                .setSuccess(true)
                .build();
        } else {
            reply = GetCurrentNodeResponse.newBuilder()
                .setNodeId("-1")
                .setMapId("-1")
                .setSuccess(false)
                .build();
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void openMap(final OpenMapRequest req, final StreamObserver<OpenMapResponse> responseObserver) {
        final Controller controller = Controller.getCurrentController();
        final ModeController modeController = controller.getModeController(MModeController.MODENAME);
        final MapController mapController = Controller.getCurrentModeController().getMapController();
        final MFileManager fileManager = MFileManager.getController(modeController);
        boolean success = false;
        final String mapFilePath = req.getFilePath();
        URL mindmapURL = null;

        LOG.info("GRPC Freeplane::openMap(mapFilePath: " + mapFilePath + ")");

        if (mapFilePath != null) {
            String path = System.getProperty("user.home") + "/mindmaps/" + mapFilePath;
            final File file = new File(path);

            try {
                mindmapURL = file.toURI().toURL();
                mapController.openMap(mindmapURL);
                LOG.info("GRPC Freeplane::openMap(URI: " + path + ")");
                success = true;

            } catch (final Exception e) {
                LOG.warning("File not found, creating new map: " + mapFilePath);
                final Path dirPath = file.getParentFile().toPath();

                try {
                    Files.createDirectories(dirPath);

                    final MapModel newMapModel = mapController.newMap();
                    newMapModel.setURL(mindmapURL);
                    final NodeModel rootNode = newMapModel.getRootNode();

                    rootNode.setText(mapFilePath);
                    fileManager.save(newMapModel);
                    success = true;
                } catch (final IOException e2) {
                    LOG.warning("Failed to create new map: " + e2.getMessage());
                    success = false;
                }

            }
        }

        responseObserver.onNext(OpenMapResponse.newBuilder().setSuccess(success).build());
        responseObserver.onCompleted();
    }

    @Override
    public void textFSM(final TextFSMRequest req, final StreamObserver<TextFSMResponse> responseObserver) {
        final MapController mapController = Controller.getCurrentModeController().getMapController();
        final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
        boolean success = false;
        int idx = 0;
        LOG.info("GRPC Freeplane::TextFSM()");

        final JSONObject obj = new JSONObject(req.getJson());

        final String indexName = obj.getString("index");

        final JSONArray header = obj.getJSONArray("header");
        LOG.info("Header: " + header.toString());

        for (int i = 0; i < header.length(); i++) {
            final String headerElement = header.getString(i);
            if (headerElement.equals(indexName)) {
                idx = i;
                LOG.info("Match found at index " + i);
                break;
            }
        }

        final JSONArray result = obj.getJSONArray("result");
        LOG.info("Result: " + result.toString());

        final NodeModel rootNode = mapController.getRootNode();

        for (int i = 0; i < result.length(); i++) {
            final JSONArray resultElement = result.getJSONArray(i);

            final NodeModel newNodeModel = mmapController.newNode(resultElement.get(idx).toString(), rootNode.getMap());
            newNodeModel.setSide(mapController.suggestNewChildSide(rootNode, NodeModel.Side.DEFAULT));
            newNodeModel.createID();
            mmapController.insertNode(newNodeModel, rootNode, 0);

            for (int j = 0; j < resultElement.length(); j++) {
                final Attribute newAttribute = new Attribute(header.getString(j), resultElement.get(j).toString());
                try {
                    MAttributeController.getController().addAttribute(newNodeModel, newAttribute);
                } catch (final Exception e) {
                    // sometimes this happens
                    // https://github.com/metacoma/freeplane_plugin_grpc/issues/1
                }
            }
        }

        success = true;
        final TextFSMResponse reply = TextFSMResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void mindMapFromJSON(final MindMapFromJSONRequest req,
                                final StreamObserver<MindMapFromJSONResponse> responseObserver) {
        final MapController mapController = Controller.getCurrentModeController().getMapController();
        final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
        final MLinkController mLinkController = (MLinkController) LinkController.getController();
        final MapModel map = Controller.getCurrentController().getMap();
        boolean success = false;
        final AttributeController attributeController = AttributeController.getController();

        LOG.info("GRPC Freeplane::MindMapFromJSON()");
        final String insert_mode_key = "_fp_import_root_node";

        final IMapSelection selection = Controller.getCurrentController().getSelection();
        NodeModel rootNode = selection.getSelected();

        // "Refresh" json canvas
        // mmapController.deleteNodes(rootNode.getChildren());

        final AttributeUtilities atrUtil = new AttributeUtilities();
        if (atrUtil.hasAttributes(rootNode)) {
            final NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(rootNode);
            final int rowCount = natm.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                AttributeController.getController().performRemoveRow(rootNode, natm, 0);
            }
        }

        final JSONObject obj = new JSONObject(req.getJson());
        if (obj.has(insert_mode_key)) {
            final String mode = obj.getString(insert_mode_key);

            if ("root".equals(mode)) {
                rootNode = mapController.getRootNode();
            }

            if (mode.startsWith("ID_")) {
                final NodeModel pickNode = map.getNodeForID(mode);
                if (pickNode != null) {
                    rootNode = pickNode;
                }
            }

            obj.remove(insert_mode_key);
        }

        jsonHelper.recursiveJSONLoop(obj, rootNode);

        final List<NodeModel> newNodes = NodeUtils.collectSubtreeNodes(rootNode);

        LOG.info("childrenCount: " + rootNode.getChildCount() + ", Total nodes: " + newNodes.size());

        for (final NodeModel node : newNodes) {
            if (atrUtil.hasAttributes(node)) {
                final NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(node);
                for (int i = 0; i < natm.getRowCount(); i++) {
                    final Attribute attr = natm.getAttribute(i);
                    if (attr.getName().equals("_relationship")) {
                        final NodeModel sourceNode = node;
                        final String relValue = attr.getValue().toString();
                        final List<Map.Entry<String, String>> pairs = NodeUtils.parseRelationships(relValue);

                        for (final Map.Entry<String, String> pair : pairs) {
                            final String uuid = pair.getKey();
                            final String relType = pair.getValue();
                            final NodeModel targetNode = NodeUtils.findNodeByAttributeUUID(newNodes, uuid);
                            if (targetNode != null) {
                                LOG.info("Relationship: " + node + " --[" + relType + "]--> " + targetNode);
                                final ConnectorModel conn = mLinkController.addConnector(sourceNode, targetNode);
                                conn.setMiddleLabel(relType);
                            } else {
                                LOG.warning("UUID not found: " + uuid);
                            }
                        }
                    }
                }
            }
        }

        success = true;
        final MindMapFromJSONResponse reply = MindMapFromJSONResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void mindMapToJSON(final MindMapToJSONRequest req,
                              final StreamObserver<MindMapToJSONResponse> responseObserver) {
        final MapController mapController = Controller.getCurrentModeController().getMapController();
        final NodeModel rootNode = mapController.getRootNode();
        final Map<String, Object> result = new java.util.HashMap<>();

        final Gson gson = new Gson();

        jsonHelper.nodeWalk(result, rootNode);
        LOG.info("GRPC Freeplane::MindMapToJSON()");
        final MindMapToJSONResponse reply = MindMapToJSONResponse.newBuilder()
            .setSuccess(true)
            .setJson(gson.toJson(result))
            .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void focusNode(final FocusNodeRequest req, final StreamObserver<FocusNodeResponse> responseObserver) {
        final Controller controller = Controller.getCurrentController();
        final MapModel map = Controller.getCurrentController().getMap();
        final MapController mapController = Controller.getCurrentModeController().getMapController();
        final IMapSelection selection = Controller.getCurrentController().getSelection();
        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        boolean success = false;

        LOG.info("GRPC Freeplane::focusNode(node_id: " + req.getNodeId() + ")");

        if (targetNode != null) {
            selection.selectAsTheOnlyOneSelected(targetNode);
            // targetNode.setChildNodeSidesAsNow();
            // controller.getSelection().scrollNodeToCenter(targetNode, false);
            controller.getMapViewManager().setViewRoot(targetNode);
            success = true;
        } else {
            LOG.warning("GRPC Freeplane::focusNode failed, targetNode == null for node_id: " + req.getNodeId());
        }

        final FocusNodeResponse reply = FocusNodeResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
