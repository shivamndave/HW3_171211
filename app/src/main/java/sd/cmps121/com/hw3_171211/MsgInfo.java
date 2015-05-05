package sd.cmps121.com.hw3_171211;

// Custom MsgInfo data type
// that holds all the info
// from a server call to
// parse it into JSON
public class MsgInfo {
    public MsgInfo() {}

    public String msg;
    public String userid;
    public String dest;
    public String ts;
    public String msgid;
    public Boolean conversation;
}