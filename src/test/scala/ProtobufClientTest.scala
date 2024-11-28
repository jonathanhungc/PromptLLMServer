import org.scalatest.funsuite.AnyFunSuite
import scalaj.http.Http
import user.{UserRequest, UserResponse}
import com.typesafe.config.ConfigFactory

/**
 * This class contains the different tests for the implementation of the Finch server, the requests it sends,
 * the responses it receives, and the serialization/deserialization of Protobuf data it performs.
 *
 * Before running tests, make sure to run the server at localhost:8081, so the server is available for the tests.
 */
class ProtobufClientTest extends AnyFunSuite {
  private val config = ConfigFactory.load()

  // Testing that the server is providing a valid response
  test("Server should return a valid response for a valid input") {
    val input = "Hello"
    val response = Http("http://localhost:8081/generate")
      .param("input", input)
      .timeout(connTimeoutMs = config.getInt("app.connTimeout"), readTimeoutMs = config.getInt("app.readTimeout"))
      .asString

    assert(response.code == 200)
    assert(response.body.nonEmpty)
  }

  // Testing for bad request when the input parameter is missing
  test("Server should return a BadRequest for missing 'input' parameter") {
    val response = Http("http://localhost:8081/generate").asString
    assert(response.code == 400) // Check if the response code is 400 (BadRequest) for empty input
  }

  // Testing the Protobuf serialization/deserialization. It creates a UserRequest, serializes it and deserializes it
  test("Protobuf serialization/deserialization should be consistent") {
    val input = "Test Input"
    val request = UserRequest(userInput = input)
    val serialized = request.toByteArray
    val deserialized = UserRequest.parseFrom(serialized)
    assert(deserialized.userInput == input) // Check that original and deserialized data are the same
  }

  // Testing that the server returns a response that is valid and relevant to the input given
  test("Server should return non-empty and valid response content") {
    val input = "What is a dog?"
    val response = Http("http://localhost:8081/generate")
      .param("input", input)
      .timeout(connTimeoutMs = config.getInt("app.connTimeout"), readTimeoutMs = config.getInt("app.readTimeout"))
      .asString

    assert(response.code == 200)
    assert(response.body.nonEmpty)

    // Checking for specific words
    assert(response.body.contains("Dog") || response.body.contains("dog"))
  }

  // Testing server response for empty input
  test("Server should return a BadRequest for empty input") {
    val input = ""
    val response = Http("http://localhost:8081/generate")
      .param("input", input)
      .timeout(connTimeoutMs = config.getInt("app.connTimeout"), readTimeoutMs = config.getInt("app.readTimeout"))
      .asString

    assert(response.code == 400) // Check if the response code is 400 (BadRequest) for empty input
  }

}
