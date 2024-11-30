# Homework 3:
### Name: Jonathan Hung

#### UIC email: jhung9@uic.edu

This program provides a RESTul service for LLM interactions. The framework used is Finch/Finagle. The client sends a GET
request to this server, the server sends a POST request to an Amazon API Gateway integrated with a Lambda Function in AWS.
The workflow is the following: 

1. The client makes a GET request to the Finch server
2. The server serializes the client request in a Protobuf object as part of the POST request
3. Sends the POST request to the API Gateway integrated with the lambda function
4. The Lambda Function deserializes the request to get the client input
5. Uses this input to make calls to Amazon Bedrock and generate a response to the input
6. Serializes the response from the model into a Protobuf object
7. Sends a response to the Finch server
8. The server deserializes this Protobuf object to extract the text response from Amazon Bedrock
9. Sends an OK response to the client with the response from Amazon Bedrock

These are the relevant files for the project:

- **src/main/scala/ProtobufClient.scala**: This file contains the code for the Finch server. It provides a GET endpoint
to take input from clients, serializes/deserializes Protobuf data, and interacts with the API Gateway integrated with
the Lambda function that uses Amazon Bedrock to generate responses.
- **src/main/protobuf/user.proto**: This file contains the structure for the Protobuf object used for serialization. The
program generates the required .scala files and classes from this .proto file.
- **src/main/resources/application.conf**: This file sets different variables in the program.
- **src/test/scala/ProtobufClientTest.scala**: This file contains some test cases for the Finch server. Make sure that
the server is running at localhost:8081 before running the test file.
- **build.sbt**: This file has all the dependencies of the program, including Finch, ScalaPB, etc.
- 
### To run the program execute the following commands. This will start the server:
```
sbt clean
sbt compile
sbt run
```
### In another terminal, you can run this to test the server. The input text needs to use URL encoding to work properly:
```
curl "http://localhost:8081/generate?input=What%20is%20cloud%20computing%3F"
```

### The video of the deployment on AWS is found [here](https://youtu.be/A7zg2CNXHAk)