package cn.xbhel.model.dynamo;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@DynamoDbBean
public class Customer {
    private int customerId;
    private String customerName;
    private String gender;
    private long birthday;

    @DynamoDbPartitionKey
    public int getCustomerId() {
        return customerId;
    }
}