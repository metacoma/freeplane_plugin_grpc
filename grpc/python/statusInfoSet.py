import grpc
import freeplane_pb2
import freeplane_pb2_grpc

channel = grpc.insecure_channel('localhost:50051') 
fp = freeplane_pb2_grpc.FreeplaneStub(channel)

fp.StatusInfoSet(freeplane_pb2.StatusInfoSetRequest(statusInfo = "hello from python"))
