package io.kosong.crux.hbase;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;

public class HBaseIterator implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(HBaseIterator.class);

    private static final int MIN_CACHE_SIZE = 32;
    private static final int MAX_CACHE_SIZE = 16384;

    private final Table table;
    private final TableName tableName;
    private final byte[] family;
    private final byte[] qualifier;

    private Result result;
    private ResultScanner scanner;

    private ScanDirection scanDirection = ScanDirection.NONE;
    private int cacheSize = MIN_CACHE_SIZE;
    private long accessCounter = 0;
    private long lastAccess = 0;

    public HBaseIterator(Connection connection, TableName tableName, byte[] family, byte[] qualifier) throws IOException {
        this.tableName = tableName;
        this.family = family;
        this.qualifier = qualifier;
        this.table = connection.getTable(this.tableName);
    }

    public void seek(byte[] key) throws IOException {

        // reset any open scanner
        closeScanner();

        initScanner(ScanDirection.FORWARD, MIN_CACHE_SIZE, key, true);

        nextResult();
    }

    public void next() throws IOException {

        if (scanDirection != ScanDirection.FORWARD) {
            initScanner(ScanDirection.FORWARD, MIN_CACHE_SIZE, key(), false);
        }

        try {
            nextResult();
        } catch (IOException e) {
            log.warn("retrying next", e);
            initScanner(ScanDirection.FORWARD, cacheSize, key(), false);
            nextResult();
        }

        if (shouldIncreaseCacheSize()) {
            closeScanner();
            int newCacheSize = nextCacheSize(cacheSize);
            initScanner(ScanDirection.FORWARD, newCacheSize, key(), false);
        }
    }

    public void prev() throws IOException {

        if (scanDirection != ScanDirection.BACKWARD) {
            initScanner(ScanDirection.BACKWARD, MIN_CACHE_SIZE, key(), false);
        }

        try {
            nextResult();
        } catch (IOException e) {
            log.warn("retry prev", e);
            initScanner(ScanDirection.BACKWARD, cacheSize, key(), false);
            nextResult();
        }

        if (shouldIncreaseCacheSize()) {
            closeScanner();
            int newCacheSize = nextCacheSize(cacheSize);
            initScanner(ScanDirection.BACKWARD, newCacheSize, key(), false);
        }
    }

    public byte[] key() {
        if (isValid()) {
            return result.getRow();
        } else {
            return null;
        }
    }

    public byte[] value() {
        if (isValid()) {
            return result.getValue(family, qualifier);
        } else {
            return null;
        }
    }

    public boolean isValid() {
        return result != null && !result.isEmpty();
    }

    @Override
    public void close() throws IOException {
        table.close();
    }

    private void initScanner(ScanDirection direction, int cacheSize, byte[] startRow, boolean inclusive) throws IOException {

        Scan s = new Scan();
        s.addColumn(family, qualifier);
        s.setReadType(Scan.ReadType.STREAM);
        s.setCaching(cacheSize);
        s.readVersions(1);
        if (direction == ScanDirection.FORWARD) {
            s.setReversed(false);
        } else if (direction == ScanDirection.BACKWARD) {
            s.setReversed(true);
        }

        if (startRow != null) {
            s.withStartRow(startRow, inclusive);
        }

        try {
            scanner = table.getScanner(s);
            this.scanDirection = direction;
            this.cacheSize = cacheSize;
            this.accessCounter = 0;
        } catch (IOException e) {
            scanner = null;
            this.scanDirection = ScanDirection.NONE;
            this.cacheSize = MIN_CACHE_SIZE;
            this.accessCounter = 0;
            throw e;
        }
    }

    private void nextResult() throws IOException {
        try {
            result = scanner.next();
            lastAccess = System.currentTimeMillis();
            accessCounter++;
        } catch (InterruptedIOException e) {
            log.warn("Error fetching next result", e);
            closeScanner();
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private void closeScanner() {
        if (scanner != null) {
            scanner.close();
            scanner = null;
            scanDirection = ScanDirection.NONE;
        }
    }

    private boolean shouldIncreaseCacheSize() {
        return accessCounter < MAX_CACHE_SIZE && accessCounter >= cacheSize;
    }

    private int nextCacheSize(int currentCacheSize) {
        return Math.min(MAX_CACHE_SIZE, currentCacheSize * 2);
    }

    enum ScanDirection {
        NONE,
        FORWARD,
        BACKWARD
    }
}
