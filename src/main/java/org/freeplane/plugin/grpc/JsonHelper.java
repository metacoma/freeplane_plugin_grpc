package org.freeplane.plugin.grpc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.freeplane.features.link.ConnectorModel;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.NodeStyleModel;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper methods for JSON ↔ mindmap node conversion.
 *
 * <p><b>Canonical JSON format</b> (used by both import and export):</p>
 * <pre>{@code
 * {
 *   "text": "node text",
 *   "id": "ID_xxxxxxxx",
 *   "children": [ ... ],
 *   "attributes": { "key": "value" },
 *   "detail": "detail text",
 *   "note": "note text",
 *   "link": "https://example.com",
 *   "tags": ["tag1", "tag2"],
 *   "icons": ["icon1", "icon2"],
 *   "background_color": "rgba(255,128,0,128)",
 *   "folded": false,
 *   "relationships": [
 *     { "target_id": "ID_yyyyyyyy", "label": "related to" }
 *   ]
 * }
 * }</pre>
 *
 * <p><b>Legacy format compatibility:</b> The import path still accepts the legacy
 * key-as-text format (where each JSON key becomes node text and nested objects
 * become children). A migration layer converts legacy input to the canonical format
 * before processing.</p>
 */
public final class JsonHelper {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(JsonHelper.class.getName());

    /** Canonical field names shared by import and export. */
    static final String FIELD_TEXT        = "text";
    static final String FIELD_ID          = "id";
    static final String FIELD_CHILDREN    = "children";
    static final String FIELD_ATTRIBUTES  = "attributes";
    static final String FIELD_DETAIL      = "detail";
    static final String FIELD_NOTE        = "note";
    static final String FIELD_LINK        = "link";
    static final String FIELD_TAGS        = "tags";
    static final String FIELD_ICONS       = "icons";
    static final String FIELD_BACKGROUND_COLOR = "background_color";
    static final String FIELD_FOLDED      = "folded";
    static final String FIELD_RELATIONSHIPS = "relationships";

    /** Legacy keys that indicate legacy import format. */
    static final String LEGACY_FP_IMPORT_ROOT = "_fp_import_root_node";
    static final String LEGACY_UUID_ATTR      = "_uuid";
    static final String LEGACY_RELATIONSHIP_ATTR = "_relationship";

    /** Set of keys that identify canonical-format node objects. */
    private static final java.util.Set<String> CANONICAL_KEYS = java.util.Set.of(
            FIELD_TEXT, FIELD_ID, FIELD_CHILDREN, FIELD_ATTRIBUTES,
            FIELD_DETAIL, FIELD_NOTE, FIELD_LINK, FIELD_TAGS,
            FIELD_ICONS, FIELD_BACKGROUND_COLOR, FIELD_FOLDED, FIELD_RELATIONSHIPS
    );

    /** Gson instance for deterministic JSON serialization (LinkedHashMap preserves insertion order). */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    JsonHelper() {
        // Prevent instantiation from outside this package
    }

    // =========================================================================
    // EXPORT: nodeWalk — canonical JSON output
    // =========================================================================

