grpc_ruby_docker_repo="grpc/ruby"
grpc_ruby_docker_tag="latest"

test -d ruby_lib && rm -rf ruby_lib 
mkdir ruby_lib
docker run --rm 	\
  -it 		\
  -v `pwd`:/host 	\
  -w /host            \
  ${grpc_ruby_docker_repo}:${grpc_ruby_docker_tag} \
  grpc_tools_ruby_protoc -I . --ruby_out=ruby_lib --grpc_out=ruby_lib ./freeplane.proto
  ls ruby_lib/
test -d python_lib && rm -rf python_lib
mkdir python_lib
docker run  \
        -v `pwd`:/host \
        -w/host 							\
        --rm -it 							\
        --entrypoint /bin/sh 						\
        python:3.11-slim -c '
                pip3 install grpcio grpcio-tools==1.51.1 protobuf
                python -m grpc_tools.protoc -I/host/ --python_out=/host/python_lib --pyi_out=/host/python_lib --grpc_python_out=/host/python_lib /host/freeplane.proto
        '

