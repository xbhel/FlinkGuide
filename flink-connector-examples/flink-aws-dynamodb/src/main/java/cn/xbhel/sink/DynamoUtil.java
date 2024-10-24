package cn.xbhel.sink;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


public final class DynamoUtil {

    private DynamoUtil() throws InstantiationException {
        throw new InstantiationException("The class cannot be instantiated.");
    }

    public static DynamoDbEnhancedClient getInstance() {
        return Singleton.US_EAST_1_INSTANCE;
    }

    public static DynamoDbEnhancedClient client(Region region) {
        var client = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(region)
                .build();
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    private static class Singleton {
        private static final DynamoDbEnhancedClient US_EAST_1_INSTANCE = client(Region.US_EAST_1);
    }

}