    /**
     * Walks a node tree and populates a Map with node data for JSON serialization
     * using the canonical format.
     */
    void nodeWalk(java.util.Map<String, Object> map, NodeModel node) {
        final MIconController mIconController =
            (MIconController) IconController.getController();
        final MNodeStyleController mNodeStyleController =
            (MNodeStyleController) NodeStyleController.getController();

        // Use LinkedHashMap for deterministic field ordering
        final Map<String, Object> result = new LinkedHashMap<>();

        result.put(FIELD_TEXT, node.getUserObject().toString());
        result.put(FIELD_ID, node.getID());

        // ---------- children ----------
        final NodeModel[] children = node.getChildren().toArray(new NodeModel[] {});
        if (children.length > 0) {
            final List<Map<String, Object>> childrenList = new ArrayList<>();
            for (final NodeModel child : children) {
                final Map<String, Object> childMap = new LinkedHashMap<>();
                nodeWalk(childMap, child);
                childrenList.add(childMap);
            }
            result.put(FIELD_CHILDREN, childrenList);
        }

        // ---------- attributes ----------
        final AttributeUtilities atrUtil = new AttributeUtilities();
        if (atrUtil.hasAttributes(node)) {
            final Map<String, Object> attributes = new LinkedHashMap<>();
            final NodeAttributeTableModel natm = NodeAttributeTableModel.getModel(node);
            for (int i = 0; i < natm.getRowCount(); i++) {
                final Attribute attr = natm.getAttribute(i);
                final String attrName = attr.getName();
                // Filter out internal attributes that should not appear in exported JSON
                if (!LEGACY_UUID_ATTR.equals(attrName) && !LEGACY_RELATIONSHIP_ATTR.equals(attrName)) {
                    attributes.put(attrName, String.valueOf(attr.getValue()));
                }
            }
            if (!attributes.isEmpty()) {
                result.put(FIELD_ATTRIBUTES, attributes);
            }
        }

        // ---------- detail ----------
        if (DetailModel.getDetail(node) != null) {
            result.put(FIELD_DETAIL, bodyText(DetailModel.getDetailText(node)));
        }

        // ---------- note ----------
        if (NoteModel.getNoteText(node) != null) {
            result.put(FIELD_NOTE, bodyText(NoteModel.getNoteText(node)));
        }

        // ---------- link ----------
        if (NodeLinks.getLink(node) != null) {
            result.put(FIELD_LINK, NodeLinks.getLink(node).toString());
        }

        // ---------- tags ----------
        final List<Tag> nodeTags = mIconController.getTags(node);
        if (nodeTags != null && !nodeTags.isEmpty()) {
            final List<String> tags = nodeTags.stream()
                .map(Tag::getContent)
                .collect(Collectors.toList());
            result.put(FIELD_TAGS, tags);
        }

        // ---------- icons ----------
        if (node.getIcons().size() > 0) {
            final List<String> icons = node.getIcons().stream()
                .map(NamedIcon::getName)
                .collect(Collectors.toList());
            result.put(FIELD_ICONS, icons);
        }

        // ---------- background_color (NEW) ----------
        Color bgColor = NodeStyleModel.getBackgroundColor(node);
        if (bgColor != null) {
            result.put(FIELD_BACKGROUND_COLOR, formatRgba(bgColor));
        }

        // ---------- folded (NEW) ----------
        if (node.isFolded()) {
            result.put(FIELD_FOLDED, true);
        }

        // ---------- relationships (NEW) ----------
        List<Map<String, String>> relationships = collectConnectors(node);
        if (!relationships.isEmpty()) {
            result.put(FIELD_RELATIONSHIPS, relationships);
        }

        // Copy all entries to the caller's map
        map.putAll(result);
    }

