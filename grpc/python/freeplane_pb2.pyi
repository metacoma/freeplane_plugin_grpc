from google.protobuf.internal import containers as _containers
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from collections.abc import Iterable as _Iterable, Mapping as _Mapping
from typing import ClassVar as _ClassVar, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class CreateChildRequest(_message.Message):
    __slots__ = ("name", "parent_node_id")
    NAME_FIELD_NUMBER: _ClassVar[int]
    PARENT_NODE_ID_FIELD_NUMBER: _ClassVar[int]
    name: str
    parent_node_id: str
    def __init__(self, name: _Optional[str] = ..., parent_node_id: _Optional[str] = ...) -> None: ...

class CreateChildResponse(_message.Message):
    __slots__ = ("node_id", "node_text")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    NODE_TEXT_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    node_text: str
    def __init__(self, node_id: _Optional[str] = ..., node_text: _Optional[str] = ...) -> None: ...

class DeleteChildRequest(_message.Message):
    __slots__ = ("node_id",)
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    def __init__(self, node_id: _Optional[str] = ...) -> None: ...

class DeleteChildResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class NodeAttributeAddRequest(_message.Message):
    __slots__ = ("node_id", "attribute_name", "attribute_value")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    ATTRIBUTE_NAME_FIELD_NUMBER: _ClassVar[int]
    ATTRIBUTE_VALUE_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    attribute_name: str
    attribute_value: str
    def __init__(self, node_id: _Optional[str] = ..., attribute_name: _Optional[str] = ..., attribute_value: _Optional[str] = ...) -> None: ...

class NodeAttributeAddResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class NodeLinkSetRequest(_message.Message):
    __slots__ = ("node_id", "link")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    LINK_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    link: str
    def __init__(self, node_id: _Optional[str] = ..., link: _Optional[str] = ...) -> None: ...

class NodeLinkSetResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class NodeDetailsSetRequest(_message.Message):
    __slots__ = ("node_id", "details")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    DETAILS_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    details: str
    def __init__(self, node_id: _Optional[str] = ..., details: _Optional[str] = ...) -> None: ...

class NodeDetailsSetResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class NodeNoteSetRequest(_message.Message):
    __slots__ = ("node_id", "note")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    NOTE_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    note: str
    def __init__(self, node_id: _Optional[str] = ..., note: _Optional[str] = ...) -> None: ...

class NodeNoteSetResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class NodeTagSetRequest(_message.Message):
    __slots__ = ("node_id", "tags")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    TAGS_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    tags: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, node_id: _Optional[str] = ..., tags: _Optional[_Iterable[str]] = ...) -> None: ...

class NodeTagSetResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class GroovyRequest(_message.Message):
    __slots__ = ("groovy_code",)
    GROOVY_CODE_FIELD_NUMBER: _ClassVar[int]
    groovy_code: str
    def __init__(self, groovy_code: _Optional[str] = ...) -> None: ...

