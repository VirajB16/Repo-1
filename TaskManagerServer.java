import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.*;

public class TaskManagerServer {
    private final int PORT = 8080;
    private TaskService taskService;
    private Gson gson;

    public TaskManagerServer() {
        this.taskService = new TaskService();
        this.gson = new Gson();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // API Routes
        server.createContext("/api/tasks", this::handleTasks);
        
        // Static file serving
        server.createContext("/", this::handleStaticFiles);
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("Server started on http://localhost:" + PORT);
    }

    private void handleTasks(HttpExchange exchange) throws IOException {
        // Enable CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
            return;
        }

        try {
            switch (method) {
                case "GET":
                    handleGetTasks(exchange);
                    break;
                case "POST":
                    handleCreateTask(exchange);
                    break;
                case "PUT":
                    handleUpdateTask(exchange, path);
                    break;
                case "DELETE":
                    handleDeleteTask(exchange, path);
                    break;
                default:
                    sendResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleGetTasks(HttpExchange exchange) throws IOException {
        String json = gson.toJson(taskService.getAllTasks());
        sendJsonResponse(exchange, 200, json);
    }

    private void handleCreateTask(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject jsonObject = gson.fromJson(requestBody, JsonObject.class);
        
        String title = jsonObject.get("title").getAsString();
        String description = jsonObject.get("description").getAsString();
        
        Task task = taskService.createTask(title, description);
        String json = gson.toJson(task);
        sendJsonResponse(exchange, 201, json);
    }

    private void handleUpdateTask(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            sendResponse(exchange, 400, "Invalid task ID");
            return;
        }
        
        int taskId = Integer.parseInt(parts[3]);
        String requestBody = readRequestBody(exchange);
        JsonObject jsonObject = gson.fromJson(requestBody, JsonObject.class);
        
        String title = jsonObject.has("title") ? jsonObject.get("title").getAsString() : null;
        String description = jsonObject.has("description") ? jsonObject.get("description").getAsString() : null;
        Boolean completed = jsonObject.has("completed") ? jsonObject.get("completed").getAsBoolean() : null;
        
        Task task = taskService.updateTask(taskId, title, description, completed);
        if (task != null) {
            String json = gson.toJson(task);
            sendJsonResponse(exchange, 200, json);
        } else {
            sendResponse(exchange, 404, "Task not found");
        }
    }

    private void handleDeleteTask(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            sendResponse(exchange, 400, "Invalid task ID");
            return;
        }
        
        int taskId = Integer.parseInt(parts[3]);
        boolean deleted = taskService.deleteTask(taskId);
        
        if (deleted) {
            sendResponse(exchange, 200, "Task deleted");
        } else {
            sendResponse(exchange, 404, "Task not found");
        }
    }

    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        
        try {
            Path filePath = Paths.get("frontend" + path);
            if (Files.exists(filePath)) {
                byte[] content = Files.readAllBytes(filePath);
                String contentType = getContentType(path);
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, content.length);
                exchange.getResponseBody().write(content);
            } else {
                sendResponse(exchange, 404, "File not found");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "Error serving file");
        }
        exchange.close();
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "text/plain";
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        sendResponse(exchange, statusCode, json);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.close();
    }

    public static void main(String[] args) {
        try {
            new TaskManagerServer().start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}