    /**
     * Collects connector/relationship data for a node.
     * Enumerates all NodeLinkModel objects for the node, filters ConnectorModel instances,
     * and returns target_id + middleLabel for each.
     */
    private List<Map<String, String>> collectConnectors(NodeModel node) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            java.util.Collection<?> links = NodeLinks.getLinks(node);
            for (Object linkObj : links) {
                if (linkObj instanceof ConnectorModel) {
                    ConnectorModel connector = (ConnectorModel) linkObj;
                    Map<String, String> rel = new LinkedHashMap<>();
                    String targetId = connector.getTargetID();
                    if (targetId != null && !targetId.isEmpty()) {
                        rel.put("target_id", targetId);
                    }
                    String label = connector.getMiddleLabel().orElse(null);
                    if (label != null && !label.isEmpty()) {
                        rel.put("label", label);
                    }
                    if (!rel.isEmpty()) {
                        result.add(rel);
                    }
                }
            }
        } catch (Exception e) {
            LOG.fine("Could not collect connectors for node: " + e.getMessage());
        }
        return result;
    }

    // =========================================================================
    // IMPORT: mindMapFromJSON — canonical format + legacy migration
    // =========================================================================

    /**
     * Entry point for JSON import. Detects legacy format and migrates to canonical
     * before delegating to the canonical import path.
     *
     * @param jsonInput  raw JSON string (may be legacy or canonical format)
     * @param parentNode the parent node under which to insert the imported tree
     * @return the JSONObject representing the root of the imported tree (for connector processing)
     */
    JSONObject mindMapFromJSON(String jsonInput, NodeModel parentNode) {
        JSONObject obj = new JSONObject(jsonInput);

        // Strip _fp_import_root_node wrapper if present (handled by gRPC service for mode)
        // but the JSON still contains it, so we need to extract the inner object.
        if (obj.has(LEGACY_FP_IMPORT_ROOT)) {
            // Find the actual mindmap object (the non-_fp_import_root_node key)
            for (String key : obj.keySet()) {
                if (!key.equals(LEGACY_FP_IMPORT_ROOT)) {
                    Object val = obj.get(key);
                    if (val instanceof JSONObject) {
                        obj = (JSONObject) val;
                        break;
                    }
                }
            }
        }

        // Detect legacy format and migrate if needed
        if (isLegacyFormat(obj)) {
            obj = migrateLegacyToCanonical(obj);
        }

        // Process canonical format
        Map<String, NodeModel> idToNode = new LinkedHashMap<>();
        recursiveJSONLoopCanonical(obj, parentNode, idToNode);

        return obj;
    }

    /**
     * Detects whether a JSONObject is in the legacy key-as-text format.
     * Legacy format indicators:
     * - Top-level keys are NOT canonical field names
     * - Contains _fp_import_root_node key
     */
    private boolean isLegacyFormat(JSONObject obj) {
        if (obj.has(LEGACY_FP_IMPORT_ROOT)) {
            return true;
        }
        // If any key is a canonical field name, treat as canonical
        for (String key : obj.keySet()) {
            if (CANONICAL_KEYS.contains(key)) {
                return false;
            }
        }
        // If all keys are non-canonical, it's legacy key-as-text format
        return true;
    }

    /**
     * Migrates legacy key-as-text format to canonical format.
     *
     * Legacy:  {"parent": {"child1": "val1", "child2": {"grandchild": "val2"}}}
     * Canonical: {"text": "parent", "children": [{"text": "child1", ...}, {"text": "child2", "children": [...]}]}
     */
    private JSONObject migrateLegacyToCanonical(JSONObject legacy) {
        LOG.info("Detected legacy import format, migrating to canonical format");
        JSONObject canonical = new JSONObject();

        for (String key : legacy.keySet()) {
            Object value = legacy.get(key);

            if (key.equals("insert_mode")) {
                // Already extracted from _fp_import_root_node
                continue;
            }

            if (value instanceof JSONObject) {
                // Nested object: key becomes text, nested becomes children
                JSONObject nestedObj = (JSONObject) value;
                JSONObject nodeObj = new JSONObject();
                nodeObj.put(FIELD_TEXT, key);
                nodeObj.put(FIELD_CHILDREN, migrateLegacyChildren(nestedObj));
                canonical = nodeObj;
                // If there are multiple top-level keys, wrap in children array
                if (legacy.keySet().size() > 1) {
                    JSONObject wrapper = new JSONObject();
                    wrapper.put(FIELD_CHILDREN, new JSONArray(List.of(canonical)));
                    return wrapper;
                }
            } else if (value instanceof JSONArray) {
                // Array: key becomes text, array elements become children
                JSONArray arr = (JSONArray) value;
                JSONArray childrenArray = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject childObj = new JSONObject();
                    childObj.put(FIELD_TEXT, Integer.toString(i));
                    if (arr.get(i) instanceof JSONObject) {
                        JSONObject elemObj = (JSONObject) arr.get(i);
                        JSONObject migrated = migrateLegacyToCanonical(elemObj);
                        // Copy fields from migrated
                        for (String mk : migrated.keySet()) {
                            childObj.put(mk, migrated.get(mk));
                        }
                    }
                    childrenArray.put(childObj);
                }
                JSONObject nodeObj = new JSONObject();
                nodeObj.put(FIELD_TEXT, key);
                nodeObj.put(FIELD_CHILDREN, childrenArray);
                if (legacy.keySet().size() > 1) {
                    JSONObject wrapper = new JSONObject();
                    wrapper.put(FIELD_CHILDREN, new JSONArray(List.of(nodeObj)));
                    return wrapper;
                }
                canonical = nodeObj;
            } else {
                // Scalar value: key is text, value is note or attribute
                JSONObject nodeObj = new JSONObject();
                nodeObj.put(FIELD_TEXT, key);
                if (value != null && !value.toString().isEmpty()) {
                    nodeObj.put(FIELD_NOTE, value.toString());
                }
                if (legacy.keySet().size() > 1) {
                    JSONObject wrapper = new JSONObject();
                    wrapper.put(FIELD_CHILDREN, new JSONArray(List.of(nodeObj)));
                    return wrapper;
                }
                canonical = nodeObj;
            }
        }
        return canonical;
    }

    /** Recursively migrates a legacy JSONObject's children to canonical children array. */
    private JSONArray migrateLegacyChildren(JSONObject legacy) {
        JSONArray children = new JSONArray();
        for (String key : legacy.keySet()) {
            Object value = legacy.get(key);
            JSONObject childObj = new JSONObject();
            childObj.put(FIELD_TEXT, key);
            if (value instanceof JSONObject) {
                JSONObject nested = (JSONObject) value;
                childObj.put(FIELD_CHILDREN, migrateLegacyChildren(nested));
            } else if (value instanceof JSONArray) {
                JSONArray arr = (JSONArray) value;
                JSONArray arrChildren = new JSONArray();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject elem = new JSONObject();
                    elem.put(FIELD_TEXT, Integer.toString(i));
                    if (arr.get(i) instanceof JSONObject) {
                        JSONObject elemObj = (JSONObject) arr.get(i);
                        JSONObject migrated = migrateLegacyToCanonical(elemObj);
                        for (String mk : migrated.keySet()) {
                            elem.put(mk, migrated.get(mk));
                        }
                    }
                    arrChildren.put(elem);
                }
                childObj.put(FIELD_CHILDREN, arrChildren);
            } else if (value != null && !value.toString().isEmpty()) {
                childObj.put(FIELD_NOTE, value.toString());
            }
            children.put(childObj);
        }
        return children;
    }

    /**
     * Canonical import: processes a JSONObject in the canonical format.
     */
    private void recursiveJSONLoopCanonical(JSONObject jsonObject, NodeModel parentNode,
                                            Map<String, NodeModel> idToNode) {
        final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
        final MTextController mTextController = (MTextController) TextController.getController();
        final MIconController mIconController = (MIconController) IconController.getController();
        final MNodeStyleController mNodeStyleController = (MNodeStyleController) NodeStyleController.getController();
        final MNoteController mNoteController = MNoteController.getController();
        final org.freeplane.features.attribute.mindmapmode.MAttributeController mAttributeController =
                org.freeplane.features.attribute.mindmapmode.MAttributeController.getController();
        final MapController mapController = Controller.getCurrentModeController().getMapController();

        // Extract text
        String nodeText = jsonObject.optString(FIELD_TEXT, null);
        if (nodeText == null || nodeText.isEmpty()) {
            nodeText = "unnamed";
        }

        final NodeModel newNodeModel = mmapController.newNode(nodeText, parentNode.getMap());
        newNodeModel.setSide(mapController.suggestNewChildSide(parentNode, NodeModel.Side.DEFAULT));
        newNodeModel.createID();
        // Insert at position 0; children are processed in order, so position 0
        // means the first child ends up at index 0 after all insertions.
        mmapController.insertNode(newNodeModel, parentNode, 0);

        // Store ID mapping for connector resolution
        idToNode.put(newNodeModel.getID(), newNodeModel);

        // Extract and set ID hint (for reference; actual ID is already set)
        if (jsonObject.has(FIELD_ID)) {
            // The ID in the JSON is informational; Freeplane generates its own IDs.
            // We store it as an attribute for reference.
            try {
                Attribute idAttr = new Attribute(LEGACY_UUID_ATTR, jsonObject.getString(FIELD_ID));
                mAttributeController.addAttribute(newNodeModel, idAttr);
            } catch (Exception e) {
                LOG.fine("Could not store ID hint: " + e.getMessage());
            }
        }

        // ---------- children (insert in reverse order so first child ends up at index 0) ----------
        if (jsonObject.has(FIELD_CHILDREN)) {
            Object childrenObj = jsonObject.get(FIELD_CHILDREN);
            if (childrenObj instanceof JSONArray) {
                JSONArray childrenArray = (JSONArray) childrenObj;
                for (int i = childrenArray.length() - 1; i >= 0; i--) {
                    Object childObj = childrenArray.get(i);
                    if (childObj instanceof JSONObject) {
                        JSONObject childJson = (JSONObject) childObj;
                        recursiveJSONLoopCanonical(childJson, newNodeModel, idToNode);
                    }
                }
            }
        }

        // ---------- attributes ----------
        if (jsonObject.has(FIELD_ATTRIBUTES)) {
            Object attrsObj = jsonObject.get(FIELD_ATTRIBUTES);
            if (attrsObj instanceof JSONObject) {
                JSONObject attrsJson = (JSONObject) attrsObj;
                for (String attrKey : attrsJson.keySet()) {
                    try {
                        Attribute attr = new Attribute(attrKey, attrsJson.get(attrKey));
                        mAttributeController.addAttribute(newNodeModel, attr);
                    } catch (Exception e) {
                        LOG.warning("addAttribute failed for key " + attrKey + ": " + e.getMessage());
                    }
                }
            }
        }

        // ---------- detail ----------
        if (jsonObject.has(FIELD_DETAIL)) {
            mTextController.setDetails(newNodeModel, jsonObject.optString(FIELD_DETAIL, ""));
        }

        // ---------- note ----------
        if (jsonObject.has(FIELD_NOTE)) {
            mNoteController.setNoteText(newNodeModel, jsonObject.optString(FIELD_NOTE, ""));
        }

        // ---------- link ----------
        if (jsonObject.has(FIELD_LINK)) {
            String linkStr = jsonObject.optString(FIELD_LINK, "");
            if (!linkStr.isEmpty()) {
                try {
                    final URI uri = new URI(linkStr);
                    final MLinkController mLinkController = (MLinkController) LinkController.getController();
                    mLinkController.setLink(newNodeModel, new Hyperlink(uri));
                } catch (Exception e) {
                    LOG.warning("Failed to set link: " + e.getMessage());
                }
            }
        }

        // ---------- tags (accept both array and comma-separated string) ----------
        if (jsonObject.has(FIELD_TAGS)) {
            Object tagsObj = jsonObject.get(FIELD_TAGS);
            List<Tag> tagList = new ArrayList<>();
            if (tagsObj instanceof JSONArray) {
                JSONArray tagsArray = (JSONArray) tagsObj;
                for (int i = 0; i < tagsArray.length(); i++) {
                    String tagContent = tagsArray.getString(i);
                    if (tagContent != null && !tagContent.trim().isEmpty()) {
                        tagList.add(new Tag(tagContent.trim()));
                    }
                }
            } else {
                // Legacy: comma-separated string
                String[] parts = tagsObj.toString().split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        tagList.add(new Tag(trimmed));
                    }
                }
            }
            if (!tagList.isEmpty()) {
                mIconController.setTags(newNodeModel, tagList, false);
            }
        }

        // ---------- icons (accept both array and comma-separated string) ----------
        if (jsonObject.has(FIELD_ICONS)) {
            Object iconsObj = jsonObject.get(FIELD_ICONS);
            List<String> iconNames = new ArrayList<>();
            if (iconsObj instanceof JSONArray) {
                JSONArray iconsArray = (JSONArray) iconsObj;
                for (int i = 0; i < iconsArray.length(); i++) {
                    iconNames.add(iconsArray.getString(i));
                }
            } else {
                String[] parts = iconsObj.toString().split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        iconNames.add(trimmed);
                    }
                }
            }
            for (String iconName : iconNames) {
                MindIcon icon = IconStoreFactory.ICON_STORE.getMindIcon(iconName);
                if (icon != null) {
                    newNodeModel.addIcon(icon);
                } else {
                    LOG.fine("Icon not found: " + iconName);
                }
            }
        }

        // ---------- background_color (NEW) ----------
        if (jsonObject.has(FIELD_BACKGROUND_COLOR)) {
            String colorStr = jsonObject.optString(FIELD_BACKGROUND_COLOR, "");
            if (!colorStr.isEmpty()) {
                Color bgColor = parseColor(colorStr);
                if (bgColor != null) {
                    mNodeStyleController.setBackgroundColor(newNodeModel, bgColor);
                }
            }
        }

        // ---------- folded (NEW) ----------
        if (jsonObject.optBoolean(FIELD_FOLDED, false)) {
            newNodeModel.setFolded(true);
        }

        // ---------- relationships (NEW) ----------
        // Store for post-processing after all nodes are created
        if (jsonObject.has(FIELD_RELATIONSHIPS)) {
            Object relsObj = jsonObject.get(FIELD_RELATIONSHIPS);
            if (relsObj instanceof JSONArray) {
                JSONArray relsArray = (JSONArray) relsObj;
                List<Map.Entry<String, String>> rels = new ArrayList<>();
                for (int i = 0; i < relsArray.length(); i++) {
                    if (relsArray.get(i) instanceof JSONObject) {
                        JSONObject relObj = (JSONObject) relsArray.get(i);
                        String targetId = relObj.optString("target_id", "");
                        String label = relObj.optString("label", "");
                        if (!targetId.isEmpty()) {
                            rels.add(new java.util.AbstractMap.SimpleEntry<>(targetId, label));
                        }
                    }
                }
                if (!rels.isEmpty()) {
                    // Store as attribute for post-processing
                    StringBuilder relValue = new StringBuilder();
                    for (int i = 0; i < rels.size(); i++) {
                        if (i > 0) relValue.append(",");
                        relValue.append(rels.get(i).getKey()).append(":").append(rels.get(i).getValue());
                    }
                    try {
                        Attribute relAttr = new Attribute(LEGACY_RELATIONSHIP_ATTR, relValue.toString());
                        mAttributeController.addAttribute(newNodeModel, relAttr);
                    } catch (Exception e) {
                        LOG.warning("Could not store relationship attribute: " + e.getMessage());
                    }
                }
            }
        }
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

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
            LOG.warning("BodyText exception: " + e.getMessage());
        }
        return printableText;
    }

    /**
     * Formats a Color as "rgba(r,g,b,a)" string.
     */
    private static String formatRgba(Color color) {
        return String.format("rgba(%d,%d,%d,%d)",
                color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * Parses a color string. Accepts:
     * - "rgba(r,g,b,a)" format
     * - "rgb(r,g,b)" format (alpha defaults to 255)
     * - Hex color "#rrggbb" or "#rrggbbaa"
     * - Named colors: "red", "green", "blue", etc.
     */
    static Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return null;
        }

        try {
            // Try rgba(r,g,b,a) format
            if (colorStr.startsWith("rgba")) {
                Pattern p = Pattern.compile("(\\d+)");
                Matcher m = p.matcher(colorStr);
                int[] vals = new int[4];
                int idx = 0;
                while (m.find() && idx < 4) {
                    vals[idx++] = Integer.parseInt(m.group());
                }
                if (idx >= 3) {
                    return new Color(vals[0], vals[1], vals[2], idx >= 4 ? vals[3] : 255);
                }
            }
            // Try rgb(r,g,b) format
            else if (colorStr.startsWith("rgb")) {
                Pattern p = Pattern.compile("(\\d+)");
                Matcher m = p.matcher(colorStr);
                int[] vals = new int[3];
                int idx = 0;
                while (m.find() && idx < 3) {
                    vals[idx++] = Integer.parseInt(m.group());
                }
                if (idx >= 3) {
                    return new Color(vals[0], vals[1], vals[2], 255);
                }
            }
            // Try hex format
            else if (colorStr.startsWith("#")) {
                String hex = colorStr.substring(1);
                if (hex.length() == 6) {
                    return Color.decode("0x" + hex);
                } else if (hex.length() == 8) {
                    int val = Long.decode("0x" + hex).intValue();
                    return new Color(
                            (val >> 16) & 0xFF,
                            (val >> 8) & 0xFF,
                            val & 0xFF,
                            (val >> 24) & 0xFF
                    );
                }
            }
            // Try named colors
            else {
                java.lang.reflect.Field[] fields = Color.class.getFields();
                for (java.lang.reflect.Field field : fields) {
                    if (field.getType() == Color.class) {
                        Color c = (Color) field.get(null);
                        if (field.getName().equalsIgnoreCase(colorStr)) {
                            return c;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.fine("Could not parse color: " + colorStr + " -> " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns the canonical JSON string for a node map.
     */
    static String toJson(Map<String, Object> map) {
        return GSON.toJson(map);
    }
}
