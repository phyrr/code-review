package com.phyrr.codereview.infrastructure.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.phyrr.codereview.types.utils.RandomStringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.Stream;

public class GitCommand {

    private static final Logger logger = LoggerFactory.getLogger(GitCommand.class);

    private final String githubReviewLogUri;
    private final String githubToken;
    private final String project;
    private final String branch;
    private final String author;
    private final String message;

    public GitCommand(String githubReviewLogUri, String githubToken, String project,
                      String branch, String author, String message) {
        this.githubReviewLogUri = githubReviewLogUri;
        this.githubToken = githubToken;
        this.project = project;
        this.branch = branch;
        this.author = author;
        this.message = message;
    }

    public String diff() throws IOException, InterruptedException {
        Process logProcess = new ProcessBuilder("git", "log", "-1", "--pretty=format:%H")
                .directory(new File("."))
                .start();

        String latestCommitHash;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()))) {
            latestCommitHash = reader.readLine();
        }
        int logExitCode = logProcess.waitFor();
        if (logExitCode != 0 || latestCommitHash == null || latestCommitHash.isEmpty()) {
            throw new RuntimeException("Failed to get latest commit hash, exit code: " + logExitCode);
        }

        Process diffProcess = new ProcessBuilder("git", "diff", latestCommitHash + "^", latestCommitHash)
                .directory(new File("."))
                .start();

        StringBuilder diffCode = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(diffProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                diffCode.append(line).append('\n');
            }
        }

        int diffExitCode = diffProcess.waitFor();
        if (diffExitCode != 0) {
            throw new RuntimeException("Failed to get diff, exit code: " + diffExitCode);
        }

        return diffCode.toString();
    }

    public String commitAndPush(String review) throws Exception {
        File repoDir = new File("repo");
        deleteDirectory(repoDir);

        Git git = Git.cloneRepository()
                .setURI(githubReviewLogUri + ".git")
                .setDirectory(repoDir)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, ""))
                .call();

        try {
            String dateFolderName = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            File dateFolder = new File(repoDir, dateFolderName);
            if (!dateFolder.exists() && !dateFolder.mkdirs()) {
                throw new IOException("Failed to create folder: " + dateFolder.getAbsolutePath());
            }

            String fileName = project + "-" + branch + "-" + author + System.currentTimeMillis()
                    + "-" + RandomStringUtils.randomNumeric(4) + ".md";
            File newFile = new File(dateFolder, fileName);
            try (FileWriter writer = new FileWriter(newFile)) {
                writer.write(review);
            }

            git.add().addFilepattern(dateFolderName + "/" + fileName).call();
            git.commit().setMessage("add code review: " + fileName).call();
            git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(githubToken, "")).call();

            logger.info("review log pushed: {}", fileName);
            return githubReviewLogUri + "/blob/master/" + dateFolderName + "/" + fileName;
        } finally {
            git.close();
            deleteDirectory(repoDir);
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        try (Stream<Path> stream = Files.walk(directory.toPath()).sorted(Comparator.reverseOrder())) {
            stream.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete: " + path, e);
                }
            });
        }
    }

    public String getProject() {
        return project;
    }

    public String getBranch() {
        return branch;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

}
