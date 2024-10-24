package cn.xbhel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String userId;
    private String username;
    private String gender;
    private String groupId;
    private String groupName;
    private Long activeDuration;
}
