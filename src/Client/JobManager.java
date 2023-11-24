package Client;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class JobManager {
    private String dirPathJobs;
    private String dirPathJobResults;
    private Map<String, String> jobs;  // <Name, Path>
    
    public JobManager(String dirPathJobs, String dirPathJobResults) {
        this.dirPathJobs = dirPathJobs;
        this.dirPathJobResults = dirPathJobResults;
        this.jobs = new HashMap<>();
    }

    public void readDirectory() throws IOException {
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(this.dirPathJobs));
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

    public void saveJobResult(String jobName, long id, byte[] result) throws IOException {
        Path path = Paths.get(this.dirPathJobResults, jobName + "_" + id + ".dat");
        Files.write(path, result);
    }

    public List<String> getJobNames() {
        return new ArrayList<>(this.jobs.keySet());
    }
}