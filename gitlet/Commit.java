package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/** @author Riddhi Bagadiaa
 * Commit class. */

@SuppressWarnings("unchecked")
public class Commit implements Serializable {

    /** Directory of all the commits. */
    static final File COMMITS_FOLDER = Utils.join(Main.MAIN_FOLDER, "COMMITS");

    /** Initialising a commit with its LOG, PARENT,
     *  BRANCH, FILETOBLOB, SECONDPARENT. */
    public Commit(String log, String parent, String branch,
                  HashMap<String, byte[]> fileToBlob, String secondParent) {
        _logMessage = log;
        _parent = parent;
        _secondParent = secondParent;
        _timeStamp = dateAndTime();
        _fileToBlob = fileToBlob;
        _branch = branch;
        _sha1 = makeSHA1();
    }

    /** Returns the SHA1 ID of the commit object. */
    private String makeSHA1() {
        byte[] fileSet = Utils.serialize(_fileToBlob.keySet().toString());
        if (_parent == null) {
            return Utils.sha1(_timeStamp, _logMessage, fileSet);
        }
        return Utils.sha1(_timeStamp, _parent, _logMessage, fileSet);
    }

    /** Returns the date and time String of the commit object. */
    private String dateAndTime() {
        String pattern = "E MMM dd HH:mm:ss yyyy Z";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

        if (_logMessage.equals("initial commit") && _parent == null) {
            Date commitdate = new Date();
            commitdate.setTime(0);
            return dateFormat.format(commitdate);
        }
        return dateFormat.format(new Date());
    }

    /** Returns the PARENT of the commit object. */
    public String getParent() {
        return _parent;
    }

    /** Returns the BRANCH of the commit object. */
    public String getBranch() {
        return _branch;
    }

    /** Returns the FILE TO BLOB LIST of the commit object. */
    public HashMap<String, byte[]> getFileToBlob() {
        return _fileToBlob;
    }

    /** Returns the SHA1 of the commit object. */
    public String getSHA1() {
        return _sha1;
    }

    /** Returns the DATE AND TIME STRING of the commit object. */
    public String getTimeStamp() {
        return _timeStamp;
    }

    /** Returns the LOG MESSAGE of the commit object. */
    public String getMessage() {
        return _logMessage;
    }

    /** Returns the SECOND PARENT of the commit object. */
    public String getSecondParent() {
        return _secondParent;
    }


    /** Log message of the commit. */
    private String _logMessage;

    /** Parent of the commit. */
    private String _parent;

    /** Second parent message of the commit. */
    private String _secondParent;

    /** Keeps track of all the files in the commit. */
    private HashMap<String, byte[]> _fileToBlob;

    /**Time stamp of the commit. */
    private String _timeStamp;

    /** Branch of the commit. */
    private String _branch;

    /** SHA1 of the commit. */
    private String _sha1;
}
