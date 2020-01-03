package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/** @author Riddhi Bagadiaa
 * Main command in the Repository. */

@SuppressWarnings("unchecked")
public class Repository {

    /** Initialises Repository with ARGS. */
    public static void init(String... args) {
        checkArgs("init", 1, args);
        if (!Main.MAIN_FOLDER.exists()) {
            Main.MAIN_FOLDER.mkdirs();
            Main.BRANCH_HEADS.mkdirs();
            Main.STAGING_AREA.mkdirs();
            Commit.COMMITS_FOLDER.mkdirs();
            Main.REMOVE_FROM_COMMIT.mkdirs();
            makeSubDirs();
            Utils.writeContents(Main.CURRENT_BRANCH, "master");

            makeCommit("initial commit", null, null, null, null);
        } else {
            System.out.println("A Gitlet version-control"
                    + " system already exists in the current directory.");
            System.exit(0);
        }
    }

    /** Makes subdirectories. */
    private static void makeSubDirs() {
        Utils.join(Commit.COMMITS_FOLDER, "0").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "1").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "2").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "3").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "4").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "5").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "6").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "7").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "8").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "9").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "a").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "b").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "c").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "d").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "e").mkdirs();
        Utils.join(Commit.COMMITS_FOLDER, "f").mkdirs();
    }

    /** Adds to the Repository with ARGS. */
    public static void add(String... args) {
        checkArgs("add", 2, args);

        String fileName = args[1];
        File addedFile = Utils.join(Main.STAGING_AREA, fileName);
        File fileInDir = Utils.join(Main.CWD, fileName);

        String headPath = Utils.readContentsAsString(Main.HEAD);
        Commit headCommit = Utils.readObject(new File(headPath), Commit.class);
        HashMap<String, byte[]> blobs = headCommit.getFileToBlob();

        if (!blobs.keySet().contains(fileName) && !fileInDir.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        if (Utils.plainFilenamesIn(Main.REMOVE_FROM_COMMIT)
                .contains(fileName)) {
            Utils.join(Main.REMOVE_FROM_COMMIT, fileName).delete();
            return;
        }

        byte[] contentsOfDirFile;
        if (!fileInDir.exists()) {
            contentsOfDirFile = null;
        } else {
            contentsOfDirFile = Utils.readContents(fileInDir);
        }

        if (blobs.containsKey(fileName)) {
            if (Arrays.equals(blobs.get(fileName), contentsOfDirFile)) {
                if (Utils.plainFilenamesIn(Main.STAGING_AREA)
                        .contains(fileName)) {
                    addedFile.delete();
                }
                return;
            }
        }

        if (addedFile.exists()) {
            if (Arrays.equals(Utils.readContents(
                    addedFile), contentsOfDirFile)) {
                return;
            }
        }
        Utils.writeContents(addedFile, contentsOfDirFile);
    }

    /** Commits to the Repository with ARGS. */
    public static void commit(String... args) {
        if (args.length < 2 || args[1].length() == 0) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        checkArgs("commit", 2, args);

        String logMessage = args[1];
        List<String> addedFiles = new ArrayList<>();
        List<String> removedFiles = new ArrayList<>();
        if (Utils.plainFilenamesIn(Main.STAGING_AREA).isEmpty()
                && Utils.plainFilenamesIn(Main.REMOVE_FROM_COMMIT).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        } else {
            addedFiles = Utils.plainFilenamesIn(Main.STAGING_AREA);
            removedFiles = Utils.plainFilenamesIn(Main.REMOVE_FROM_COMMIT);

        }
        File currentHead = new File(Utils.readContentsAsString(Main.HEAD));
        Commit headCommit = Utils.readObject(currentHead, Commit.class);

        makeCommit(logMessage, headCommit, addedFiles, removedFiles, null);
    }

    /** Makes the commit with the given LOG,
     *  PREVCOMMIT, ADDEDFILES, REMOVEDFILES, SECONDPARENT. */
    private static void makeCommit(String log, Commit prevCommit,
                                   List<String> addedFiles, List<String>
                                           removedFiles, String secondParent) {
        HashMap<String, byte[]> fileToBlob = new HashMap();
        if (prevCommit != null) {
            for (String originalFiles : prevCommit.getFileToBlob().keySet()) {
                if (removedFiles == null
                        || !removedFiles.contains(originalFiles)) {
                    fileToBlob.put(originalFiles, prevCommit.
                            getFileToBlob().get(originalFiles));
                }
                Utils.join(Main.REMOVE_FROM_COMMIT, originalFiles).delete();
            }
        }
        if (addedFiles != null) {
            String newContent = "";
            for (String fileName : addedFiles) {
                File txtFile = Utils.join(Main.STAGING_AREA, fileName);
                byte[] blob = Utils.readContents(txtFile);
                fileToBlob.put(fileName, blob);
                txtFile.delete();
                newContent = newContent + "\n" + fileName;
            }
        }
        Commit c;
        if (prevCommit == null) {
            c = new Commit(log, null, "master", fileToBlob, secondParent);
        } else {
            c = new Commit(log, prevCommit.getSHA1(),
                    Utils.readContentsAsString(Main.CURRENT_BRANCH),
                    fileToBlob, secondParent);
        }
        byte[] cArray = Utils.serialize(c);
        String sha1 = c.getSHA1();
        String subDir = sha1.substring(0, 1);
        File dir = Utils.join(Commit.COMMITS_FOLDER, subDir);
        Utils.writeContents(Utils.join(dir, sha1), cArray);
        updateHeads(Utils.join(dir, sha1));
    }

    /** Updates heads of the branch with the given FILE. */
    public static void updateHeads(File file) {
        Utils.writeContents(Main.HEAD, file.getPath());
        Utils.writeContents(Utils.join(Main.BRANCH_HEADS,
                Utils.readContentsAsString(Main.CURRENT_BRANCH)),
                file.getPath());
    }

    /** Gives log of Repository with ARGS. */
    public static void log(String... args) {
        checkArgs("log", 1, args);

        File currentHead = new File(Utils.readContentsAsString(Main.HEAD));
        Commit headCommit = Utils.readObject(currentHead, Commit.class);
        while (headCommit.getParent() != null) {
            System.out.println("===");
            System.out.println("commit " + headCommit.getSHA1());
            if (headCommit.getSecondParent() != null) {
                System.out.println("Merge: " + headCommit.getParent()
                        .substring(0, 7)
                        + " " + headCommit.getSecondParent().substring(0, 7));
            }
            System.out.println("Date: " + headCommit.getTimeStamp());
            System.out.println(headCommit.getMessage());
            System.out.println();

            File parentFile = Utils.join(Commit.COMMITS_FOLDER,
                    headCommit.getParent().substring(0, 1),
                    headCommit.getParent());
            headCommit = Utils.readObject(parentFile, Commit.class);
        }

        System.out.println("===");
        System.out.println("commit " + headCommit.getSHA1());
        System.out.println("Date: " + headCommit.getTimeStamp());
        System.out.println(headCommit.getMessage());
        System.out.println();

    }

    /** Checkout Repository parts with ARGS. */
    public static void checkout(String... args) {
        if (args.length == 3) {

            checkArgs("checkout", 3, args);
            if (args[1].equals("--")) {
                File currentHead = new File(
                        Utils.readContentsAsString(Main.HEAD));
                Commit headCommit = Utils.readObject(currentHead, Commit.class);
                checkoutFile(args[2], headCommit);
            } else {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
        } else if (args.length == 4) {

            checkArgs("checkout", 4, args);
            if (args[2].equals("--")) {
                File commitSubDir = Utils.join(Commit.COMMITS_FOLDER,
                        args[1].substring(0, 1));

                String commitID = ifFileExists(args[1], commitSubDir);

                File getCommit = Utils.join(commitSubDir, commitID);
                Commit commitObject = Utils.readObject(getCommit, Commit.class);
                checkoutFile(args[3], commitObject);
            } else {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }

        } else if (args.length == 2) {
            checkArgs("checkout", 2, args);
            checkoutBranch(args[1]);
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /** Checks if file with ID in SUBDIR exists return the file. */
    private static String ifFileExists(String id, File subDir) {

        int sha1length = id.length();
        boolean exists = false;
        String commitID = "";
        if (sha1length != FORTY) {
            for (String commitName : Utils.plainFilenamesIn(subDir)) {
                if (commitName.substring(0, sha1length).equals(id)) {
                    exists = true;
                    commitID = commitName;
                    break;
                }
            }
            if (!exists) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
        } else {
            if (!Utils.plainFilenamesIn(subDir).contains(id)) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            } else {
                commitID = id;
            }
        }
        return commitID;
    }

    /** Checkout file with FILENAME and THECOMMIT. */
    private static void checkoutFile(String fileName, Commit theCommit) {
        HashMap<String, byte[]> committedFiles = theCommit.getFileToBlob();
        if (committedFiles.containsKey(fileName)) {
            File fileInDir = new File(Main.CWD, fileName);
            if (committedFiles.get(fileName) == null) {
                fileInDir.delete();
                return;
            }
            byte[] changeContentTo = committedFiles.get(fileName);

            Utils.writeContents(fileInDir, new String(changeContentTo));
            if (Utils.plainFilenamesIn(Main.STAGING_AREA).contains(fileName)) {
                Utils.join(Main.STAGING_AREA, fileName).delete();
            }
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    /** Checkout branch with BRANCHNAME. */
    private static void checkoutBranch(String branchName) {
        if (!Utils.plainFilenamesIn(Main.BRANCH_HEADS).contains(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        List<String> filesForRemoval =
                Utils.plainFilenamesIn(Main.REMOVE_FROM_COMMIT);
        while (filesForRemoval.size() != 0) {
            Utils.writeContents(Utils.join(Main.CWD, filesForRemoval.get(0)),
                    Utils.readContentsAsString(Utils.join(
                            Main.REMOVE_FROM_COMMIT, filesForRemoval.get(0))));
            Utils.join(Main.REMOVE_FROM_COMMIT,
                    filesForRemoval.get(0)).delete();
            filesForRemoval.remove(0);
        }
        String headPath = Utils.readContentsAsString(Main.HEAD);
        Commit headCommit = Utils.readObject(new File(headPath), Commit.class);

        String branchHeadPath = Utils.readContentsAsString(
                Utils.join(Main.BRANCH_HEADS, branchName));
        Commit branchHead = Utils.readObject(
                new File(branchHeadPath), Commit.class);

        if (branchName.equals(Utils.
                readContentsAsString(Main.CURRENT_BRANCH))) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        for (String f : Utils.plainFilenamesIn(Main.CWD)) {
            if (!f.equals(".gitlet") && !headCommit.
                    getFileToBlob().containsKey(f))  {
                if (branchHead.getFileToBlob().containsKey(f)) {
                    byte[] workingDirFileContents =
                            Utils.readContents(Utils.join(Main.CWD, f));
                    if (!Arrays.equals(workingDirFileContents,
                            branchHead.getFileToBlob().get(f))) {
                        System.out.println("There is an untracked file "
                                + "in the way; delete it or add it first.");
                        System.exit(0);
                    }
                }
            } else if (!f.equals(".gitlet")) {
                Utils.join(Main.CWD, f).delete();
            }
        }

        for (String f : branchHead.getFileToBlob().keySet()) {
            Utils.writeContents(Utils.join(Main.CWD, f),
                    new String(branchHead.getFileToBlob().get(f)));
        }

        for (String f : Utils.plainFilenamesIn(Main.STAGING_AREA)) {
            Utils.join(Main.STAGING_AREA, f).delete();
        }

        Utils.writeContents(Main.HEAD, branchHeadPath);
        Utils.writeContents(Main.CURRENT_BRANCH, branchName);
    }

    /** Makes new Branch with ARGS. */
    public static void branch(String... args) {
        checkArgs("branch", 2, args);
        String newBranch = args[1];
        if (Utils.plainFilenamesIn(Main.BRANCH_HEADS).contains(newBranch)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        File thisBranch = Utils.join(Main.BRANCH_HEADS, newBranch);
        Utils.writeContents(thisBranch, Utils.readContentsAsString(Main.HEAD));
    }

    /** Removes Branch with ARGS. */
    public static void rmBranch(String... args) {
        checkArgs("rm-branch", 2, args);
        String branchToRemove = args[1];
        if (branchToRemove.equals(Utils.
                readContentsAsString(Main.CURRENT_BRANCH))) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        if (!Utils.plainFilenamesIn(Main.BRANCH_HEADS).
                contains(branchToRemove)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        File deleteBranch = Utils.join(Main.BRANCH_HEADS, branchToRemove);
        deleteBranch.delete();
    }

    /** Gives global log with ARGS. */
    public static void globalLog(String... args) {
        checkArgs("global-log", 1, args);
        for (File dirName : Commit.COMMITS_FOLDER.
                listFiles(File::isDirectory)) {
            if (Utils.plainFilenamesIn(dirName).size() != 0) {
                for (String commitName : Utils.plainFilenamesIn(dirName)) {
                    File commitFile = Utils.join(dirName, commitName);
                    Commit commitObject = Utils.readObject(
                            commitFile, Commit.class);
                    System.out.println("===");
                    System.out.println("commit " + commitObject.getSHA1());
                    if (commitObject.getSecondParent() != null) {
                        System.out.println("Merge: " + commitObject.
                                getParent().substring(0, 7)
                                + " " + commitObject.
                                getSecondParent().substring(0, 7));
                    }
                    System.out.println("Date: " + commitObject.getTimeStamp());
                    System.out.println(commitObject.getMessage());
                    System.out.println();
                }
            }
        }
    }

    /** Finds commits with ARGS. */
    public static void find(String... args) {
        checkArgs("find", 2, args);
        String commitMessage = args[1];
        int count = 0;
        for (String subDir : Commit.COMMITS_FOLDER.list()) {
            if (Utils.plainFilenamesIn(Utils.join(
                    Commit.COMMITS_FOLDER, subDir)).size() != 0) {
                for (String commitNames : Utils.plainFilenamesIn(
                        Utils.join(Commit.COMMITS_FOLDER, subDir))) {
                    File commitFile = Utils.join(
                            Commit.COMMITS_FOLDER, subDir, commitNames);
                    Commit commitObject = Utils.readObject(
                            commitFile, Commit.class);
                    if (commitObject.getMessage().equals(commitMessage)) {
                        System.out.println(commitObject.getSHA1());
                        count += 1;
                    }
                }
            }
        }

        if (count == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Removes file with ARGS. */
    public static void rm(String... args) {
        checkArgs("rm", 2, args);
        String removeFile = args[1];

        File currentHead = new File(Utils.readContentsAsString(Main.HEAD));
        Commit headCommit = Utils.readObject(currentHead, Commit.class);

        if (!headCommit.getFileToBlob().keySet().contains(removeFile)
                && !Utils.plainFilenamesIn(
                        Main.STAGING_AREA).contains(removeFile)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        } else if (headCommit.getFileToBlob().keySet().contains(removeFile)) {

            byte[] rmFileContent = headCommit.getFileToBlob().get(removeFile);
            Utils.writeContents(Utils.join(
                    Main.REMOVE_FROM_COMMIT, removeFile), rmFileContent);

            if (Utils.join(Main.CWD, removeFile).exists()) {
                Utils.join(Main.CWD, removeFile).delete();
            }
        }

        if (Utils.plainFilenamesIn(Main.STAGING_AREA).contains(removeFile)) {
            Utils.join(Main.STAGING_AREA, removeFile).delete();
        }
    }

    /** Resets commit with ARGS. */
    public static void reset(String... args) {
        checkArgs("reset", 2, args);
        File commitSubDir = Utils.join(
                Commit.COMMITS_FOLDER, args[1].substring(0, 1));
        String commitID = ifFileExists(args[1], commitSubDir);

        List<String> filesForRemoval =
                Utils.plainFilenamesIn(Main.REMOVE_FROM_COMMIT);
        while (filesForRemoval.size() != 0) {
            Utils.writeContents(Utils.join(Main.CWD, filesForRemoval.get(0)),
                    Utils.readContentsAsString(Utils.join
                            (Main.REMOVE_FROM_COMMIT, filesForRemoval.get(0))));
            Utils.join(Main.REMOVE_FROM_COMMIT,
                    filesForRemoval.get(0)).delete();
            filesForRemoval.remove(0);
        }

        String headPath = Utils.readContentsAsString(Main.HEAD);
        Commit headCommit = Utils.readObject(new File(headPath), Commit.class);

        Commit goToCommit = Utils.readObject(
                Utils.join(commitSubDir, commitID), Commit.class);

        for (String f : Utils.plainFilenamesIn(Main.CWD)) {
            if (!f.equals(".gitlet")
                    && !headCommit.getFileToBlob().containsKey(f))  {
                if (goToCommit.getFileToBlob().containsKey(f)) {
                    byte[] workingDirFileContents =
                            Utils.readContents(Utils.join(Main.CWD, f));
                    if (!Arrays.equals(workingDirFileContents,
                            goToCommit.getFileToBlob().get(f))) {
                        System.out.println("There is an untracked"
                                + " file in the way; "
                                + "delete it or add it first.");
                        System.exit(0);
                    }
                }
            } else if (!f.equals(".gitlet")) {
                Utils.join(Main.CWD, f).delete();
            }
        }

        for (String f : goToCommit.getFileToBlob().keySet()) {
            Utils.writeContents(Utils.join(Main.CWD, f),
                    new String(goToCommit.getFileToBlob().get(f)));
        }

        for (String f : Utils.plainFilenamesIn(
                Main.STAGING_AREA)) {
            Utils.join(Main.STAGING_AREA, f).delete();
        }

        String currBranch = Utils.readContentsAsString(Main.CURRENT_BRANCH);
        String path = Utils.join(commitSubDir, commitID).getPath();
        Utils.writeContents(Utils.join(Main.BRANCH_HEADS, currBranch), path);
        Utils.writeContents(Main.HEAD, path);
    }

    /** Gives status of repository with ARGS. */
    public static void status(String... args) {
        checkArgs("status", 1, args);
        HashSet<String> a = new HashSet<>();
        String c = Utils.readContentsAsString(Main.CURRENT_BRANCH);
        System.out.println("=== Branches ===");
        for (String branchName : Utils.plainFilenamesIn(Main.BRANCH_HEADS)) {
            a.add(branchName);
            if (c.equals(branchName)) {
                branchName = "*" + branchName;
            }
            System.out.println(branchName);
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String stagedFile : Utils.plainFilenamesIn(Main.STAGING_AREA)) {
            System.out.println(stagedFile);
            a.add(stagedFile);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String removedFile : Utils.plainFilenamesIn(
                Main.REMOVE_FROM_COMMIT)) {
            System.out.println(removedFile);
            a.add(removedFile);
        }
        System.out.println();
        String headPath = Utils.readContentsAsString(Main.HEAD);
        Commit h = Utils.readObject(new File(headPath), Commit.class);
        Set<String> committedFiles = h.getFileToBlob().keySet();
        ArrayList<String> notStagedForCommit = new ArrayList<>();
        for (String f : committedFiles) {
            if (!Utils.join(Main.CWD, f).exists() && !a.contains(f)) {
                a.add(f);
                f += " (deleted)";
                notStagedForCommit.add(f);
            } else if (Utils.join(Main.CWD, f).exists()
                    && !a.contains(f)
                    && !Utils.readContentsAsString(Utils.join(Main.CWD, f))
                            .equals(new String(h.getFileToBlob().get(f)))) {
                a.add(f);
                f += " (modified)";
                notStagedForCommit.add(f);
            } else {
                a.add(f);
            }
        }
        Collections.sort(notStagedForCommit);
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String f : notStagedForCommit) {
            System.out.println(f);
        }
        System.out.println();
        List<String> dirFiles = Utils.plainFilenamesIn(Main.CWD);
        System.out.println("=== Untracked Files ===");
        for (String f : dirFiles) {
            if (!a.contains(f)) {
                System.out.println(f);
            }
        }
    }

    /** Merge two branches with ARGS. */
    public static void merge(String... args) {
        checkArgs("merge", 2, args);
        String givenBranchName = args[1];
        String currentBranchName =
                Utils.readContentsAsString(Main.CURRENT_BRANCH);
        m1(givenBranchName);
        m2(givenBranchName, currentBranchName);
        m3();
        String currHeadPath = Utils.readContentsAsString
                (Utils.join(Main.BRANCH_HEADS, currentBranchName));
        Commit currBranch = Utils.readObject(
                new File(currHeadPath), Commit.class);
        String givenHeadPath = Utils.readContentsAsString
                (Utils.join(Main.BRANCH_HEADS, givenBranchName));
        Commit givenBranch = Utils.readObject(
                new File(givenHeadPath), Commit.class);
        String splitPointSHA1 = splitPointHelper(currBranch, givenBranch);
        Commit splitPoint = Utils.readObject(Utils.join
                (Commit.COMMITS_FOLDER, splitPointSHA1.substring(0, 1),
                        splitPointSHA1), Commit.class);
        m5(splitPointSHA1, givenBranch);
        m4(splitPointSHA1, currBranch, givenHeadPath, givenBranch);

        HashMap<String, byte[]> givenBranchFiles = givenBranch.getFileToBlob();
        HashMap<String, byte[]> currBranchFiles = currBranch.getFileToBlob();
        HashMap<String, byte[]> splitPointFiles = splitPoint.getFileToBlob();
        boolean encounteredConflict = false;

        encounteredConflict = theOGLoop(givenBranchFiles, splitPointFiles,
                currBranchFiles, givenBranch, encounteredConflict);

        encounteredConflict = theLoop(currBranchFiles, splitPointFiles,
                givenBranchFiles, encounteredConflict);

        String logMsg = "Merged " + givenBranchName
                + " into " + currentBranchName + ".";
        mergeCommit(logMsg, givenBranch.getSHA1());
        if (encounteredConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** with GIVENBRANCHNAME. */
    private static void m1(String givenBranchName) {
        if (!Utils.plainFilenamesIn(
                Main.BRANCH_HEADS).contains(givenBranchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
    }

    /** with CURRENTBRANCHNAME, GIVENBRANCHNAME. */
    private static void m2(String givenBranchName, String currentBranchName) {
        if (givenBranchName.equals(currentBranchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }

    /** m3. */
    private static void m3() {
        if (Utils.plainFilenamesIn(Main.STAGING_AREA).size() != 0
                || Utils.plainFilenamesIn(
                Main.REMOVE_FROM_COMMIT).size() != 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
    }

    /** with SPLITPOINTSHA1, GIVENBRANCH, CURRBRANCH, GIVENHEADPATH. */
    private static void m4(String splitPointSHA1, Commit currBranch,
                           String givenHeadPath, Commit givenBranch) {
        if (splitPointSHA1.equals(currBranch.getSHA1())) {
            updateHeads(new File(givenHeadPath));
            System.out.println("Current branch fast-forwarded.");
            mergeReset(givenBranch);
            System.exit(0);
        }
    }

    /** with SPLITPOINTSHA1, GIVENBRANCH. */
    private static void m5(String splitPointSHA1, Commit givenBranch) {
        if (splitPointSHA1.equals(givenBranch.getSHA1())) {
            System.out.println("Given branch is "
                    + "an ancestor of the current branch.");
            System.exit(0);
        }
    }

    /** with GBFILENAME, CURRCONTENTS, GIVENCONTENTS. */
    private static void m6(String gBfilename,
                           String currContents, String givenContents) {
        Utils.writeContents(Utils.join(Main.CWD, gBfilename),
                "<<<<<<< HEAD" + "\n" + currContents
                        + "=======" + "\n" + givenContents
                        + ">>>>>>>" + "\n");
    }

    /** with GBFILENAME, GIVENCONTENTS. */
    private static void m7(String gBfilename, String givenContents) {
        Utils.writeContents(Utils.join(Main.CWD, gBfilename),
                "<<<<<<< HEAD" + "\n" + ""
                        + "=======" + "\n" + givenContents
                        + ">>>>>>>" + "\n");
    }

    /** Returns encounteredConflict with CURRBRANCHFILES,
     *  SPLITPOINTFILES, GIVENBRANCHFILES, GBFILENAME,
     *  INSPLITPOINT, INCURRBRANCH. */
    private static boolean checkInSPC(boolean inSplitPoint,
                                      boolean inCurrBranch,
                                      HashMap<String, byte[]> givenBranchFiles,
                                      String gBfilename,
                                      HashMap<String, byte[]> currBranchFiles,
                                      HashMap<String, byte[]> splitPointFiles) {
        if (inSplitPoint && inCurrBranch) {
            if (!Arrays.equals(givenBranchFiles.get(gBfilename),
                    currBranchFiles.get(gBfilename))) {
                if (Arrays.equals(splitPointFiles.get(gBfilename),
                        currBranchFiles.get(gBfilename))) {
                    Utils.writeContents(Utils.join(Main.CWD, gBfilename),
                            new String(givenBranchFiles.get(gBfilename)));
                    add("add", gBfilename);
                    return true;
                }
            }
            if (Arrays.equals(givenBranchFiles.get(gBfilename),
                    splitPointFiles.get(gBfilename))) {
                if (!Arrays.equals(splitPointFiles.get(gBfilename),
                        currBranchFiles.get(gBfilename))) {
                    return true;
                }
            }
            if (!Arrays.equals(splitPointFiles.get(gBfilename),
                    currBranchFiles.get(gBfilename))) {
                if (!Arrays.equals(splitPointFiles.get(gBfilename),
                        givenBranchFiles.get(gBfilename))) {
                    if (Arrays.equals(givenBranchFiles.get(gBfilename),
                            currBranchFiles.get(gBfilename))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Returns encounteredConflict with CURRBRANCHFILES,
     *  SPLITPOINTFILES, GIVENBRANCHFILES, GBFILENAME,
     *  INSPLITPOINT, INCURRBRANCH. */
    private static boolean checkNotSP(boolean inSplitPoint,
                                      HashMap<String, byte[]> splitPointFiles,
                                      String gBfilename,
                                      HashMap<String, byte[]> givenBranchFiles,
                                      boolean inCurrBranch,
                                      HashMap<String, byte[]> currBranchFiles) {
        if (!inSplitPoint && !Arrays.equals(
                splitPointFiles.get(gBfilename),
                givenBranchFiles.get(gBfilename))) {
            String givenContents = "";
            if (givenBranchFiles.get(gBfilename) != null) {
                givenContents =
                        new String(givenBranchFiles.get(gBfilename));
            }
            if (inCurrBranch) {
                String currContents = "";
                if (currBranchFiles.get(gBfilename) != null) {
                    currContents =
                            new String(currBranchFiles.get(gBfilename));
                }
                Utils.writeContents(Utils.join(Main.CWD, gBfilename),
                        "<<<<<<< HEAD" + "\n"
                                + currContents + "=======" + "\n"
                                + givenContents + ">>>>>>>" + "\n");
            } else {
                Utils.writeContents(Utils.join(Main.CWD, gBfilename),
                        "<<<<<<< HEAD" + "\n" + ""
                                + "=======" + "\n" + givenContents
                                + ">>>>>>>" + "\n");
            }
            add("add", gBfilename);
            return true;
        }
        return false;
    }

    /** Returns encounteredConflict with CURRBRANCHFILES,
     *  SPLITPOINTFILES, GIVENBRANCHFILES, GBFILENAME,
     *  INSPLITPOINT, INCURRBRANCH. */
    private static boolean checkSP(boolean inSplitPoint,
                                   HashMap<String, byte[]> splitPointFiles,
                                   String gBfilename,
                                   HashMap<String, byte[]> givenBranchFiles,
                                   boolean inCurrBranch,
                                   HashMap<String, byte[]> currBranchFiles) {
        if (inSplitPoint && !Arrays.equals(splitPointFiles.get(gBfilename),
                givenBranchFiles.get(gBfilename))) {
            String givenContents = "";
            if (givenBranchFiles.get(gBfilename) != null) {
                givenContents =
                        new String(givenBranchFiles.get(gBfilename));
            }
            if (inCurrBranch) {
                String currContents = "";
                if (currBranchFiles.get(gBfilename) != null) {
                    currContents =
                            new String(currBranchFiles.get(gBfilename));
                }
                m6(gBfilename, currContents, givenContents);
            } else {
                m7(gBfilename, givenContents);
            }
            add("add", gBfilename);
            return true;
        }
        return false;
    }

    /** Returns encounteredConflict with CURRBRANCHFILES,
     *  SPLITPOINTFILES, GIVENBRANCHFILES, CBFILENAME,
     *  INSPLITPOINT, INGIVENBRANCH. */
    private static boolean checkNotSP2(boolean inSplitPoint,
                                       HashMap<String, byte[]> splitPointFiles,
                                       String cBfilename,
                                       HashMap<String, byte[]> currBranchFiles,
                                       boolean inGivenBranch,
                                       HashMap<String, byte[]>
                                               givenBranchFiles) {
        if (!inSplitPoint && !Arrays.equals(splitPointFiles.get(cBfilename),
                currBranchFiles.get(cBfilename))) {
            String currContents = "";
            if (currBranchFiles.get(cBfilename) != null) {
                currContents = new String(currBranchFiles.get(cBfilename));
            }
            if (inGivenBranch) {
                String givenContents = "";
                if (givenBranchFiles.get(cBfilename) != null) {
                    givenContents =
                            new String(givenBranchFiles.get(cBfilename));
                }
                Utils.writeContents(Utils.join(Main.CWD, cBfilename),
                        "<<<<<<< HEAD" + "\n" + currContents
                                + "=======" + "\n"
                                + givenContents + ">>>>>>>" + "\n");
            } else {
                Utils.writeContents(Utils.join(Main.CWD, cBfilename),
                        "<<<<<<< HEAD" + "\n"
                                + currContents + "======="
                                + "\n" + "" + ">>>>>>>" + "\n");
            }
            add("add", cBfilename);
            return true;
        }
        return false;
    }

    /** Returns encounteredConflict with CURRBRANCHFILES,
     *  SPLITPOINTFILES, GIVENBRANCHFILES, CBFILENAME,
     *  INSPLITPOINT, INGIVENBRANCH. */
    private static boolean checkSP2(boolean inSplitPoint,
                                    HashMap<String, byte[]> splitPointFiles,
                                    String cBfilename,
                                    HashMap<String, byte[]> currBranchFiles,
                                    boolean inGivenBranch,
                                    HashMap<String, byte[]> givenBranchFiles) {
        if (inSplitPoint && !Arrays.equals(splitPointFiles.get(cBfilename),
                currBranchFiles.get(cBfilename))) {
            String currContents = "";
            if (currBranchFiles.get(cBfilename) != null) {
                currContents = new String(currBranchFiles.get(cBfilename));
            }
            if (inGivenBranch) {
                String givenContents = "";
                if (givenBranchFiles.get(cBfilename) != null) {
                    givenContents =
                            new String(givenBranchFiles.get(cBfilename));
                }
                Utils.writeContents(Utils.join(Main.CWD, cBfilename),
                        "<<<<<<< HEAD" + "\n" + currContents
                                + "=======" + "\n"
                                + givenContents + ">>>>>>>" + "\n");
            } else {
                Utils.writeContents(Utils.join(Main.CWD, cBfilename),
                        "<<<<<<< HEAD" + "\n" + currContents
                                + "=======" + "\n" + "" + ">>>>>>>" + "\n");
            }
            add("add", cBfilename);
            return true;
        }
        return false;
    }

    /** Returns encounteredConflict with CURRBRANCHFILES,
     *  SPLITPOINTFILES, GIVENBRANCHFILES, ENCOUNTEREDCONFLICT. */
    private static boolean theLoop(HashMap<String, byte[]> currBranchFiles,
                                   HashMap<String, byte[]> splitPointFiles,
                                   HashMap<String, byte[]> givenBranchFiles,
                                   boolean encounteredConflict) {
        for (String cBfilename : currBranchFiles.keySet()) {
            boolean inSplitPoint = splitPointFiles.containsKey(cBfilename);
            boolean inGivenBranch = givenBranchFiles.containsKey(cBfilename);
            if (inSplitPoint && !inGivenBranch) {
                if (Arrays.equals(splitPointFiles.get(cBfilename),
                        currBranchFiles.get(cBfilename))) {
                    rm("rm", cBfilename);
                }
            }
            if (!inSplitPoint && !inGivenBranch) {
                continue;
            }
            if (checkNotSP2(inSplitPoint, splitPointFiles, cBfilename,
                    currBranchFiles, inGivenBranch, givenBranchFiles)) {
                encounteredConflict = true;
                continue;
            }
            if (checkSP2(inSplitPoint, splitPointFiles, cBfilename,
                    currBranchFiles, inGivenBranch, givenBranchFiles)) {
                encounteredConflict = true;
                continue;
            }
        }
        return encounteredConflict;
    }

    /** Returns encounteredConflict with GIVENBRANCHFILES,
     *  SPLITPOINTFILES, CURRBRANCHFILES, GIVENBRANCH, ENCOUNTEREDCONFLICT. */
    private static boolean theOGLoop(HashMap<String, byte[]> givenBranchFiles,
                                     HashMap<String, byte[]> splitPointFiles,
                                     HashMap<String, byte[]> currBranchFiles,
                                     Commit givenBranch,
                                     boolean encounteredConflict) {
        for (String gBfilename : givenBranchFiles.keySet()) {
            boolean inSplitPoint = splitPointFiles.containsKey(gBfilename);
            boolean inCurrBranch = currBranchFiles.containsKey(gBfilename);
            if (checkInSPC(inSplitPoint, inCurrBranch, givenBranchFiles,
                    gBfilename, currBranchFiles, splitPointFiles)) {
                continue;
            }
            if (!inCurrBranch && Utils.join(Main.CWD, gBfilename).exists()) {
                if (!Arrays.equals(givenBranchFiles.get(gBfilename),
                        Utils.readContents(Utils.join(Main.CWD, gBfilename)))) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it or add it first.");
                    System.exit(0);
                }
            }
            if (!inSplitPoint && !inCurrBranch) {
                checkout("checkout", givenBranch.getSHA1(), "--", gBfilename);
                add("add", gBfilename);
                continue;
            }
            if (inSplitPoint && !inCurrBranch) {
                if (Arrays.equals(splitPointFiles.get(gBfilename),
                        givenBranchFiles.get(gBfilename))) {
                    continue;
                }
            }
            if (checkNotSP(inSplitPoint, splitPointFiles, gBfilename,
                    givenBranchFiles, inCurrBranch, currBranchFiles)) {
                encounteredConflict = true;
                continue;
            }
            if (checkSP(inSplitPoint, splitPointFiles, gBfilename,
                    givenBranchFiles, inCurrBranch, currBranchFiles)) {
                encounteredConflict = true;
                continue;
            }
        }
        return encounteredConflict;
    }

    /** Reset for Merge with GOTOCOMMIT. */
    private static void mergeReset(Commit goToCommit) {
        File commitSubDir = Utils.join(Commit.COMMITS_FOLDER,
                goToCommit.getSHA1().substring(0, 1), goToCommit.getSHA1());
        HashMap<String, byte[]> goToCommitFiles = goToCommit.getFileToBlob();

        for (String f : Utils.plainFilenamesIn(Main.CWD)) {
            if (!goToCommitFiles.containsKey(f)) {
                Utils.join(Main.CWD, f).delete();
            }
        }

        String currBranch = Utils.readContentsAsString(Main.CURRENT_BRANCH);
        String path = Utils.join(commitSubDir, goToCommit.getSHA1()).getPath();
        Utils.writeContents(Utils.join(Main.BRANCH_HEADS, currBranch), path);
        Utils.writeContents(Main.HEAD, path);
    }

    /** Commit for Merge with LOGMSG and SECONDPARENT. */
    private static void mergeCommit(String logMsg, String secondParent) {
        String logMessage = logMsg;
        List<String> addedFiles;
        List<String> removedFiles;
        if (Utils.plainFilenamesIn(Main.STAGING_AREA).isEmpty()
                && Utils.plainFilenamesIn(Main.REMOVE_FROM_COMMIT).isEmpty()) {
            addedFiles = null;
            removedFiles = null;
        } else {
            addedFiles = Utils.plainFilenamesIn(Main.STAGING_AREA);
            removedFiles = Utils.plainFilenamesIn(Main.REMOVE_FROM_COMMIT);

        }
        File currentHead = new File(Utils.readContentsAsString(Main.HEAD));
        Commit headCommit = Utils.readObject(currentHead, Commit.class);

        makeCommit(logMessage, headCommit,
                addedFiles, removedFiles, secondParent);
    }

    /** Helps to find Split Point of CURRBRANCH
     * and GIVENBRANCH return splitcommit. */
    private static String splitPointHelper(Commit currBranch,
                                           Commit givenBranch) {
        HashSet<String> givenBranchCommits = new HashSet<>();
        addGivenParent(givenBranchCommits, givenBranch.getSHA1());

        HashMap<String, String> splitPointMap =
                findSplitPoint(currBranch.getSHA1(), givenBranchCommits, 0);
        return splitPointMap.get("Commit");
    }

    /** Adds parents from PARENTS of GIVENBRANCH. */
    private static void addGivenParent(HashSet<String>
                                               parents, String givenBranch) {
        if (givenBranch == null) {
            return;
        } else {
            parents.add(givenBranch);
            Commit com = Utils.readObject(Utils.join(Commit.COMMITS_FOLDER,
                    givenBranch.substring(0, 1), givenBranch), Commit.class);
            addGivenParent(parents, com.getParent());
            addGivenParent(parents, com.getSecondParent());
        }
    }

    /** Finds splitpoint from the CURRCOMMIT,
     *  GIVENCOMMITS and LENGTH return hashMap. */
    private static HashMap<String, String> findSplitPoint(
            String currCommit, HashSet<String> givenCommits, int length) {
        if (currCommit == null) {
            return null;

        } else if (givenCommits.contains(currCommit)) {
            HashMap<String, String> temp = new HashMap<>();
            temp.put("Commit", currCommit);
            temp.put("Length", Integer.toString(length));
            return temp;

        } else {
            Commit com = Utils.readObject(Utils.join
                    (Commit.COMMITS_FOLDER, currCommit.substring(0, 1),
                            currCommit), Commit.class);
            HashMap<String, String> splitOne =
                    findSplitPoint(com.getParent(), givenCommits, length + 1);
            HashMap<String, String> splitTwo =
                    findSplitPoint(com.getSecondParent(),
                            givenCommits, length + 1);

            if (splitOne == null && splitTwo == null) {
                System.out.println("No splitpoint.");
                System.exit(0);
            } else if (splitOne == null) {
                return splitTwo;
            } else if (splitTwo == null) {
                return splitOne;
            } else {
                if (Integer.parseInt(splitOne.get("Length"))
                        <= Integer.parseInt(splitTwo.get("Length"))) {
                    return splitOne;
                } else {
                    return splitTwo;
                }
            }
        }
        return new HashMap<>();
    }

    /** Checking ARGS using CMD, N. */
    private static void checkArgs(String cmd, int n, String... args) {
        if (!cmd.equals("init")) {
            if (!Main.MAIN_FOLDER.exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
        }
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /** FORTY. */
    static final int FORTY = 40;
}
