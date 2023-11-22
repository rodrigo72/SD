package Client;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class Jobs {
    private String directoryPath;
    private Map<String, String> jobs;  // <Name, Path>
    
    public Jobs(String directoryPath) {
        this.directoryPath = directoryPath;
        this.jobs = new HashMap<>();
    }

    public void readDirectory() throws IOException {
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(this.directoryPath));
        for (Path path : directoryStream) {
            if (Files.isRegularFile(path) && path.toString().endsWith(".class")) {
                String jobName = path.getFileName().toString();
                String jobPath = path.toString();
                this.jobs.put(jobName, jobPath);
            }
        }
    }

    public byte[] getJob(String jobName) {
        String filePath = this.jobs.get(jobName);
        if (filePath != null) {
            try {
                return Files.readAllBytes(Paths.get(filePath));
            } catch (IOException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public List<String> getJobNames() {
        return new ArrayList<>(this.jobs.keySet());
    }
}
