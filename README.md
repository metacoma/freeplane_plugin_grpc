1. [Introduction](#introduction)

2. [gRPC](#grpc)

3. [Examples](#examples)

    3.1 [Ruby](#example_python)

    3.2 [Python](#example_ruby)

    3.3 [Shell (grpcurl)](#example_shell)

4. [How it works?](#how_it_works)

5. [Quick start](#quick_start)

    5.1 [Install plugin](#install_plugin)

    5.2 [Build plugin](#build_plugin)

    5.2 [Add new gRPC function](#add_new_grpc_function)

6. [Limitations of the current implementation](#limitation)
  
**<a id="introduction">1. Introduction</a>**

  This repository is a part of [MindWM](https://github.com/metacoma/mindwm) projects and contains:

  [Freeplane plugin grpc](https://github.com/metacoma/freeplane_plugin_grpc/tree/main/src/main/java/org/freeplane/plugin/grpc)

  The Freeplane plugin for gRPC allows you to start a gRPC server within a running instance of Freeplane. The gRCP server listens on port 50051 and waits for gRPC calls from clients

  [Protobuf file](https://github.com/metacoma/freeplane_plugin_grpc/blob/ca8f667a0f373506c579762c92ffd954ca1827c7/src/main/proto/freeplane.proto)

  The Protocol Buffer file describes the protocol for interaction between gRPC clients and servers.

  Pre-built necessary libraries for [python](https://github.com/metacoma/freeplane_plugin_grpc/tree/main/grpc/python) and [ruby](https://github.com/metacoma/freeplane_plugin_grpc/tree/main/grpc/ruby/lib)

  The pre-built libraries for Python and Ruby, based on the Protocol Buffer file, make it easy to get started with these programming languages.

  Examples for [python](https://github.com/metacoma/freeplane_plugin_grpc/blob/main/grpc/python/freeplane_ec2.py), [ruby](https://github.com/metacoma/freeplane_plugin_grpc/blob/ca8f667a0f373506c579762c92ffd954ca1827c7/grpc/ruby/pomodoro.rb) and [shell](https://github.com/metacoma/freeplane_plugin_grpc/blob/ca8f667a0f373506c579762c92ffd954ca1827c7/grpc/shell/grpcurl_test.sh)

  The repository also includes simple examples of gRPC clients written in Shell, Python, and Ruby.

**<a id="grpc">2. gRPC</a>**

![image](https://developer.token.io/token_rest_api_doc/content/resources/images/grpc-proto_concept.png)

gRPC is a high-performance, open-source framework for building remote procedure call (RPC) APIs. 
It uses the Protocol Buffers data serialization format and supports a variety of programming languages. 
The main goals of gRPC are to provide a high-performance, efficient, and low-latency way to create scalable, distributed systems. 

With freeplane_grpc_plugin you can use your preferred programming language to enhance/extend the functionality of Freeplane, here is a list of programming languages that support gRPC, along with gRPC tutorial links.

  *  [C# / .NET](https://grpc.io/docs/languages/csharp/)

  *  [C++](https://grpc.io/docs/languages/cpp/)

  *  [Dart](https://grpc.io/docs/languages/dart/)

  *  [Go](https://grpc.io/docs/languages/go/)

  *  [Java](https://grpc.io/docs/languages/java/)

  *  [Kotlin](https://grpc.io/docs/languages/kotlin/)

  *  [Node](https://grpc.io/docs/languages/node/)

  *  [Objective-C](https://grpc.io/docs/languages/objective-c/)

  *  [PHP](https://grpc.io/docs/languages/php/)

  *  [Python](https://grpc.io/docs/languages/python/)

  *  [Ruby](https://grpc.io/docs/languages/ruby/)

**<a id="exampels">3. Examples</a>**

  You can find ruby,python and shell examples in [grpc](https://github.com/metacoma/freeplane_plugin_grpc/tree/main/grpc) directory.

**<a id="example_ruby">3.1 Ruby gRPC client example</a>**

  I use the Pomodoro time management system to plan and organize my day. I typically use the [get-pomodori](https://github.com/Matt-Deacalion/Pomodoro-Calculator) CLI tool to calculate my Pomodoro sessions.

![image](https://camo.githubusercontent.com/cc146572f14629e0a4fc2f834a61240b16b522c4e7775ae186b029c49d497ca1/68747470733a2f2f7261772e6769746875622e636f6d2f4d6174742d44656163616c696f6e2f506f6d6f646f726f2d43616c63756c61746f722f6d61737465722f73637265656e73686f742e706e67)

  The following ruby script helps me to visualize the pomodoro sessions in mindmap as mindmap nodes.
  

```bash
# install required gRPC ruby dependenices https://grpc.io/docs/languages/ruby/quickstart/
$ gem install grpc 
$ gem install grpc-tools
# clone this repo
$ git clone https://github.com/metacoma/freeplane_plugin_grpc 
# run Freeplane with installed freeplane_plugin_grpc
$ ~/freeplane/BIN/freeplane.sh &
$ cd grpc/ruby
# if you have installed get-pomodori
$ get-pomodori --from=08:00:00 --break=5 --long-break=26 --amount 16 --json | ruby ./pomodoro.rb 
# if you don't have get-pomodori, you can use example json data
$ cat pomodori_example_data.json | ruby ./pomodoro.rb
```

**<a id="example_python">3.2 Python example</a>**
  
This example demonstrates how you can visualize and create a mind map for a list of Amazon EC2 virtual machines.

```bash
# install required gRPC python dependencies https://grpc.io/docs/languages/python/quickstart/
# you need python 3.7 or higher and pip 9.0.1 or higher
$ python -m pip install grpcio
$ python -m pip install grpcio-tools 
# run Freeplane with installed freeplane_plugin_grpc
$ ~/freeplane/BIN/freeplane.sh &
$ git clone https://github.com/metacoma/freeplane_plugin_grpc 
$ cd freeplane_plugin_grpc/grpc/python
# if you have installed and configured awscli https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html
$ aws emr list-instances | python3 ./freeplane_ec2.py
# if you don't have amazon cli tool, you can use just the example data from this repo
$ cat ec2_list_instances.json | python3 ./freeplane_ec2.py 
```

**<a id="example_shell">3.3 Shell example</a>**
   
   This example demonstrates how you can interact with a running Freeplane instance from a terminal bash session using the [grpcurl]( https://github.com/fullstorydev/grpcurl) CLI tool 
   The shell example will retrieve the current temperature using the sensors utility, parse the output, and update the mind map every 0.1 seconds in infinite loop.

```bash
# install grpcurl in any convient way https://github.com/fullstorydev/grpcurl/releases 
# https://repology.org/project/grpcurl/information
# I preffer to use grpcurl inside docker container
# also, you need to edit grpcurl_test.sh 
$ alias grpcurl="docker run -i -v `pwd`:/host/ -w /host --network=host fullstorydev/grpcurl" 
$ git clone https://github.com/metacoma/freeplane_plugin_grpc 
$ cd freeplane_plugin_grpc/grpc/shell
# install lm_sensors package to grab gather the temperature sensor
$ bash ./grpcurl_test.sh
```

**<a id="how_it_works">4 How it works?</a>**

  The Freeplane plugin gRPC starts by launching the Freeplane instance and listens on TCP4 port 50051 to accept gRPC clients.

  Once a gRPC client connects and calls a gRPC function, the plugin will use the Freeplane Java API to create, delete, or modify the mind map. 
  Then returns the result to the gRPC client, such as the ID of a new node or the result code of the operation. 

  The diagram below illustrates the high-level architecture.

  ![image](https://github.com/metacoma/freeplane_plugin_grpc/blob/main/misc/grpc_plugin.png?raw=true)

  At this moment, Freeplane plugin gRPC provides following API (this is part of protobuf definition, the full protobuf file you can find (here) (here)[https://github.com/metacoma/freeplane_plugin_grpc/blob/ca8f667a0f373506c579762c92ffd954ca1827c7/src/main/proto/freeplane.proto]
```
service Freeplane {
  rpc CreateChild (CreateChildRequest) returns (CreateChildResponse) {};
  rpc DeleteChild (DeleteChildRequest) returns (DeleteChildResponse) {};
  rpc NodeAttributeAdd (NodeAttributeAddRequest) returns (NodeAttributeAddResponse) {};
  rpc NodeLinkSet (NodeLinkSetRequest) returns (NodeLinkSetResponse) {};
  rpc NodeDetailsSet (NodeDetailsSetRequest) returns (NodeDetailsSetResponse) {};
  rpc Groovy (GroovyRequest) returns (GroovyResponse) {}; // not ready 
  rpc NodeColorSet (NodeColorSetRequest) returns (NodeColorSetResponse) {};
  rpc NodeBackgroundColorSet (NodeBackgroundColorSetRequest) returns (NodeBackgroundColorSetResponse) {};
}
```

  As you can see, this plugin enables remote control of mind maps over the network, allowing you to send data to multiple Freeplane instances on different computers.

  ![image](https://github.com/metacoma/freeplane_plugin_grpc/blob/main/misc/freeplane_multinode.png?raw=true)
  
  And here is rough plan how i will use Freeplane and freeplane_plugin_grpc in (MindWM)[https://github.com/metacoma/mindwm]


  ![image](https://github.com/metacoma/freeplane_plugin_grpc/blob/main/misc/freeplane_network.png?raw=true)
 
**<a id="quick_start">5. Quick start</a>**

**<a id="install_plugin>">5.1 Install plugin</a>**
 
  Install pre-built plugin from [Release](https://github.com/metacoma/freeplane_plugin_grpc/releases) GitHub page.

**<a id="build_plugin">5.2 Build plugin</a>**

This plugin is built in the same manner as any other Freeplane Java plugin.

```bash
# clone the freeplane repository
$ git clone  https://github.com/freeplane/freeplane
$ cd freeplane
# clone this repo
$ git clone https://github.com/metacoma/freeplane_plugin_grpc
# Edit settings.gradle file in your favorite text editor and add the line 'freeplane_grpc'
$ head -n6 settings.gradle
rootProject.name='freeplane_root'
include 'freeplane',
        'freeplane_api',
        'freeplane_ant',
        'freeplane_framework',
        'freeplane_grpc',
# build plugin
$ cd freeplane_plugin_grpc
$ gradle build
# then run Freeplane
$ cd ../
$ BIN/freeplane.sh
# The following log line will indicate that plugin loaded, works, and waits new gRPC client on the TCP port 50051
....
Freeplane grpc plugin loaded and listen 50051 port
....
```

**<a id="add_new_grpc_function">5.3. Add new gRPC function</a>**
 
 Let's add a new gRPC function, "StatusInfoSet," similar to "c.StatusInfo = '...'", in Groovy. 

 The freeplane.Freeplane.StatusInfoSet gRPC function will accept a single argument, statusInfo, as a string and return a boolean status code. 

 If successful, the status code will be true; if failed, the status code will be false.

 1. Define a new gRPC service in the protobuf file.

 Open [main/src/main/proto/freeplane.proto](https://github.com/metacoma/freeplane_plugin_grpc/blob/main/src/main/proto/freeplane.proto) file in your favorite text editor and add the new StatusInfoSet function definition in the service section.

```
service Freeplane {
  rpc CreateChild (CreateChildRequest) returns (CreateChildResponse) {};
  ...
  rpc StatusInfoSet (StatusInfoSetRequest) returns (StatusInfoSetResponse) {};
}
message StatusInfoSetRequest {
  string statusInfo = 1;
}

message StatusInfoSetResponse {
  bool success = 1;
}
```

2. Next, let's implement gRPC function handler for StatusInfoSet function

```java
//Add this import in the import section in the head of the 
import org.freeplane.features.ui.ViewController;
//.......
// Add statusInfoSet public java method inside class FreeplaneImpl definition
                @Override
                public void statusInfoSet(StatusInfoSetRequest req, StreamObserver<StatusInfoSetResponse> responseObserver) {
                        boolean success = false;
                        final MapController mapController = Controller.getCurrentModeController().getMapController();
                        final String statusInfo = req.getStatusInfo();

                        if (statusInfo != null && statusInfo.length() > 0) {
                            success = true;
                            final ViewController viewController = Controller.getCurrentController().getViewController();
                            viewController.out(statusInfo);
                        }

                        StatusInfoSetResponse reply = StatusInfoSetResponse.newBuilder().setSuccess(success).build();
                        responseObserver.onNext(reply);
                        responseObserver.onCompleted();
                }

//....
```

3. That's it! Let's try to call our new gRPC function 

Shell example with using grpcurl cli tool
```bash 
# note, you need specify the path for freeplane.proto, because the current implementation of gRPC doesn't support reflective api 
$ echo '{"statusInfo": "hello from shell"}' | grpcurl -plaintext -proto ./freeplane.proto -d @ 127.0.0.1:50051 freeplane.Freeplane/StatusInfoSet
{
  "success": true
}
```
 
  For python and ruby, you need do "regenerate" freeplane grpc libraries:

  For more details, please check [python](https://grpc.io/docs/languages/python/basics/) and [ruby](https://grpc.io/docs/languages/ruby/basics/) basic gRPC tutorial.
  
```bash
$ cd freeplane/freeplane_plugin_grpc
# for python
$ python -m grpc_tools.protoc -Isrc/main/proto --python_out=grpc/python --pyi_out=grpc/python --grpc_python_out=grpc/python src/main/proto/freeplane.proto
# for ruby
$ grpc_tools_ruby_protoc -Isrc/main/proto --ruby_out=grpc/ruby/ruby_lib --grpc_out=grpc/ruby/ruby_lib src/main/proto/freeplane.proto
```

Python example:
```python
import grpc
import freeplane_pb2
import freeplane_pb2_grpc

channel = grpc.insecure_channel('localhost:50051')
fp = freeplane_pb2_grpc.FreeplaneStub(channel)

fp.StatusInfoSet(freeplane_pb2.StatusInfoSetRequest(statusInfo = "hello from python"))
```

Ruby: example:
```ruby
this_dir = File.expand_path(File.dirname(__FILE__))
lib_dir = File.join(this_dir, 'lib')
$LOAD_PATH.unshift(lib_dir) unless $LOAD_PATH.include?(lib_dir)

require 'grpc'
require 'freeplane_services_pb'

stub = Freeplane::Freeplane::Stub.new('localhost:50051', :this_channel_is_insecure)
stub.status_info_set(Freeplane::StatusInfoSetRequest.new(statusInfo: "hello from ruby"));
```

**<a id="limitations">6.0. Limitations of the current implementation</a>**

  * This plugin in active development and the gRPC API may be changed

  * gRPC TCP server port 50051 is hardcoded https://github.com/metacoma/freeplane_plugin_grpc/blob/ca8f667a0f373506c579762c92ffd954ca1827c7/src/main/java/org/freeplane/plugin/grpc/GrpcRegistration.java#L59-L65

  * Sometimes Freeplane throws a Java exception, [Java exception](https://github.com/metacoma/freeplane_plugin_grpc/issues/1), possibly due to race conditions or other factors.

  * gRPC groovy function have a stub and not implemented yet https://github.com/metacoma/freeplane_plugin_grpc/blob/ca8f667a0f373506c579762c92ffd954ca1827c7/src/main/java/org/freeplane/plugin/grpc/GrpcRegistration.java#L183-L196

  * This plugin I have tested only on the Linux. However, I don't anticipate any issues with it working on other operating systems.
