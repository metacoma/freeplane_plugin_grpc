//package org.freeplane.plugin.grpc;
package org.freeplane.plugin.grpc;
//import org.freeplane.plugin.grpc.HelloWorldServer;
//import io.grpc.examples.helloworld;

import org.freeplane.features.icon.IconClickedEvent;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.IconMouseListener;
import org.freeplane.features.mode.ModeController;

import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.map.mindmapmode.MMapController;

import org.freeplane.features.attribute.mindmapmode.MAttributeController;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.link.LinkController;

import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.SharedNodeData;
import org.freeplane.features.map.NodeBuilder;
import org.freeplane.core.util.TextUtils;

import org.freeplane.api.Attributes;
import org.freeplane.core.util.Hyperlink;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.net.URI;


/*
import org.freeplane.plugin.openmaps.actions.InsertGrpcAction;
import org.freeplane.plugin.openmaps.actions.RemoveGrpcAction;
import org.freeplane.plugin.openmaps.actions.ViewGrpcAction;
*/

/**
 * @author Blair Archibald 
 */
public class GrpcRegistration {
        private Server server;
        private Integer port = 50051;
	public GrpcRegistration(ModeController modeController) { 
                  try {
                  server = ServerBuilder.forPort(port)
                                  .addService(new FreeplaneImpl())
                                          .build()
                                                  .start();
                  } catch(IOException e) {
                     System.out.println("exception"); 
                  } 
                System.out.println("Freeplane grpc plugin loaded and listen " + port + " port"); 
	}
 	static class FreeplaneImpl extends FreeplaneGrpc.FreeplaneImplBase {

    		@Override
    		public void createChild(CreateChildRequest req, StreamObserver<CreateChildResponse> responseObserver) {
                        //final MapController mapController = Controller.getCurrentModeController().getMapController();
                        final MapController mapController = Controller.getCurrentModeController().getMapController();
                        final MMapController mmapController = (MMapController) Controller.getCurrentModeController().getMapController();
                        final NodeModel rootNode = mapController.getRootNode();
                        final MapModel map = Controller.getCurrentController().getMap();
                        
                        System.out.println("GRPC Freeplane::createChild(" + req.getName() + ")"); 
                        NodeModel newNodeModel = mapController.newNode("test", rootNode.getMap());
                        newNodeModel.setSide(mapController.suggestNewChildSide(rootNode, NodeModel.Side.DEFAULT));
                        newNodeModel.createID();
                        mmapController.insertNode(newNodeModel, rootNode, false);

      			CreateChildResponse reply = CreateChildResponse.newBuilder().setNodeId(newNodeModel.getID()).setNodeText(newNodeModel.getText()).build();
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

                        NodeModel targetNode = map.getNodeForID(req.getNodeId());
                        if (targetNode != null) {
                          success = true;
                          Attribute newAttribute = new Attribute(req.getAttributeName(), req.getAttributeValue());
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
  	}

}
