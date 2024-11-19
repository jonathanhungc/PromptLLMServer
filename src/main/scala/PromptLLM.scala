import com.twitter.finagle.Http
import com.twitter.util.Await
import io.finch._
import io.finch.syntax._
import io.circe.generic.auto._
import io.finch.circe._
import org.deeplearning4j.util.ModelSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.api.ndarray.INDArray
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork

object PromptLLM extends App {

  val log = LoggerFactory.getLogger(getClass)

  val config = ConfigFactory.load()

  val word2Vec = WordVectorSerializer.readWord2VecModel("src/main/resources/word-vectors-medium.txt")
  log.info("Word2Vec model loaded successfully.")

  val network: MultiLayerNetwork = ModelSerializer.restoreMultiLayerNetwork("src/main/resources/model.zip")
  log.info("Neural network model loaded successfully.")

  // Function to get vector representation of input text as a 2D array
  def getVectorRepresentation(inputText: String, model: Word2Vec): INDArray = {
    val words = inputText.split(" ")

    // Create a 2D array with each word as a separate row
    val wordVectors = words.flatMap { word =>
      if (model.hasWord(word)) Some(model.getWordVectorMatrix(word).toDoubleVector)
      else None // Ignore words not in the vocabulary
    }

    // Convert the list of word vectors to an INDArray (2D array)
    val vectorArray2D = if (wordVectors.isEmpty) {
      // If no valid words are found, return an empty 2D array with shape (0, vectorSize)
      Nd4j.zeros(1, model.getLayerSize)
    } else {
      Nd4j.create(wordVectors).transpose()
    }

    // Convert the 2D array to a 3D array with batch size of 1
    vectorArray2D.reshape(1, vectorArray2D.rows(), vectorArray2D.columns())
  }

  // Function to predict the next word using the neural network model
  def generateNextWord(inputText: String): String = {
    // Get the 2D vector representation of the input sequence
    val inputVector = getVectorRepresentation(inputText.toLowerCase, word2Vec)

    // Ensure the input is not empty
    if (inputVector.isEmpty) return "Unknown"

    // Pass the sequence to the neural network for prediction
    val output : INDArray = network.output(inputVector)

    log.info("Network Output: " + output)

    // Find the closest word in the Word2Vec space
    val closestWord = word2Vec.wordsNearest(output,1)
    closestWord.toArray.head.toString
  }


  def generateSentence(inputText: String, numWords: Int = 5): String = {
    val sentence = new StringBuilder(inputText.trim.toLowerCase)

    // Generate each word one at a time and append it to the sentence
    (1 to numWords).foreach { _ =>
      // Use the current sentence as context for generating the next word
      val nextWord = generateNextWord(sentence.toString())

      // Break if no valid next word is found
      if (nextWord == "Unknown" || nextWord.isEmpty) {
        return sentence.toString()
      }

      // Append the predicted word to the sentence
      sentence.append(s" $nextWord")
    }

    sentence.toString()
  }


  // Define API endpoints
  val hello: Endpoint[String] = get("hello") { Ok("Hello, World!") }

  // Endpoint to generate the next word
  val nextWordEndpoint: Endpoint[String] = get("predict" :: param("text")) { (text: String) =>
    val nextWord = generateNextWord(text)
    Ok(nextWord)
  }

  // Endpoint to generate a full sentence
  val sentenceEndpoint: Endpoint[String] = get("generate" :: param("text")) { (text: String) =>
    log.info(s"Received text: $text")
    val sentence = generateSentence(text)
    Ok(sentence)
  }

  // Combine all endpoints
  val api = hello :+: nextWordEndpoint :+: sentenceEndpoint

  // Start the Finagle server
  log.info("Starting the Finagle server on port 8081...")
  Await.ready(Http.server.serve(":8081", api.toService))
}
