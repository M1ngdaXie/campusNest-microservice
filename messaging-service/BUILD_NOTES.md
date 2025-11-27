# Messaging Service Build Notes

## Build Requirements

### Java Version
This service MUST be built with Java 17, not Java 24 or newer versions.

The project uses:
- **Java 17** - Target version
- **Lombok 1.18.34** - Annotation processor
- **maven-compiler-plugin 3.11.0** - Compatible with Apple Silicon

### Build Command

To build the messaging-service from the parent directory:

```bash
JAVA_HOME=/Users/xiemingda/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home mvn clean install -DskipTests -pl messaging-service -am
```

Or set JAVA_HOME for your shell session:

```bash
export JAVA_HOME=/Users/xiemingda/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home
mvn clean install -DskipTests -pl messaging-service -am
```

### Known Issues

1. **TypeTag::UNKNOWN Error**: Occurs when building with Java 24. This is due to internal changes in the Java compiler that are incompatible with maven-compiler-plugin and Lombok. Solution: Use Java 17.

2. **maven-compiler-plugin 3.13.0**: The default version from Spring Boot 3.3.5 parent has compatibility issues on Apple Silicon. We explicitly use version 3.11.0 in the pom.xml.

## Maven Configuration

The pom.xml includes explicit configuration to ensure compatibility:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>${java.version}</source>
        <target>${java.version}</target>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

## Microservices Architecture

This service implements a microservices pattern with:
- **Decoupled Entities**: Stores IDs instead of JPA relationships
- **Feign Clients**: Cross-service communication with user-service and housing-service
- **Separate Database**: Uses `campusNest_messaging` database
- **WebSocket/STOMP**: Real-time messaging
- **Redis**: Caching and user presence tracking

## Next Steps

After successful build:
1. Update docker-compose.yml if needed
2. Test the service with Docker Compose
3. Verify Feign client connectivity to user-service and housing-service