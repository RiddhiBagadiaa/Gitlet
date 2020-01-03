package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Riddhi Bagadiaa
 */
public class Main {

    /** Current Working Directory. */
    static final File CWD = new File(".");
    /** Main metadata folder. */
    static final File MAIN_FOLDER = Utils.join(CWD, ".gitlet");
    /** Folder of Branch Heads. */
    static final File BRANCH_HEADS = Utils.join(MAIN_FOLDER, "BRANCHES");
    /** File of Main Head. */
    static final File HEAD = Utils.join(MAIN_FOLDER, "HEAD");
    /** Folder of Staging Area. */
    static final File STAGING_AREA =
            Utils.join(MAIN_FOLDER, "STAGING AREA");
    /** Folder of Unstaging Area. */
    static final File REMOVE_FROM_COMMIT =
            Utils.join(MAIN_FOLDER, "UNSTAGING AREA");
    /** File of current branch name. */
    static final File CURRENT_BRANCH =
            Utils.join(MAIN_FOLDER, "CURRENT BRANCH");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        switch (args[0]) {
        case "init":
            Repository.init(args);
            break;
        case "add":
            Repository.add(args);
            break;
        case "commit":
            Repository.commit(args);
            break;
        case "log":
            Repository.log(args);
            break;
        case "checkout":
            Repository.checkout(args);
            break;
        case "branch":
            Repository.branch(args);
            break;
        case "rm-branch":
            Repository.rmBranch(args);
            break;
        case "global-log":
            Repository.globalLog(args);
            break;
        case "find":
            Repository.find(args);
            break;
        case "rm":
            Repository.rm(args);
            break;
        case "reset":
            Repository.reset(args);
            break;
        case "status":
            Repository.status(args);
            break;
        case "merge":
            Repository.merge(args);
            break;
        default:
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
    }
}
