package com.technicalassessment.btgpactual.repository;

import com.technicalassessment.btgpactual.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Optional;

@Repository
public class UserRepository {

    private final DynamoDbTable<User> table;
    private final DynamoDbIndex<User> usernameIndex;

    public UserRepository(DynamoDbEnhancedClient enhancedClient,
                          @Value("${aws.dynamodb.table.users:Users}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(User.class));
        this.usernameIndex = table.index("username-index");
    }

    public DynamoDbTable<User> getTable() {
        return table;
    }

    public void save(User user) {
        table.putItem(user);
    }

    public Optional<User> findById(String userId) {
        return Optional.ofNullable(table.getItem(Key.builder().partitionValue(userId).build()));
    }

    public Optional<User> findByUsername(String username) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(username).build());

        return usernameIndex.query(queryConditional)
                .stream()
                .flatMap(page -> page.items().stream())
                .findFirst();
    }
}
