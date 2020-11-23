package cn.coderblue.mongodb.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * @author coderblue
 */
@Document(collection = "UserInfo")
@Data
public class UserInfo {

        @Id
        private String id;
        @Field("USERNAME")
        private String username;
        private String password;
        private String phone;
        private String name;
        private String gender;
        private String age;
}


