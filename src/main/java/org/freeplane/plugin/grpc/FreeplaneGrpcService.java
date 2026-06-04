package org.freeplane.plugin.grpc;

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
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.note.NoteModel;
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
    private final ModeController modeController;

    FreeplaneGrpcService() {
        this(null);
    }

    FreeplaneGrpcService(final ModeController modeController) {
        this.modeController = modeController;
    }

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
        GetCurrentNodeResponse reply = GetCurrentNodeResponse.newBuilder()
            .setNodeId("-1")
            .setMapId("-1")
            .setSuccess(false)
            .build();

        // Wait for Freeplane controller to become ready (up to 5 seconds)
        Controller controller = null;
        for (int i = 0; i < 10; i++) {
            controller = Controller.getCurrentController();
            if (controller != null) break;
            try { Thread.sleep(500); } catch (final InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }

        if (controller != null) {
            try {
                final IMapSelection selection = controller.getSelection();
                if (selection != null) {
                    final MapModel map = controller.getMap();
                    final NodeModel currentNode = selection.getSelected();
                    if (currentNode != null) {
                        reply = GetCurrentNodeResponse.newBuilder()
                            .setNodeId(currentNode.getID())
                            .setMapId(map.getTitle())
                            .setSuccess(true)
                            .build();
                    }
                }
            } catch (final Exception e) {
                // Controller may not be fully ready, return failure response
            }
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
        LOG.info("GRPC Freeplane::MindMapFromJSON()");

        // Wait for Freeplane controller to become ready (up to 10 seconds)
        Controller controller = null;
        MapController mapController = null;
        for (int i = 0; i < 20; i++) {
            controller = Controller.getCurrentController();
            if (controller != null) {
                try {
                    mapController = controller.getModeController(MModeController.MODENAME).getMapController();
                    if (mapController != null) break;
                } catch (final Exception e) {
                    // mode controller not ready yet
                }
            }
            try { Thread.sleep(500); } catch (final InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }

        if (controller == null || mapController == null) {
            LOG.warning("GRPC Freeplane::MindMapFromJSON controller not ready after timeout");
            final MindMapFromJSONResponse failReply = MindMapFromJSONResponse.newBuilder().setSuccess(false).build();
            responseObserver.onNext(failReply);
            responseObserver.onCompleted();
            return;
        }

        final MMapController mmapController = (MMapController) mapController;
        final MLinkController mLinkController = (MLinkController) LinkController.getController();
        final MapModel map = controller.getMap();
        boolean success = false;

        // Resolve _fp_import_root_node insert mode to determine the target node FIRST.
        // This must happen before accessing selection or map, which may be null.
        final JSONObject importJson = new JSONObject(req.getJson());
        final String insertMode;
        if (importJson.has(JsonHelper.LEGACY_FP_IMPORT_ROOT)) {
            insertMode = importJson.getString(JsonHelper.LEGACY_FP_IMPORT_ROOT);
        } else {
            insertMode = null;
        }

        NodeModel rootNode = null;
        if ("root".equals(insertMode)) {
            try {
                rootNode = mapController.getRootNode();
            } catch (final NullPointerException e) {
                LOG.log(java.util.logging.Level.WARNING, "Could not get root node: map not loaded");
            }
        } else if (insertMode != null && insertMode.startsWith("ID_")) {
            try {
                final NodeModel pickNode = map.getNodeForID(insertMode);
                rootNode = (pickNode != null) ? pickNode : mapController.getRootNode();
            } catch (final NullPointerException e) {
                LOG.log(java.util.logging.Level.WARNING, "Could not resolve insert mode ID: " + insertMode);
            }
        } else {
            // Default: use selected node or fall back to root
            final IMapSelection selection = controller.getSelection();
            if (selection != null) {
                final NodeModel selected = selection.getSelected();
                rootNode = (selected != null) ? selected : mapController.getRootNode();
            }
        }

        // Final fallback: if still no rootNode, try to get the root
        if (rootNode == null) {
            try {
                rootNode = mapController.getRootNode();
            } catch (final NullPointerException e) {
                LOG.log(java.util.logging.Level.WARNING, "Could not get root node: map not loaded");
            }
        }

        if (rootNode == null) {
            LOG.log(java.util.logging.Level.WARNING, "MindMapFromJSON: no target node available (no selection and no insert mode), returning failure");
            final MindMapFromJSONResponse failReply = MindMapFromJSONResponse.newBuilder().setSuccess(false).build();
            responseObserver.onNext(failReply);
            responseObserver.onCompleted();
            return;
        }

        // Clear existing attributes on the target node
        final AttributeUtilities atrUtil = new AttributeUtilities();
        if (atrUtil.hasAttributes(rootNode)) {
            final NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(rootNode);
            final int rowCount = natm.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                AttributeController.getController().performRemoveRow(rootNode, natm, 0);
            }
        }

        // Import using canonical format with legacy migration.
        jsonHelper.mindMapFromJSON(req.getJson(), rootNode);

        final List<NodeModel> newNodes = NodeUtils.collectSubtreeNodes(rootNode);

        // Notify Freeplane's model that imported nodes have changed so they are visible in subsequent exports.
        // Freeplane's MapController uses nodeChanged() to fire NodeChangeEvent to listeners.
        // commitChanges() does not exist on MapController in Freeplane 1.13.x.
        try {
            mapController.nodeChanged(rootNode);
            for (final NodeModel node : newNodes) {
                mapController.nodeChanged(node);
            }
        } catch (final Exception e) {
            LOG.fine("Could not notify model changes after import: " + e.getMessage());
        }

        LOG.info("childrenCount: " + rootNode.getChildCount() + ", Total nodes: " + newNodes.size());

        // Process relationships (connectors) for all nodes in the imported subtree
        for (final NodeModel node : newNodes) {
            if (atrUtil.hasAttributes(node)) {
                final NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(node);
                for (int i = 0; i < natm.getRowCount(); i++) {
                    final Attribute attr = natm.getAttribute(i);
                    if (attr.getName().equals(JsonHelper.LEGACY_RELATIONSHIP_ATTR)) {
                        final NodeModel sourceNode = node;
                        final String relValue = attr.getValue().toString();
                        final List<Map.Entry<String, String>> pairs = NodeUtils.parseRelationships(relValue);

                        for (final Map.Entry<String, String> pair : pairs) {
                            final String targetId = pair.getKey();
                            final String relType = pair.getValue();
                            // Use target_id directly (canonical format) or fall back to UUID lookup (legacy)
                            NodeModel targetNode = map.getNodeForID(targetId);
                            if (targetNode == null) {
                                // Legacy fallback: try UUID lookup
                                targetNode = NodeUtils.findNodeByAttributeUUID(newNodes, targetId);
                            }
                            if (targetNode != null) {
                                LOG.info("Relationship: " + sourceNode + " --[" + relType + "]--> " + targetNode);
                                final ConnectorModel conn = mLinkController.addConnector(sourceNode, targetNode);
                                conn.setMiddleLabel(relType);
                            } else {
                                LOG.warning("Target node not found for: " + targetId);
                            }
                        }
                    }
                }
            }
        }

        // Clean up _relationship attributes after connector creation to prevent leakage into export
        for (final NodeModel node : newNodes) {
            if (atrUtil.hasAttributes(node)) {
                final NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(node);
                for (int i = natm.getRowCount() - 1; i >= 0; i--) {
                    final Attribute attr = natm.getAttribute(i);
                    if (attr.getName().equals(JsonHelper.LEGACY_RELATIONSHIP_ATTR)) {
                        AttributeController.getController().performRemoveRow(node, natm, i);
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
        LOG.info("GRPC Freeplane::MindMapToJSON()");

        // Wait for Freeplane controller and map to become ready (up to 10 seconds)
        Controller controller = null;
        MapController mapController = null;
        MapModel map = null;
        NodeModel rootNode = null;
        for (int i = 0; i < 20; i++) {
            try {
                controller = Controller.getCurrentController();
                if (controller != null) {
                    try {
                        mapController = controller.getModeController(MModeController.MODENAME).getMapController();
                        if (mapController != null) {
                            map = controller.getMap();
                            if (map != null) {
                                rootNode = map.getRootNode();
                                if (rootNode != null) {
                                    LOG.info("GRPC Freeplane::MindMapToJSON controller and map ready after " + (i+1) + " tries");
                                    break;
                                }
                            }
                        }
                    } catch (final Exception e) {
                        LOG.log(java.util.logging.Level.FINE, "GRPC Freeplane::MindMapToJSON mode controller not ready: " + e.getMessage());
                    }
                }
            } catch (final Exception e) {
                LOG.log(java.util.logging.Level.FINE, "GRPC Freeplane::MindMapToJSON getCurrentController failed: " + e.getMessage());
            }
            try { Thread.sleep(500); } catch (final InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }

        if (controller == null || mapController == null || map == null || rootNode == null) {
            LOG.warning("GRPC Freeplane::MindMapToJSON controller or map not ready after timeout (controller=" + (controller != null ? "not-null" : "null") + ", mapController=" + (mapController != null ? "not-null" : "null") + ", map=" + (map != null ? "not-null" : "null") + ", rootNode=" + (rootNode != null ? "not-null" : "null") + ")");
            final MindMapToJSONResponse failReply = MindMapToJSONResponse.newBuilder()
                .setSuccess(false)
                .setJson("")
                .build();
            responseObserver.onNext(failReply);
            responseObserver.onCompleted();
            return;
        }

        // Use LinkedHashMap for deterministic field ordering in canonical JSON
        final Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            jsonHelper.nodeWalk(result, rootNode);
        } catch (final Exception e) {
            LOG.warning("GRPC Freeplane::MindMapToJSON nodeWalk failed: " + e.getMessage());
            final MindMapToJSONResponse failReply = MindMapToJSONResponse.newBuilder()
                .setSuccess(false)
                .setJson("")
                .build();
            responseObserver.onNext(failReply);
            responseObserver.onCompleted();
            return;
        }
        final MindMapToJSONResponse reply = MindMapToJSONResponse.newBuilder()
            .setSuccess(true)
            .setJson(JsonHelper.toJson(result))
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

    // --- Group A: Node Inspection ---

    @Override
    public void getNodeText(final GetNodeTextRequest req,
                            final StreamObserver<GetNodeTextResponse> responseObserver) {
        final MapModel map = Controller.getCurrentController().getMap();
        LOG.info("GRPC Freeplane::getNodeText(node_id: " + req.getNodeId() + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        GetNodeTextResponse reply;

        if (targetNode != null) {
            final String text = targetNode.getText();
            reply = GetNodeTextResponse.newBuilder()
                .setSuccess(true)
                .setNodeId(targetNode.getID())
                .setText(text)
                .build();
        } else {
            LOG.warning("GRPC Freeplane::getNodeText node not found: " + req.getNodeId());
            reply = GetNodeTextResponse.newBuilder()
                .setSuccess(false)
                .setNodeId(req.getNodeId())
                .setErrorMessage("Node not found: " + req.getNodeId())
                .build();
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void getParentNode(final GetParentNodeRequest req,
                              final StreamObserver<GetParentNodeResponse> responseObserver) {
        final MapModel map = Controller.getCurrentController().getMap();
        LOG.info("GRPC Freeplane::getParentNode(node_id: " + req.getNodeId() + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        GetParentNodeResponse reply;

        if (targetNode != null) {
            final NodeModel parentNode = targetNode.getParentNode();
            if (parentNode != null) {
                reply = GetParentNodeResponse.newBuilder()
                    .setSuccess(true)
                    .setNodeId(targetNode.getID())
                    .setParentNodeId(parentNode.getID())
                    .setParentNodeText(parentNode.getText())
                    .build();
            } else {
                // targetNode is the root or has no parent
                reply = GetParentNodeResponse.newBuilder()
                    .setSuccess(true)
                    .setNodeId(targetNode.getID())
                    .setParentNodeId("")
                    .setParentNodeText("")
                    .build();
            }
        } else {
            LOG.warning("GRPC Freeplane::getParentNode node not found: " + req.getNodeId());
            reply = GetParentNodeResponse.newBuilder()
                .setSuccess(false)
                .setNodeId(req.getNodeId())
                .setErrorMessage("Node not found: " + req.getNodeId())
                .build();
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void listChildNodes(final ListChildNodesRequest req,
                               final StreamObserver<ListChildNodesResponse> responseObserver) {
        final MapModel map = Controller.getCurrentController().getMap();
        LOG.info("GRPC Freeplane::listChildNodes(node_id: " + req.getNodeId() + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        ListChildNodesResponse.Builder replyBuilder = ListChildNodesResponse.newBuilder().setSuccess(false);

        if (targetNode != null) {
            int childCount = targetNode.getChildCount();
            LOG.info("GRPC Freeplane::listChildNodes found " + childCount + " children");
            for (NodeModel child : targetNode.getChildren()) {
                replyBuilder.addChildren(ChildNodeInfo.newBuilder()
                    .setNodeId(child.getID())
                    .setText(child.getText())
                    .build());
            }
            replyBuilder.setSuccess(true);
        } else {
            LOG.warning("GRPC Freeplane::listChildNodes node not found: " + req.getNodeId());
            replyBuilder.setErrorMessage("Node not found: " + req.getNodeId());
        }

        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getNodeNote(final GetNodeNoteRequest req,
                            final StreamObserver<GetNodeNoteResponse> responseObserver) {
        final MapModel map = Controller.getCurrentController().getMap();
        LOG.info("GRPC Freeplane::getNodeNote(node_id: " + req.getNodeId() + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        GetNodeNoteResponse reply;

        if (targetNode != null) {
            org.freeplane.features.note.NoteModel note = org.freeplane.features.note.NoteModel.getNote(targetNode);
            if (note != null) {
                String noteText = org.freeplane.features.note.NoteModel.getNoteText(targetNode);
                if (noteText == null) {
                    noteText = "";
                }
                reply = GetNodeNoteResponse.newBuilder()
                    .setSuccess(true)
                    .setNodeId(targetNode.getID())
                    .setNote(noteText)
                    .setHasNote(true)
                    .build();
            } else {
                reply = GetNodeNoteResponse.newBuilder()
                    .setSuccess(true)
                    .setNodeId(targetNode.getID())
                    .setNote("")
                    .setHasNote(false)
                    .build();
            }
        } else {
            LOG.warning("GRPC Freeplane::getNodeNote node not found: " + req.getNodeId());
            reply = GetNodeNoteResponse.newBuilder()
                .setSuccess(false)
                .setNodeId(req.getNodeId())
                .setErrorMessage("Node not found: " + req.getNodeId())
                .build();
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void getNodeLink(final GetNodeLinkRequest req,
                            final StreamObserver<GetNodeLinkResponse> responseObserver) {
        final MapModel map = Controller.getCurrentController().getMap();
        LOG.info("GRPC Freeplane::getNodeLink(node_id: " + req.getNodeId() + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        GetNodeLinkResponse reply;

        if (targetNode != null) {
            Hyperlink hyperlink = NodeLinks.getLink(targetNode);
            if (hyperlink != null && hyperlink.getUri() != null) {
                reply = GetNodeLinkResponse.newBuilder()
                    .setSuccess(true)
                    .setNodeId(targetNode.getID())
                    .setLink(hyperlink.getUri().toString())
                    .setHasLink(true)
                    .build();
            } else {
                reply = GetNodeLinkResponse.newBuilder()
                    .setSuccess(true)
                    .setNodeId(targetNode.getID())
                    .setLink("")
                    .setHasLink(false)
                    .build();
            }
        } else {
            LOG.warning("GRPC Freeplane::getNodeLink node not found: " + req.getNodeId());
            reply = GetNodeLinkResponse.newBuilder()
                .setSuccess(false)
                .setNodeId(req.getNodeId())
                .setErrorMessage("Node not found: " + req.getNodeId())
                .build();
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    // --- Group B: Node Manipulation ---

    @Override
    public void setNodeText(final SetNodeTextRequest req,
                            final StreamObserver<SetNodeTextResponse> responseObserver) {
        final MapModel map = Controller.getCurrentController().getMap();
        LOG.info("GRPC Freeplane::setNodeText(node_id: " + req.getNodeId() + ", text: " + req.getText() + ")");

        final NodeModel targetNode = map.getNodeForID(req.getNodeId());
        SetNodeTextResponse reply;

        if (targetNode != null) {
            targetNode.setText(req.getText());
            reply = SetNodeTextResponse.newBuilder()
                .setSuccess(true)
                .setNodeId(targetNode.getID())
                .build();
        } else {
            LOG.warning("GRPC Freeplane::setNodeText node not found: " + req.getNodeId());
            reply = SetNodeTextResponse.newBuilder()
                .setSuccess(false)
                .setNodeId(req.getNodeId())
                .setErrorMessage("Node not found: " + req.getNodeId())
                .build();
        }

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void moveNode(final MoveNodeRequest req,
                         final StreamObserver<MoveNodeResponse> responseObserver) {
        final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
        final MapModel map = Controller.getCurrentController().getMap();
        LOG.info("GRPC Freeplane::moveNode(node_id: " + req.getNodeId() + ", new_parent_node_id: " + req.getNewParentNodeId() + ")");

        final NodeModel nodeToMove = map.getNodeForID(req.getNodeId());
        final NodeModel newParent = map.getNodeForID(req.getNewParentNodeId());
        String errorMessage = null;

        if (nodeToMove == null) {
            LOG.warning("GRPC Freeplane::moveNode node to move not found: " + req.getNodeId());
            errorMessage = "Node not found: " + req.getNodeId();
        } else if (newParent == null) {
            LOG.warning("GRPC Freeplane::moveNode new parent not found: " + req.getNewParentNodeId());
            errorMessage = "New parent node not found: " + req.getNewParentNodeId();
        } else if (nodeToMove == newParent) {
            LOG.warning("GRPC Freeplane::moveNode node cannot be moved under itself");
            errorMessage = "Node cannot be moved under itself";
        } else if (req.getNodeId().equals(req.getNewParentNodeId())) {
            LOG.warning("GRPC Freeplane::moveNode node ID equals new parent node ID");
            errorMessage = "Node cannot be moved under itself";
        } else {
            // Check if newParent is already a descendant of nodeToMove (would create a cycle)
            NodeModel check = newParent;
            boolean isDescendant = false;
            while (check != null) {
                if (check == nodeToMove) {
                    isDescendant = true;
                    break;
                }
                check = check.getParentNode();
            }

            if (isDescendant) {
                LOG.warning("GRPC Freeplane::moveNode would create a cycle; new parent is a descendant of node");
                errorMessage = "Cannot move node: new parent is a descendant of the node";
            } else {
                // Use Freeplane's moveNodeAndItsClones to preserve full subtree and metadata
                mmapController.moveNodeAndItsClones(nodeToMove, newParent, newParent.getChildCount());
            }
        }

        final MoveNodeResponse reply = errorMessage != null
            ? MoveNodeResponse.newBuilder().setSuccess(false).setErrorMessage(errorMessage).build()
            : MoveNodeResponse.newBuilder().setSuccess(true).build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
