package org.bsnelson;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import org.json.JSONObject;

public class CustomResponseTransformer extends ResponseTransformer {

    @Override
    public String getName() {
        return "custom-response-transformer";
    }

    private String extractFieldFromRequestBody(String requestBody) {
        // Implement your logic to extract the field from the request body
        // For example, using a JSON library like Jackson or Gson
        // Here is a simple example assuming the field is directly in the request body
        JSONObject jsonObject = new JSONObject(requestBody);

        // Extract the "position" field
        int position = (((jsonObject.getInt("Position") + 9) / 10) * 10);
        return Integer.toString(position); // Replace with actual extraction logic
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }

    @Override
    public Response transform(Request request, Response response, FileSource fileSource, Parameters parameters) {
        String requestBody = request.getBodyAsString();
        // Extract the field from the request body (assuming JSON format)
        String position = extractFieldFromRequestBody(requestBody);

        String responseBody = "{\"device\":{\"idDevice\":" + parameters.getString("deviceId") + ",\"name\": \"" + parameters.getString("deviceName") + "\",\"position\":" + position + "}}";
        // Create a new response body with the extracted field
        //String responseBody = "{\"Position\":\"" + extractedField + "\"}";

        return Response.Builder.like(response)
                .but()
                .body(responseBody)
                .build();
    }
}