package com.technicalassessment.btgpactual.config;

import com.technicalassessment.btgpactual.model.Client;
import com.technicalassessment.btgpactual.model.Fund;
import com.technicalassessment.btgpactual.model.Subscription;
import com.technicalassessment.btgpactual.model.Transaction;
import com.technicalassessment.btgpactual.model.User;
import com.technicalassessment.btgpactual.repository.ClientRepository;
import com.technicalassessment.btgpactual.repository.FundRepository;
import com.technicalassessment.btgpactual.repository.SubscriptionRepository;
import com.technicalassessment.btgpactual.repository.TransactionRepository;
import com.technicalassessment.btgpactual.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // GUIDs fijos para datos seed (reproducibles)
    public static final String DEFAULT_CLIENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    public static final String FUND_1_ID = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";
    public static final String FUND_2_ID = "05be699e-e5a6-421b-8d00-b24ba68393e8";
    public static final String FUND_3_ID = "1a3c7f77-c471-41dc-b295-615076beaef7";
    public static final String FUND_4_ID = "d780a4fb-d7e9-4906-bbe1-306cdc9298c8";
    public static final String FUND_5_ID = "d97687eb-3ac5-4155-903a-9a96856acd8e";
    public static final String DEFAULT_USER_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    public static final String ADMIN_USER_ID = "f9e8d7c6-b5a4-3210-fedc-ba9876543210";

    private final DynamoDbClient dynamoDbClient;
    private final ClientRepository clientRepository;
    private final FundRepository fundRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(DynamoDbClient dynamoDbClient,
                      ClientRepository clientRepository,
                      FundRepository fundRepository,
                      TransactionRepository transactionRepository,
                      SubscriptionRepository subscriptionRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.dynamoDbClient = dynamoDbClient;
        this.clientRepository = clientRepository;
        this.fundRepository = fundRepository;
        this.transactionRepository = transactionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createTables();
        seedFunds();
        seedDefaultClient();
        seedUsers();
    }

    private void createTables() {
        createTableIfNotExists("Clients", () -> clientRepository.getTable().createTable());

        createTableIfNotExists("Funds", () -> fundRepository.getTable().createTable());

        createTableIfNotExists("Transactions", () ->
                transactionRepository.getTable().createTable(CreateTableEnhancedRequest.builder()
                        .globalSecondaryIndices(
                                EnhancedGlobalSecondaryIndex.builder()
                                        .indexName("clientId-index")
                                        .projection(Projection.builder()
                                                .projectionType(ProjectionType.ALL)
                                                .build())
                                        .build(),
                                EnhancedGlobalSecondaryIndex.builder()
                                        .indexName("subscriptionId-index")
                                        .projection(Projection.builder()
                                                .projectionType(ProjectionType.ALL)
                                                .build())
                                        .build()
                        )
                        .build())
        );

        createTableIfNotExists("Subscriptions", () -> subscriptionRepository.getTable().createTable());

        createTableIfNotExists("Users", () ->
                userRepository.getTable().createTable(CreateTableEnhancedRequest.builder()
                        .globalSecondaryIndices(
                                EnhancedGlobalSecondaryIndex.builder()
                                        .indexName("username-index")
                                        .projection(Projection.builder()
                                                .projectionType(ProjectionType.ALL)
                                                .build())
                                        .build()
                        )
                        .build())
        );
    }

    private void createTableIfNotExists(String tableName, Runnable createAction) {
        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
            log.info("Tabla '{}' ya existe", tableName);
        } catch (ResourceNotFoundException e) {
            createAction.run();
            log.info("Tabla '{}' creada exitosamente", tableName);
        }
    }

    private void seedFunds() {
        List<Fund> funds = List.of(
                Fund.builder().fundId(FUND_1_ID).name("FPV_BTG_PACTUAL_RECAUDADORA").minimumAmount(75000.0).category("FPV").build(),
                Fund.builder().fundId(FUND_2_ID).name("FPV_BTG_PACTUAL_ECOPETROL").minimumAmount(125000.0).category("FPV").build(),
                Fund.builder().fundId(FUND_3_ID).name("DEUDAPRIVADA").minimumAmount(50000.0).category("FIC").build(),
                Fund.builder().fundId(FUND_4_ID).name("FDO-ACCIONES").minimumAmount(250000.0).category("FIC").build(),
                Fund.builder().fundId(FUND_5_ID).name("FPV_BTG_PACTUAL_DINAMICA").minimumAmount(100000.0).category("FPV").build()
        );

        funds.forEach(fund -> {
            fundRepository.save(fund);
            log.info("Fondo '{}' registrado (ID: {})", fund.getName(), fund.getFundId());
        });
    }

    private void seedDefaultClient() {
        if (clientRepository.findById(DEFAULT_CLIENT_ID).isEmpty()) {
            Client client = Client.builder()
                    .clientId(DEFAULT_CLIENT_ID)
                    .name("Cliente Demo")
                    .email("demo@btgpactual.com")
                    .phone("+573001234567")
                    .notificationPreference("EMAIL")
                    .balance(500000.0)
                    .build();
            clientRepository.save(client);
            log.info("Cliente demo creado (ID: {}) con saldo COP $500.000", DEFAULT_CLIENT_ID);
        }
    }

    private void seedUsers() {
        if (userRepository.findById(DEFAULT_USER_ID).isEmpty()) {
            User clientUser = User.builder()
                    .userId(DEFAULT_USER_ID)
                    .username("cliente1")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role("CLIENT")
                    .clientId(DEFAULT_CLIENT_ID)
                    .build();
            userRepository.save(clientUser);
            log.info("Usuario CLIENT creado: cliente1 (ID: {})", DEFAULT_USER_ID);
        }

        if (userRepository.findById(ADMIN_USER_ID).isEmpty()) {
            User adminUser = User.builder()
                    .userId(ADMIN_USER_ID)
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .clientId(null)
                    .build();
            userRepository.save(adminUser);
            log.info("Usuario ADMIN creado: admin (ID: {})", ADMIN_USER_ID);
        }
    }
}
