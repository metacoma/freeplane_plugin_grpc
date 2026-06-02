package org.freeplane.plugin.grpc;

import org.freeplane.core.util.Hyperlink;
import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.attribute.mindmapmode.AttributeUtilities;
import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.icon.mindmapmode.MIconController;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.awt.Color;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper methods for JSON ↔ mindmap node conversion.
 */
public final class JsonHelper {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(JsonHelper.class.getName());

    JsonHelper() {
        // Prevent instantiation from outside this package
    }

    /**
     * Recursively processes a JSONObject to create mindmap nodes and apply attributes.
     * Mirrors the original FreeplaneImpl.recursiveJSONLoop behavior exactly.
     */
    void recursiveJSONLoop(JSONObject jsonObject, NodeModel parentNode) {
        final MapController mapController = Controller.getCurrentModeController().getMapController();
        final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
        final MTextController mTextController = (MTextController) TextController.getController();
        final MIconController mIconController = (MIconController) IconController.getController();
        final MLinkController mLinkController = (MLinkController) LinkController.getController();
        final MNodeStyleController mNodeStyleController = (MNodeStyleController) NodeStyleController.getController();
        final MNoteController mNoteController = MNoteController.getController();
        final org.freeplane.features.attribute.mindmapmode.MAttributeController mAttributeController =
                org.freeplane.features.attribute.mindmapmode.MAttributeController.getController();

        for (final String key : jsonObject.keySet()) {
            final Object value = jsonObject.get(key);

            LOG.fine("GRPC recursiveJSONLoop, key " + key);

            if (value instanceof JSONObject) {
                // Nested object, recursively iterate
                final NodeModel newNodeModel = mmapController.newNode(key, parentNode.getMap());
                newNodeModel.setSide(mapController.suggestNewChildSide(parentNode, NodeModel.Side.DEFAULT));
                newNodeModel.createID();
                mmapController.insertNode(newNodeModel, parentNode, 0);
                recursiveJSONLoop((JSONObject) value, newNodeModel);
            } else if (value instanceof JSONArray) {
                // Array of objects, iterate over each object
                final JSONArray jsonArray = (JSONArray) value;

                for (int i = 0; i < jsonArray.length(); i++) {
                    final NodeModel newNodeModel = mmapController.newNode(Integer.toString(i), parentNode.getMap());
                    newNodeModel.setSide(mapController.suggestNewChildSide(parentNode, NodeModel.Side.DEFAULT));
                    newNodeModel.createID();
                    mmapController.insertNode(newNodeModel, parentNode, 0);
                    final Object arrayElement = jsonArray.get(i);
                    if (arrayElement instanceof JSONObject) {
                        recursiveJSONLoop((JSONObject) arrayElement, newNodeModel);
                    }
                }
            } else if (key.equals("icons")) {
                final List<String> iconList = Arrays.stream(value.toString().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

                for (final String iconName : iconList) {
                    final MindIcon icon = IconStoreFactory.ICON_STORE.getMindIcon(iconName);
                    if (icon != null) {
                        parentNode.addIcon(icon);
                    }
                }
            } else if (key.equals("tags")) {
                final List<Tag> tagList = Arrays.stream(value.toString().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Tag::new)
                    .collect(Collectors.toList());

                mIconController.setTags(parentNode, tagList, false);
            } else if (key.equals("detail")) {
                mTextController.setDetails(parentNode, value.toString());
            } else if (key.equals("link")) {
                try {
                    final URI uri = new URI(value.toString());
                    mLinkController.setLink(parentNode, new org.freeplane.core.util.Hyperlink(uri));
                } catch (final Exception e) {
                    LOG.warning("Failed to set link: " + e.getMessage());
                }
            } else if (key.equals("note")) {
                mNoteController.setNoteText(parentNode, value.toString());
            } else if (key.equals("color")) {
                final int[] rgba = new int[4];
                final Pattern pattern = Pattern.compile("(\\d+)");
                final Matcher matcher = pattern.matcher(value.toString());

                int index = 0;
                while (matcher.find() && index < 4) {
                    rgba[index++] = Integer.parseInt(matcher.group());
                }

                if (index < 4) {
                    throw new IllegalArgumentException("Input string does not contain exactly four numeric values.");
                }
                mNodeStyleController.setBackgroundColor(parentNode, new Color(rgba[0], rgba[1], rgba[2], rgba[3]));
            } else {
                final Attribute newAttribute = new Attribute(key, value);
                try {
                    mAttributeController.addAttribute(parentNode, newAttribute);
                } catch (final Exception e) {
                    // sometimes this happens
                    // https://github.com/metacoma/freeplane_plugin_grpc/issues/1
                    LOG.warning("addAttribute failed for key " + key + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Extracts plain text from HTML using Jsoup.
     */
    static String bodyText(String html) {
        String printableText = "";
        try {
            final Document document = Jsoup.parse(html);
            final Element bodyElement = document.body();
            printableText = bodyElement.text();
        } catch (final NullPointerException e) {
            // known issue https://github.com/metacoma/freeplane_plugin_grpc/issues/10
            LOG.warning("BodyText exception: " + e.getMessage());
        }

        return printableText;
    }

    /**
     * Walks a node tree and populates a Map with node data for JSON serialization.
     */
    void nodeWalk(java.util.Map<String, Object> map, NodeModel node) {
        final MIconController mIconController =
            (MIconController) IconController.getController();

        map.put("text", node.getUserObject().toString());
        map.put("id", node.getID());

        // ---------- children ----------
        final NodeModel[] children = node.getChildren().toArray(new NodeModel[] {});
        if (children.length > 0) {
            final List<java.util.Map<String, Object>> childrenList = new ArrayList<>();

            for (final NodeModel child : children) {
                final java.util.Map<String, Object> childMap = new java.util.HashMap<>();
                nodeWalk(childMap, child);
                childrenList.add(childMap);
            }

            map.put("children", childrenList);
        }

        // ---------- attributes ----------
        final AttributeUtilities atrUtil = new AttributeUtilities();
        if (atrUtil.hasAttributes(node)) {
            final java.util.Map<String, Object> attributes = new java.util.HashMap<>();

            final NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(node);
            for (int i = 0; i < natm.getRowCount(); i++) {
                final Attribute attr = natm.getAttribute(i);
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
        if (NodeLinks.getLink(node) != null) {
            map.put("link", NodeLinks.getLink(node).toString());
        }

        // ---------- tags ----------
        final List<Tag> nodeTags = mIconController.getTags(node);
        if (nodeTags != null && !nodeTags.isEmpty()) {
            final List<String> tags = nodeTags.stream()
                .map(Tag::getContent)
                .collect(Collectors.toList());

            map.put("tags", tags);
        }

        if (node.getIcons().size() > 0) {
            final List<String> icons = node.getIcons().stream()
                .map(NamedIcon::getName)
                .collect(Collectors.toList());

            map.put("icons", icons);
        }
    }
}