class GroovyResponse(_message.Message):
    __slots__ = ("success", "result", "error_message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    RESULT_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    result: str
    error_message: str
    def __init__(self, success: _Optional[bool] = ..., result: _Optional[str] = ..., error_message: _Optional[str] = ...) -> None: ...

class NodeColorSetRequest(_message.Message):
    __slots__ = ("node_id", "red", "green", "blue", "alpha")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    RED_FIELD_NUMBER: _ClassVar[int]
    GREEN_FIELD_NUMBER: _ClassVar[int]
    BLUE_FIELD_NUMBER: _ClassVar[int]
    ALPHA_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    red: int
    green: int
    blue: int
    alpha: int
    def __init__(self, node_id: _Optional[str] = ..., red: _Optional[int] = ..., green: _Optional[int] = ..., blue: _Optional[int] = ..., alpha: _Optional[int] = ...) -> None: ...

class NodeColorSetResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class NodeBackgroundColorSetRequest(_message.Message):
    __slots__ = ("node_id", "red", "green", "blue", "alpha")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    RED_FIELD_NUMBER: _ClassVar[int]
    GREEN_FIELD_NUMBER: _ClassVar[int]
    BLUE_FIELD_NUMBER: _ClassVar[int]
    ALPHA_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    red: int
    green: int
    blue: int
    alpha: int
    def __init__(self, node_id: _Optional[str] = ..., red: _Optional[int] = ..., green: _Optional[int] = ..., blue: _Optional[int] = ..., alpha: _Optional[int] = ...) -> None: ...

class NodeBackgroundColorSetResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class StatusInfoSetRequest(_message.Message):
    __slots__ = ("statusInfo",)
    STATUSINFO_FIELD_NUMBER: _ClassVar[int]
    statusInfo: str
    def __init__(self, statusInfo: _Optional[str] = ...) -> None: ...

class StatusInfoSetResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class TextFSMRequest(_message.Message):
    __slots__ = ("json",)
    JSON_FIELD_NUMBER: _ClassVar[int]
    json: str
    def __init__(self, json: _Optional[str] = ...) -> None: ...

class TextFSMResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class MindMapFromJSONRequest(_message.Message):
    __slots__ = ("json",)
    JSON_FIELD_NUMBER: _ClassVar[int]
    json: str
    def __init__(self, json: _Optional[str] = ...) -> None: ...

class MindMapFromJSONResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class MindMapToJSONRequest(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class MindMapToJSONResponse(_message.Message):
    __slots__ = ("success", "json")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    JSON_FIELD_NUMBER: _ClassVar[int]
    success: bool
    json: str
    def __init__(self, success: _Optional[bool] = ..., json: _Optional[str] = ...) -> None: ...

class GetCurrentNodeRequest(_message.Message):
    __slots__ = ()
    def __init__(self) -> None: ...

class GetCurrentNodeResponse(_message.Message):
    __slots__ = ("map_id", "node_id", "success")
    MAP_ID_FIELD_NUMBER: _ClassVar[int]
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    map_id: str
    node_id: str
    success: bool
    def __init__(self, map_id: _Optional[str] = ..., node_id: _Optional[str] = ..., success: _Optional[bool] = ...) -> None: ...

class NodeTagAddRequest(_message.Message):
    __slots__ = ("node_id", "tags")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    TAGS_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    tags: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, node_id: _Optional[str] = ..., tags: _Optional[_Iterable[str]] = ...) -> None: ...

class NodeTagAddResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class NodeConnectRequest(_message.Message):
    __slots__ = ("source_node_id", "target_node_id", "relationship")
    SOURCE_NODE_ID_FIELD_NUMBER: _ClassVar[int]
    TARGET_NODE_ID_FIELD_NUMBER: _ClassVar[int]
    RELATIONSHIP_FIELD_NUMBER: _ClassVar[int]
    source_node_id: str
    target_node_id: str
    relationship: str
    def __init__(self, source_node_id: _Optional[str] = ..., target_node_id: _Optional[str] = ..., relationship: _Optional[str] = ...) -> None: ...

class NodeConnectResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class NodeAddIconRequest(_message.Message):
    __slots__ = ("node_id", "icon_name")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    ICON_NAME_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    icon_name: str
    def __init__(self, node_id: _Optional[str] = ..., icon_name: _Optional[str] = ...) -> None: ...

class NodeAddIconResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class OpenMapRequest(_message.Message):
    __slots__ = ("file_path",)
    FILE_PATH_FIELD_NUMBER: _ClassVar[int]
    file_path: str
    def __init__(self, file_path: _Optional[str] = ...) -> None: ...

class OpenMapResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class FocusNodeRequest(_message.Message):
    __slots__ = ("node_id",)
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    def __init__(self, node_id: _Optional[str] = ...) -> None: ...

class FocusNodeResponse(_message.Message):
    __slots__ = ("success",)
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    success: bool
    def __init__(self, success: _Optional[bool] = ...) -> None: ...

class GetNodeTextRequest(_message.Message):
    __slots__ = ("node_id",)
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    def __init__(self, node_id: _Optional[str] = ...) -> None: ...

class GetNodeTextResponse(_message.Message):
    __slots__ = ("success", "node_id", "text", "error_message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    TEXT_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    node_id: str
    text: str
    error_message: str
    def __init__(self, success: _Optional[bool] = ..., node_id: _Optional[str] = ..., text: _Optional[str] = ..., error_message: _Optional[str] = ...) -> None: ...

class GetParentNodeRequest(_message.Message):
    __slots__ = ("node_id",)
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    def __init__(self, node_id: _Optional[str] = ...) -> None: ...

class GetParentNodeResponse(_message.Message):
    __slots__ = ("success", "node_id", "parent_node_id", "parent_node_text", "error_message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    PARENT_NODE_ID_FIELD_NUMBER: _ClassVar[int]
    PARENT_NODE_TEXT_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    node_id: str
    parent_node_id: str
    parent_node_text: str
    error_message: str
    def __init__(self, success: _Optional[bool] = ..., node_id: _Optional[str] = ..., parent_node_id: _Optional[str] = ..., parent_node_text: _Optional[str] = ..., error_message: _Optional[str] = ...) -> None: ...

class ListChildNodesRequest(_message.Message):
    __slots__ = ("node_id",)
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    def __init__(self, node_id: _Optional[str] = ...) -> None: ...

class ListChildNodesResponse(_message.Message):
    __slots__ = ("success", "children", "error_message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    CHILDREN_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    children: _containers.RepeatedCompositeFieldContainer[ChildNodeInfo]
    error_message: str
    def __init__(self, success: _Optional[bool] = ..., children: _Optional[_Iterable[_Union[ChildNodeInfo, _Mapping]]] = ..., error_message: _Optional[str] = ...) -> None: ...

class ChildNodeInfo(_message.Message):
    __slots__ = ("node_id", "text")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    TEXT_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    text: str
    def __init__(self, node_id: _Optional[str] = ..., text: _Optional[str] = ...) -> None: ...

class GetNodeNoteRequest(_message.Message):
    __slots__ = ("node_id",)
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    def __init__(self, node_id: _Optional[str] = ...) -> None: ...

class GetNodeNoteResponse(_message.Message):
    __slots__ = ("success", "node_id", "note", "has_note", "error_message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    NOTE_FIELD_NUMBER: _ClassVar[int]
    HAS_NOTE_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    node_id: str
    note: str
    has_note: bool
    error_message: str
    def __init__(self, success: _Optional[bool] = ..., node_id: _Optional[str] = ..., note: _Optional[str] = ..., has_note: _Optional[bool] = ..., error_message: _Optional[str] = ...) -> None: ...

class GetNodeLinkRequest(_message.Message):
    __slots__ = ("node_id",)
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    def __init__(self, node_id: _Optional[str] = ...) -> None: ...

class GetNodeLinkResponse(_message.Message):
    __slots__ = ("success", "node_id", "link", "has_link", "error_message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    LINK_FIELD_NUMBER: _ClassVar[int]
    HAS_LINK_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    node_id: str
    link: str
    has_link: bool
    error_message: str
    def __init__(self, success: _Optional[bool] = ..., node_id: _Optional[str] = ..., link: _Optional[str] = ..., has_link: _Optional[bool] = ..., error_message: _Optional[str] = ...) -> None: ...

class SetNodeTextRequest(_message.Message):
    __slots__ = ("node_id", "text")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    TEXT_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    text: str
    def __init__(self, node_id: _Optional[str] = ..., text: _Optional[str] = ...) -> None: ...

class SetNodeTextResponse(_message.Message):
    __slots__ = ("success", "node_id", "error_message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    node_id: str
    error_message: str
    def __init__(self, success: _Optional[bool] = ..., node_id: _Optional[str] = ..., error_message: _Optional[str] = ...) -> None: ...

class MoveNodeRequest(_message.Message):
    __slots__ = ("node_id", "new_parent_node_id")
    NODE_ID_FIELD_NUMBER: _ClassVar[int]
    NEW_PARENT_NODE_ID_FIELD_NUMBER: _ClassVar[int]
    node_id: str
    new_parent_node_id: str
    def __init__(self, node_id: _Optional[str] = ..., new_parent_node_id: _Optional[str] = ...) -> None: ...

class MoveNodeResponse(_message.Message):
    __slots__ = ("success", "error_message")
    SUCCESS_FIELD_NUMBER: _ClassVar[int]
    ERROR_MESSAGE_FIELD_NUMBER: _ClassVar[int]
    success: bool
    error_message: str
    def __init__(self, success: _Optional[bool] = ..., error_message: _Optional[str] = ...) -> None: ...
