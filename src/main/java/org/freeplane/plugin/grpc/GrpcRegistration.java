//package org.freeplane.plugin.grpc;
package org.freeplane.plugin.grpc;
//import org.freeplane.plugin.grpc.HelloWorldServer;
//import io.grpc.examples.helloworld;

import org.freeplane.features.icon.IconClickedEvent;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.IconMouseListener;
import org.freeplane.features.mode.ModeController;

import org.freeplane.features.mode.Controller;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.core.util.TextUtils;

//import org.freeplane.api.Node;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
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
	public GrpcRegistration(ModeController modeController) { 
                  try {
                  server = ServerBuilder.forPort(50051)
                                  .addService(new FreeplaneImpl())
                                          .build()
                                                  .start();
                  } catch(IOException e) {
                     System.out.println("exception"); 
                  } 
                System.out.println("Hello, World!"); 
	}
 	static class FreeplaneImpl extends FreeplaneGrpc.FreeplaneImplBase {

    		@Override
    		public void createChild(CreateChildRequest req, StreamObserver<CreateChildResponse> responseObserver) {
                        final MapController mapController = Controller.getCurrentModeController().getMapController();
                        final NodeModel rootNode = mapController.getRootNode();
                        NodeModel newNode = new NodeModel(rootNode.getMap());
                        newNode.setText(req.getName());
                        mapController.insertNodeIntoWithoutUndo(newNode, rootNode);
      			CreateChildResponse reply = CreateChildResponse.newBuilder().setMessage("Hello " + req.getName()).build();
      			responseObserver.onNext(reply);
      			responseObserver.onCompleted();
    		}
  	}

}
