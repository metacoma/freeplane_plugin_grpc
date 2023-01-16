//package org.freeplane.plugin.grpc;
package org.freeplane.plugin.grpc;
//import org.freeplane.plugin.grpc.HelloWorldServer;
//import io.grpc.examples.helloworld;

import org.freeplane.features.icon.IconClickedEvent;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.IconMouseListener;
import org.freeplane.features.mode.ModeController;

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
                                  .addService(new GreeterImpl())
                                          .build()
                                                  .start();
                  } catch(IOException e) {
                     System.out.println("exception"); 
                  } 
                System.out.println("Hello, World!"); 
	}
 	static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

    		@Override
    		public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
      			HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
      			responseObserver.onNext(reply);
      			responseObserver.onCompleted();
    		}
  	}

}
