import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.testcase.CheckResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SafetyNetStage4Test extends org.hyperskill.hstest.stage.StageTest {

    private static final Path ROOT = Paths.get("").toAbsolutePath().getParent().getParent();
    private static final Path repoPath = ROOT.resolve("Safety-net-study-repository");

    private Repository repository;

    @DynamicTest
    CheckResult test1() {
        // Test if the path is a valid git repository
        File gitDir = new File(repoPath.toFile(), ".git");
        if (!gitDir.exists()) {
            return CheckResult.wrong("The path '" + repoPath + "' does not exist or is not a valid git repository!");
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
        // Test if all required branches exist in the local repository
        try (Git git = new Git(repository)) {
            List<String> branchList = List.of("main", "0.2.x-dev");

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
        // Test if the active branch is 'main'
        String activeBranch = "main";
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

    @DynamicTest
    CheckResult test4() {
        // Test if the commit count in the 'main' branch is 1 (after reset)
        int expectedCommitCount = 1;
        try (Git git = new Git(repository)) {
            Iterable<RevCommit> commits = git.log().call();
            int actualCommitCount = 0;
            for (RevCommit commit : commits) {
                actualCommitCount++;
            }
            if (actualCommitCount != expectedCommitCount) {
                return CheckResult.wrong("You should have " + expectedCommitCount + " commits in the branch 'main'! Found " + actualCommitCount + " commits.");
            }
        } catch (GitAPIException e) {
            return CheckResult.wrong("Error occurred while reading commits: " + e.getMessage());
        }

        return CheckResult.correct();
    }

    @DynamicTest
    CheckResult test5() {
        // Test if the commit messages in the 'main' branch are correct (only the initial commit should remain)
        List<String> expectedMessages = List.of(
                "feat: Initial"
        );
        try (Git git = new Git(repository)) {
            Iterable<RevCommit> commits = git.log().call();
            List<String> commitMessages = new ArrayList<>();
            for (RevCommit commit : commits) {
                commitMessages.add(commit.getFullMessage().trim());
            }

            for (String expectedMessage : expectedMessages) {
                if (commitMessages.stream().noneMatch(msg -> msg.contains(expectedMessage))) {
                    return CheckResult.wrong("Commit `" + expectedMessage + "` was not found in the branch 'main'!");
                }
            }
        } catch (GitAPIException e) {
            return CheckResult.wrong("Error occurred while reading commits: " + e.getMessage());
        }

        return CheckResult.correct();
    }

    @DynamicTest
    CheckResult test6() {
        // Test if the commit count in the '0.2.x-dev' branch is 4 (after cherry-pick)
        String branchName = "0.2.x-dev";
        int expectedCommitCount = 4;
        try (Git git = new Git(repository)) {
            // Switch to the '0.2.x-dev' branch
            git.checkout().setName(branchName).call();

            Iterable<RevCommit> commits = git.log().call();
            int actualCommitCount = 0;
            for (RevCommit commit : commits) {
                actualCommitCount++;
            }
            if (actualCommitCount != expectedCommitCount) {
                return CheckResult.wrong("You should have " + expectedCommitCount + " commits in the branch '" + branchName + "'! Found " + actualCommitCount + " commits.");
            }
        } catch (GitAPIException e) {
            return CheckResult.wrong("Error occurred while reading commits: " + e.getMessage());
        }

        return CheckResult.correct();
    }

    @DynamicTest
    CheckResult test7() {
        // Test if the commit messages in the '0.2.x-dev' branch are correct
        String branchName = "0.2.x-dev";
        List<String> expectedMessages = List.of(
                "feat: Initial",
                "feat: new function get_numbers",
                "feat: new function get_letters",
                "feat: new function addition"
        );
        try (Git git = new Git(repository)) {
            // Ensure the branch is '0.2.x-dev'
            git.checkout().setName(branchName).call();

            Iterable<RevCommit> commits = git.log().call();
            List<String> commitMessages = new ArrayList<>();
            for (RevCommit commit : commits) {
                commitMessages.add(commit.getFullMessage().trim());
            }

            for (String expectedMessage : expectedMessages) {
                if (commitMessages.stream().noneMatch(msg -> msg.contains(expectedMessage))) {
                    return CheckResult.wrong("Commit `" + expectedMessage + "` was not found in the branch '" + branchName + "'!");
                }
            }
        } catch (GitAPIException e) {
            return CheckResult.wrong("Error occurred while reading commits: " + e.getMessage());
        }

        return CheckResult.correct();
    }
}
