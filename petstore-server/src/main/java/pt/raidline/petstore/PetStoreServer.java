package pt.raidline.petstore;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import pt.raidline.petstore.model.Model.CreatePetRequest;
import pt.raidline.petstore.model.Model.Pet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PetStoreServer {

    // Simulating a Database
    private static final Map<Long, Pet> petStore = new ConcurrentHashMap<>();
    private static final AtomicLong petIdGenerator = new AtomicLong(1);
    private static final AtomicInteger reqCount = new AtomicInteger(0);


    public static void main(String[] args) throws IOException {
        // Create server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // JAVA 25: Use Virtual Threads for high concurrency
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        int limitReqCount = new Random().nextInt(10);
        // Define Routes
        server.createContext("/pets", new PetHandler(limitReqCount));
        server.createContext("/users", new UserHandler(limitReqCount));

        System.out.println("🚀 PetStore Server started on http://localhost:8080");
        server.start();
    }

    // --- Handlers ---

    static class PetHandler implements HttpHandler {
        private final int limitReqCount;

        PetHandler(int limitReqCount) {
            this.limitReqCount = limitReqCount;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (reqCount.incrementAndGet() == limitReqCount) {
                reqCount.set(0);
                //sendResponse(exchange, 500, "Some error : oops");
                System.exit(1);
                return;
            }
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // Route: /pets vs /pets/{id}
            String[] parts = path.split("/");

            try {
                if (parts.length == 2 && path.equals("/pets")) {
                    // Collection Operations
                    switch (method) {
                        case "GET" -> handleListPets(exchange);
                        case "POST" -> handleCreatePet(exchange);
                        default -> sendResponse(exchange, 405, "Method Not Allowed");
                    }
                } else if (parts.length == 3 && path.startsWith("/pets/")) {
                    // Item Operations
                    long petId = Long.parseLong(parts[2]);
                    switch (method) {
                        case "GET" -> handleGetPet(exchange, petId);
                        case "DELETE" -> handleDeletePet(exchange, petId);
                        default -> sendResponse(exchange, 405, "Method Not Allowed");
                    }
                } else {
                    sendResponse(exchange, 404, "Not Found");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal Server Error");
            }
        }

        private void handleListPets(HttpExchange exchange) throws IOException {
            // Logic: Return all values from the map
            List<Pet> pets = new ArrayList<>(petStore.values());

            // Query Params (simple simulation)
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("status=")) {
                // Filter logic would go here
            }

            String jsonResponse = JsonUtils.toJson(pets);
            sendResponse(exchange, 200, jsonResponse);
        }

        private void handleCreatePet(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            CreatePetRequest req = JsonUtils.fromJson(body, CreatePetRequest.class);

            if (req.name() == null || req.name().isEmpty()) {
                sendResponse(exchange, 400, "{\"code\": \"400\", \"message\": \"Invalid input\"}");
                return;
            }

            // Create new Pet
            long id = petIdGenerator.getAndIncrement();
            Pet newPet = new Pet(
                    id,
                    req.name(),
                    null, // Category placeholder
                    req.status() != null ? req.status() : "available",
                    List.of(), // Tags placeholder
                    List.of(), // Photos placeholder
                    LocalDateTime.now()
            );

            petStore.put(id, newPet);
            sendResponse(exchange, 201, JsonUtils.toJson(newPet));
        }

        private void handleGetPet(HttpExchange exchange, long id) throws IOException {
            Pet pet = petStore.get(id);
            if (pet != null) {
                sendResponse(exchange, 200, JsonUtils.toJson(pet));
            } else {
                sendResponse(exchange, 404, "{\"code\": \"404\", \"message\": \"Pet not found\"}");
            }
        }

        private void handleDeletePet(HttpExchange exchange, long id) throws IOException {
            // Check API Key header
            if (!exchange.getRequestHeaders().containsKey("X-Api-Key")) {
                sendResponse(exchange, 401, "{\"code\": \"401\", \"message\": \"Unauthorized\"}");
                return;
            }

            if (petStore.remove(id) != null) {
                sendResponse(exchange, 204, "");
            } else {
                sendResponse(exchange, 404, "{\"code\": \"404\", \"message\": \"Pet not found\"}");
            }
        }
    }

    static class UserHandler implements HttpHandler {
        private final int limitReqCount;

        UserHandler(int limitReqCount) {
            this.limitReqCount = limitReqCount;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (reqCount.incrementAndGet() == limitReqCount) {
                reqCount.set(0);
                sendResponse(exchange, 501, "{\"message\": \"Not Implemented in Demo\"}");
            } else {
                sendResponse(exchange, 200, "{\"message\": \"Implemented in Demo\"}");
            }

        }
    }

    // --- Helpers ---

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length > 0 ? bytes.length : -1);
        try (OutputStream os = exchange.getResponseBody()) {
            if (bytes.length > 0) os.write(bytes);
        }
    }

    /**
     * Mock JSON Utility.
     * In a real project, use Jackson (ObjectMapper) or Gson.
     * This is just to make the code compile and run for the demo without dependencies.
     */
    static class JsonUtils {
        static String toJson(Object obj) {
            // Very naive string representation for demo purposes
            return obj.toString();
        }

        @SuppressWarnings("unchecked")
        static <T> T fromJson(String json, Class<T> clazz) {
            // In a real app, this would parse JSON strings to Records.
            // For this demo, we return a dummy object to prevent crashes.
            if (clazz == CreatePetRequest.class) {
                return (T) new CreatePetRequest("Doggie", 1L, "available", List.of());
            }
            return null;
        }
    }
}
