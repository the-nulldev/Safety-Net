import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SafetyNetStage5Test extends StageTest {

    private static final Path ROOT = Paths.get("").toAbsolutePath().getParent().getParent();
    private static final Path repoPath = ROOT.resolve("Safety-net-study-repository");

    private static Repository repository;

    @BeforeClass
    public static void setUp() throws IOException {
        File gitDir = new File(repoPath.toFile(), ".git");
        if (!gitDir.exists()) {
            throw new IOException("The path '" + repoPath + "' does not exist or is not a valid git repository!");
        }
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(gitDir)
                .readEnvironment()
                .findGitDir()
                .build();
    }

    @AfterClass
    public static void tearDown() {
        if (repository != null) {
            repository.close();
        }
    }

    @DynamicTest
    CheckResult test1() {
        if (repository == null) {
            return CheckResult.wrong("Repository is not initialized!");
        }
        return CheckResult.correct();
    }

    @DynamicTest
    CheckResult test2() {
        if (repository == null) {
            return CheckResult.wrong("Repository is not initialized!");
        }
        try (Git git = new Git(repository)) {
            List<String> branchList = List.of("main", "0.2.x-dev", "feature/case");
            List<String> branches = git.branchList().call().stream()
                    .map(ref -> ref.getName().replace("refs/heads/", ""))
                    .collect(Collectors.toList());
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
        if (repository == null) {
            return CheckResult.wrong("Repository is not initialized!");
        }
        String activeBranch = "feature/case";
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
        if (repository == null) {
            return CheckResult.wrong("Repository is not initialized!");
        }
        int expectedCommitCount = 7;
        try (Git git = new Git(repository)) {
            Iterable<RevCommit> commits = git.log().call();
            int actualCommitCount = 0;
            for (RevCommit commit : commits) {
                actualCommitCount++;
            }
            if (actualCommitCount != expectedCommitCount) {
                return CheckResult.wrong("You should have " + expectedCommitCount + " commits in the branch 'feature/case'! Found " + actualCommitCount + " commits.");
            }
        } catch (GitAPIException e) {
            return CheckResult.wrong("Error occurred while reading commits: " + e.getMessage());
        }
        return CheckResult.correct();
    }

    @DynamicTest
    CheckResult test5() {
        if (repository == null) {
            return CheckResult.wrong("Repository is not initialized!");
        }
        List<String> expectedMessages = List.of(
                "feat: Initial",
                "feat: new function get_numbers",
                "feat: new function get_letters",
                "new function upper",
                "new function title",
                "new function capitalize",
                "refactor: restored case operations from 6b2ec72"
        );
        try (Git git = new Git(repository)) {
            Iterable<RevCommit> commits = git.log().call();
            List<String> commitMessages = new ArrayList<>();
            for (RevCommit commit : commits) {
                commitMessages.add(commit.getFullMessage().trim());
            }
            for (String expectedMessage : expectedMessages) {
                if (commitMessages.stream().noneMatch(msg -> msg.contains(expectedMessage))) {
                    return CheckResult.wrong("Commit `" + expectedMessage + "` was not found in the branch 'feature/case'!");
                }
            }
        } catch (GitAPIException e) {
            return CheckResult.wrong("Error occurred while reading commits: " + e.getMessage());
        }
        return CheckResult.correct();
    }

    @DynamicTest
    CheckResult test6() {
        if (repository == null) {
            return CheckResult.wrong("Repository is not initialized!");
        }
        String filePath = "case_operations.py";
        List<String> expectedContentParts = List.of(
                "def make_upper",
                "print(text.upper())"
        );
        try (Git git = new Git(repository)) {
            ObjectId head = repository.resolve("HEAD");
            RevCommit commit = repository.parseCommit(head);
            RevTree tree = commit.getTree();
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(filePath));
                if (!treeWalk.next()) {
                    return CheckResult.wrong("'" + filePath + "' not found in commit " + commit.getId().getName());
                }
                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = repository.open(objectId);
                String fileContent = new String(loader.getBytes(), StandardCharsets.UTF_8).trim();
                String normalizedContent = fileContent.replaceAll("\\s+", " ");
                for (String expectedPart : expectedContentParts) {
                    if (!normalizedContent.contains(expectedPart)) {
                        return CheckResult.wrong("The file '" + filePath + "' is missing expected content: '" + expectedPart + "'");
                    }
                }
            }
        } catch (IOException e) {
            return CheckResult.wrong("Error while reading the file content from the commit tree: " + e.getMessage());
        }
        return CheckResult.correct();
    }
}