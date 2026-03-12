package com.technicalassessment.btgpactual.repository;

import com.technicalassessment.btgpactual.model.Client;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Repository
public class ClientRepository {

    private final DynamoDbTable<Client> table;

    public ClientRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Clients", TableSchema.fromBean(Client.class));
    }

    public void save(Client client) {
        table.putItem(client);
    }

    public Optional<Client> findById(String clientId) {
        Client client = table.getItem(Key.builder().partitionValue(clientId).build());
        return Optional.ofNullable(client);
    }

    public DynamoDbTable<Client> getTable() {
        return table;
    }
}
