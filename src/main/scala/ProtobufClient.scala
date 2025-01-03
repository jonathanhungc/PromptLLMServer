import user.UserRequest
import user.UserResponse

import com.twitter.finagle.Http
import com.twitter.util.Await
import io.finch._
import io.finch.syntax._
import io.circe.generic.auto._
import io.finch.circe._

import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory

/**
 * This class creates a server using Finch, which calls an API Gateway associated with a Lambda function
 * hosted in AWS, using Protobuf data as the exchanging mechanism. After the server is running, the client
 * sends a GET request with some input text, the server calls the API Gateway by sending a POST request with
 * data serialized from Protobuf, triggers the lambda function, the function calls Amazon Bedrock to generate
 * a response to the input text from the client, serializes the response into a Protobuf object and returns
 * it to the Finch server, which sends an OK response to the client with the text generated by Amazon Bedrock.
 */
object ProtobufClient {
  private val log = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()

  // This is the GET request endpoint for the server used to call the lambda function
  val LLMlambda: Endpoint[String] = get("generate" :: param("input")) { (input: String) =>

    if (input.trim.isEmpty) {
      // Handle empty input by returning a BadRequest with an appropriate message
      log.warn("Received empty input from user")
      BadRequest(new Exception("Input cannot be empty"))
    } else {

      log.info("Input from user: " + input)

      // Create the Protobuf request message
      val request = UserRequest(userInput = input)

      val serializedRequest = request.toByteArray

      // Send the POST request to API Gateway with request
      val response = scalaj.http.Http("https://ghfng1llqj.execute-api.us-east-1.amazonaws.com/dev/protobuf")
        .timeout(connTimeoutMs = config.getInt("app.connTimeout"), readTimeoutMs = config.getInt("app.readTimeout")) // Specify timeouts for connections and responses
        .postData(serializedRequest)
        .header("Content-Type", "application/x-protobuf")
        .header("Accept", "application/x-protobuf")
        .asBytes

      // Parse response from the lambda function into userResponse object, and get result text
      val userResponse = UserResponse.parseFrom(response.body).result

      log.info("Response from Lambda: " + userResponse)

      // Send OK response with text from the lambda function
      Ok(userResponse)
    }
  }

  def main(args: Array[String]): Unit = {

    // Set the endpoint for the API
    val api = LLMlambda

    // Start the Finagle server
    log.info("Starting the Finagle server on port 8081...")
    Await.ready(Http.server.serve(":8081", api.toService))
  }
}
