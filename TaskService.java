import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.*;

public class TaskService {
    private final String DATA_FILE = "backend/data/tasks.json";
    private List<Task> tasks;
    private AtomicInteger nextId;
    private Gson gson;

    public TaskService() {
        this.gson = new Gson();
        this.tasks = new ArrayList<>();
        this.nextId = new AtomicInteger(1);
        loadTasks();
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public Task getTaskById(int id) {
        return tasks.stream()
                .filter(task -> task.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Task createTask(String title, String description) {
        Task task = new Task(nextId.getAndIncrement(), title, description);
        tasks.add(task);
        saveTasks();
        return task;
    }

    public Task updateTask(int id, String title, String description, Boolean completed) {
        Task task = getTaskById(id);
        if (task != null) {
            if (title != null) task.setTitle(title);
            if (description != null) task.setDescription(description);
            if (completed != null) task.setCompleted(completed);
            saveTasks();
        }
        return task;
    }

    public boolean deleteTask(int id) {
        boolean removed = tasks.removeIf(task -> task.getId() == id);
        if (removed) {
            saveTasks();
        }
        return removed;
    }

    private void loadTasks() {
        try {
            Path path = Paths.get(DATA_FILE);
            if (Files.exists(path)) {
                String json = Files.readString(path);
                Task[] taskArray = gson.fromJson(json, Task[].class);
                if (taskArray != null) {
                    tasks.addAll(Arrays.asList(taskArray));
                    // Set next ID based on existing tasks
                    int maxId = tasks.stream().mapToInt(Task::getId).max().orElse(0);
                    nextId.set(maxId + 1);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading tasks: " + e.getMessage());
        }
    }

    private void saveTasks() {
        try {
            Path path = Paths.get(DATA_FILE);
            Files.createDirectories(path.getParent());
            String json = gson.toJson(tasks);
            Files.writeString(path, json);
        } catch (Exception e) {
            System.err.println("Error saving tasks: " + e.getMessage());
        }
    }
}