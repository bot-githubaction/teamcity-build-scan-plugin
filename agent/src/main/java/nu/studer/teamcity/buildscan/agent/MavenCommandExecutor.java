package nu.studer.teamcity.buildscan.agent;

import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.ToolCannotBeFoundException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Executes Maven commands and returns their output, both standard output and standard error, when given a {@link BuildRunnerContext}.
 */
final class MavenCommandExecutor {

    private final BuildRunnerContext runnerContext;

    MavenCommandExecutor(BuildRunnerContext context) {
        this.runnerContext = context;
    }

    @NotNull
    Result execute(String args) throws IOException, InterruptedException {
        File mavenExec = getMvnExec();
        if (mavenExec == null) {
            return new Result();
        }

        String command = mavenExec.getAbsolutePath() + " " + args;
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" ")).redirectErrorStream(true);
        Process process = processBuilder.start();
        process.waitFor();

        return new Result(process);
    }

    @Nullable
    private File getMvnExec() {
        String mavenPath;
        try {
            mavenPath = runnerContext.getToolPath("maven");
        } catch (ToolCannotBeFoundException e) {
            return null;
        }

        File installationBinDir = new File(mavenPath, "bin");
        String mvnExecutableName = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
        return new File(installationBinDir, mvnExecutableName);
    }

    public static class Result {

        private final Process process;
        private String output;

        public Result() {
            process = null;
        }

        public Result(Process process) {
            this.process = process;
        }

        public boolean isSuccessful() {
            if (process == null) {
                return false;
            }

            return process.exitValue() == 0;
        }

        @NotNull
        public String getOutput() throws IOException {
            if (process == null || !isSuccessful()) {
                return "";
            }

            if (output == null) {
                // this logic eagerly consumes the entire output into memory, which should not be an issue when only
                // used for `mvn --version`, which generates ~5 lines of output
                // this should be revisited if other commands are executed here
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                int ch;
                while ((ch = reader.read()) != -1) {
                    sb.append((char) ch);
                }

                output = sb.toString();
            }

            return output;
        }

    }

}
