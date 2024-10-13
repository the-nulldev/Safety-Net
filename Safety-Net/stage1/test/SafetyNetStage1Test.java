import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class SafetyNetStage1Test extends StageTest {

    private static final Path ROOT = Paths.get("").toAbsolutePath().getParent().getParent();
    private static final Path repoPath = ROOT.resolve("Safety-net-study-repository");

    private Repository repository;

    @DynamicTest
    CheckResult test1() {
        // Test if the path is a valid git repository
        File gitDir = new File(repoPath.toFile(), ".git");
        if (!gitDir.exists()) {
            return CheckResult.wrong("The expected path '" + repoPath + "' does not exist or is not a valid git repository!");
        }

        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repository = builder.setGitDir(gitDir)
                    .readEnvironment()
                    .findGitDir()
                    .build();

        } catch (IOException e) {
            return CheckResult.wrong("The path '" + repoPath + "' does not exist or is not a valid git repository!");
        }

        return CheckResult.correct();
    }

    @DynamicTest
    CheckResult test2() {
        // Test if all branches exist in the local repository
        try (Git git = new Git(repository)) {
            List<String> branchList = List.of("0.2.x-dev", "main");

            // Fetch the list of branch names, stripping any "refs/heads/" prefix
            List<String> branches = git.branchList().call().stream()
                    .map(ref -> ref.getName().replace("refs/heads/", ""))
                    .collect(Collectors.toList());

            // Check if each required branch exists in the local repository
            for (String branch : branchList) {
                if (!branches.contains(branch)) {
                    return CheckResult.wrong(branch + " is missing!");
                }
            }
        } catch (GitAPIException e) {
            return CheckResult.wrong("Error occurred while reading branches: " + e.getMessage());
        }

        return CheckResult.correct();
    }


    @DynamicTest
    CheckResult test3() {
        // Test if the active branch is '0.2.x-dev'
        String activeBranch = "0.2.x-dev";
        try (Git git = new Git(repository)) {
            String currentBranch = repository.getBranch();
            if (!currentBranch.equals(activeBranch)) {
                return CheckResult.wrong("Active branch is not '" + activeBranch + "'!");
            }
        } catch (IOException e) {
            return CheckResult.wrong("Error occurred while reading the active branch: " + e.getMessage());
        }

        return CheckResult.correct();
    }
}
