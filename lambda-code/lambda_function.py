import json
import boto3
import logging
import base64
from google.protobuf.message import DecodeError
from user_pb2 import UserRequest, UserResponse

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Bedrock client initialization
bedrock_client = boto3.client('bedrock-runtime', region_name='us-east-1')

def lambda_handler(event, context):
    # Log event to check request
    logger.info("Received full event: %s", json.dumps(event, indent=2))

    body = event.get('body', '') # Extract headers and body from the event# Extract headers and body from the event

    try:
        if event.get('isBase64Encoded', False): # Decode Base64 if indicated
            body = base64.b64decode(body)
            logger.info("Decoded message of body: %s", body)

        user_request = UserRequest.FromString(body) # Parse Protobuf object from body

        input_text = user_request.user_input    # Extract user input
        logger.info("Extracted user input: %s", input_text)

        prompt = f"\n\nHuman: {input_text}\n\nAssistant:"   # Format prompt

        # Call the Bedrock model
        try:
            response = bedrock_client.invoke_model(
            modelId='anthropic.claude-v2',
            body=json.dumps({
                "prompt": prompt,
                "max_tokens_to_sample": 400
            }),
            contentType='application/json'
        )

            logger.info(("Response from Bedrock:", response))

            # Read and decode content
            response_body = json.loads(response['body'].read().decode('utf-8'))

            # Extract the generated text from the response
            model_response = response_body.get('completion', 'No response generated')

            logger.info("Model response from Bedrock: %s", model_response)

        except Exception as bedrock_error:
            logger.error("Error invoking Bedrock model: %s", str(bedrock_error))
            model_response = "Error invoking model."

        # Create the Protobuf response message
        user_response = UserResponse(result=model_response)

        # Return the Protobuf response
        return {
            'statusCode': 200,
            'body': base64.b64encode(user_response.SerializeToString()).decode('utf-8'),
            'isBase64Encoded': True,
            'headers': {
                'Content-Type': 'application/x-protobuf',
            }
        }

    except DecodeError as e:
        logger.error("Failed to decode Protobuf message: %s", str(e))
        return {
            'statusCode': 400,
            'body': json.dumps({"error": f"Invalid Protobuf data: {str(e)}"})
        }

    except Exception as e:
        logger.error("An unexpected error occurred: %s", str(e))
        return {
            'statusCode': 500,
            'body': json.dumps({"error": str(e)})
        }
