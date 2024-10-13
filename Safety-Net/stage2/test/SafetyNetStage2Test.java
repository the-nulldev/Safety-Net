import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.hyperskill.hstest.stage.StageTest;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.testcase.CheckResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SafetyNetStage2Test extends StageTest {

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
        // Test if all branches exist in the local repository
        try (Git git = new Git(repository)) {
            List<String> branchList = List.of("main", "0.2.x-dev", "feature/math");

            // Fetch the list of branch names, stripping any "refs/heads/" prefix
            List<String> branches = git.branchList().call().stream()
                    .map(ref -> ref.getName().replace("refs/heads/", ""))
                    .toList();

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
        // Test if the active branch is 'feature/math'
        String activeBranch = "feature/math";
        try {
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
        // Test if the commit count in the 'feature/math' branch is 4
        int expectedCommitCount = 4;
        try (Git git = new Git(repository)) {
            Iterable<RevCommit> commits = git.log().call();
            int actualCommitCount = 0;
            for (RevCommit ignored : commits) {
                actualCommitCount++;
            }
            if (actualCommitCount != expectedCommitCount) {
                return CheckResult.wrong("You should have " + expectedCommitCount + " commits in the branch 'feature/math'! Ensure you created a local copy of the repository from origin! Found " + actualCommitCount + " commits.");
            }
        } catch (GitAPIException e) {
            return CheckResult.wrong("Error occurred while reading commits: " + e.getMessage());
        }

        return CheckResult.correct();
    }

    @DynamicTest
    CheckResult test5() {
        // Test if the commit messages are correct
        List<String> expectedMessages = List.of(
                "feat: Initial",
                "feat: new function get_numbers",
                "feat: new function get_letters",
                "feat: new function addition"
        );
        try (Git git = new Git(repository)) {
            Iterable<RevCommit> commits = git.log().call();
            List<String> commitMessages = new ArrayList<>();
            for (RevCommit commit : commits) {
                commitMessages.add(commit.getFullMessage().trim());
            }

            for (String expectedMessage : expectedMessages) {
                if (commitMessages.stream().noneMatch(msg -> msg.contains(expectedMessage))) {
                    return CheckResult.wrong("Commit `" + expectedMessage + "` was not found in the branch 'feature/math'!");
                }
            }
        } catch (GitAPIException e) {
            return CheckResult.wrong("Error occurred while reading commits: " + e.getMessage());
        }

        return CheckResult.correct();
    }

    @DynamicTest
    CheckResult test6() {
        // Test the committed file and its content
        String filePath = "math_operations.py";

        // Define key parts of the content that we expect to be in the file
        List<String> expectedContentParts = new ArrayList<>();
        expectedContentParts.add("def addition");

        try (Git git = new Git(repository)) {
            // Ensure the branch 'feature/math' is checked out
            git.checkout().setName("feature/math").call();

            // Get the latest commit (HEAD) on the 'feature/math' branch
            ObjectId head = repository.resolve("HEAD");
            RevCommit commit = repository.parseCommit(head);

            // Get the tree of the commit
            RevTree tree = commit.getTree();

            // Use TreeWalk to find the file in the tree
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(filePath));

                if (!treeWalk.next()) {
                    return CheckResult.wrong("'" + filePath + "' not found in commit " + commit.getId().getName());
                }

                // Get the object ID of the file and load its content
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                String fileContent = new String(loader.getBytes(), StandardCharsets.UTF_8).trim();

                // Normalize the file content by removing extra spaces and newlines
                String normalizedContent = fileContent.replaceAll("\\s+", " ");

                // Check if the key parts of the expected content are present in the file
                for (String expectedPart : expectedContentParts) {
                    if (!normalizedContent.contains(expectedPart)) {
                        return CheckResult.wrong("The file '" + filePath + "' is missing expected content: '" + expectedPart + "'");
                    }
                }
            }
        } catch (IOException | GitAPIException e) {
            return CheckResult.wrong("Error while reading the file content from the commit tree: " + e.getMessage());
        }

        return CheckResult.correct();
    }

}
