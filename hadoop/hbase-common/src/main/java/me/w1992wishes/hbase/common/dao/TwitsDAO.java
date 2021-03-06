package me.w1992wishes.hbase.common.dao;

import me.w1992wishes.hbase.common.util.Md5Utils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author wanqinfeng
 * @date 2019/7/7 14:49.
 */
public class TwitsDAO {

    public static final byte[] TABLE_NAME = Bytes.toBytes("twits");
    public static final byte[] TWITS_FAM = Bytes.toBytes("twits");

    public static final byte[] USER_COL = Bytes.toBytes("user");
    public static final byte[] TWIT_COL = Bytes.toBytes("twit");

    private static final int LONG_LENGTH = 8;

    private Connection conn;

    private static final Logger log = Logger.getLogger(TwitsDAO.class);

    public TwitsDAO(Connection conn) {
        this.conn = conn;
    }

    private static byte[] mkRowKey(Twit t) {
        return mkRowKey(t.user, t.dt);
    }

    private static byte[] mkRowKey(String user, DateTime dt) {
        byte[] userHash = Md5Utils.md5sum(user);
        byte[] timestamp = Bytes.toBytes(-1 * dt.getMillis());
        byte[] rowKey = new byte[Md5Utils.MD5_LENGTH + LONG_LENGTH];

        int offset = 0;
        offset = Bytes.putBytes(rowKey, offset, userHash, 0, userHash.length);
        Bytes.putBytes(rowKey, offset, timestamp, 0, timestamp.length);
        return rowKey;
    }

    private static Put mkPut(Twit t) {
        Put p = new Put(mkRowKey(t));
        p.addColumn(TWITS_FAM, USER_COL, Bytes.toBytes(t.user));
        p.addColumn(TWITS_FAM, TWIT_COL, Bytes.toBytes(t.text));
        return p;
    }

    private static Get mkGet(String user, DateTime dt) {
        Get g = new Get(mkRowKey(user, dt));
        g.addColumn(TWITS_FAM, USER_COL);
        g.addColumn(TWITS_FAM, TWIT_COL);
        return g;
    }

    private static String toStr(byte[] xs) {
        StringBuilder sb = new StringBuilder(xs.length * 2);
        for (byte b : xs) {
            sb.append(b).append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private static Scan mkScan(String user) {
        byte[] userHash = Md5Utils.md5sum(user);
        // 212d...866f00...
        byte[] startRow = Bytes.padTail(userHash, LONG_LENGTH);
        byte[] stopRow = Bytes.padTail(userHash, LONG_LENGTH);
        // 212d...867000...
        stopRow[Md5Utils.MD5_LENGTH - 1]++;

        log.debug("Scan starting at: '" + toStr(startRow) + "'");
        log.debug("Scan stopping at: '" + toStr(stopRow) + "'");

        Scan s = new Scan().withStartRow(startRow).withStopRow(stopRow);
        s.addColumn(TWITS_FAM, USER_COL);
        s.addColumn(TWITS_FAM, TWIT_COL);
        return s;
    }

    public void postTwit(String user, DateTime dt, String text) throws IOException {
        Table twits = conn.getTable(TableName.valueOf(TABLE_NAME));

        Put p = mkPut(new Twit(user, dt, text));
        twits.put(p);

        twits.close();
    }

    public me.w1992wishes.hbase.common.model.Twit getTwit(String user, DateTime dt) throws IOException {

        Table twits = conn.getTable(TableName.valueOf(TABLE_NAME));

        Get g = mkGet(user, dt);
        Result result = twits.get(g);
        if (result.isEmpty()) {
            return null;
        }

        Twit t = new Twit(result);
        twits.close();
        return t;
    }

    public List<me.w1992wishes.hbase.common.model.Twit> list(String user) throws IOException {

        Table twits = conn.getTable(TableName.valueOf(TABLE_NAME));

        ResultScanner results = twits.getScanner(mkScan(user));
        List<me.w1992wishes.hbase.common.model.Twit> ret = new ArrayList<>();
        for (Result r : results) {
            ret.add(new Twit(r));
        }

        twits.close();
        return ret;
    }

    private static class Twit extends me.w1992wishes.hbase.common.model.Twit {

        private Twit(Result r) {
            this(
                    r.getValue(TWITS_FAM, USER_COL),
                    Arrays.copyOfRange(r.getRow(), Md5Utils.MD5_LENGTH, Md5Utils.MD5_LENGTH + LONG_LENGTH),
                    r.getValue(TWITS_FAM, TWIT_COL));
        }

        private Twit(byte[] user, byte[] dt, byte[] text) {
            this(
                    Bytes.toString(user),
                    new DateTime(-1 * Bytes.toLong(dt)),
                    Bytes.toString(text));
        }

        private Twit(String user, DateTime dt, String text) {
            this.user = user;
            this.dt = dt;
            this.text = text;
        }
    }

}
