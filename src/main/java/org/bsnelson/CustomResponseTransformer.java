package org.bsnelson;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

public class CustomResponseTransformer extends ResponseTransformer {

    @Override
    public String getName() {
        return "custom-response-transformer";
    }

    private String extractFieldFromRequestBody(String requestBody) {
        // Implement your logic to extract the field from the request body
        // For example, using a JSON library like Jackson or Gson
        // Here is a simple example assuming the field is directly in the request body
        return requestBody; // Replace with actual extraction logic
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public Response transform(Request request, Response response, FileSource fileSource, Parameters parameters) {
        String requestBody = request.getBodyAsString();
        // Extract the field from the request body (assuming JSON format)
        String extractedField = extractFieldFromRequestBody(requestBody);

        // Create a new response body with the extracted field
        String responseBody = "{\"extractedField\":\"" + extractedField + "\"}";

        return Response.Builder.like(response)
                .but()
                .body(responseBody)
                .build();
    }
}