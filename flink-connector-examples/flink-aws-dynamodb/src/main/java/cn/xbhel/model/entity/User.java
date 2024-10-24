package cn.xbhel.model.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@Accessors(chain = true)
@DynamoDbBean
public class User {

    private int userId;
    private String username;
    private String gender;
    private long birthday;

    @DynamoDbPartitionKey
    public int getUserId() {
        return userId;
    }
}
