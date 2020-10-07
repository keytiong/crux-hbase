package io.kosong.crux.hbase;

import org.apache.hadoop.hbase.client.*;

import java.io.Closeable;
import java.io.IOException;

public class HBaseIterator implements Closeable {

    private final Table table;

    private ResultScanner forwardScanner;
    private ResultScanner backwardScanner;

    private Result cursor;

    private final byte[] family;
    private final byte[] qualifier;

    public HBaseIterator(Table table, byte[] family, byte[] qualifier) {
        this.table = table;
        this.family = family;
        this.qualifier = qualifier;
    }

    public void seek(byte[] key) throws IOException {
        invalidateBackwardScanner();
        invalidateForwardScanner();
        resetCursor(key);
    }

    public void next() throws IOException {
        invalidateBackwardScanner();
        scannerNext(forwardScanner());
    }

    public void prev() throws IOException {
        invalidateForwardScanner();
        scannerNext(backwardScanner());
    }

    public byte[] key() {
        if (isValid()) {
            return cursor.getRow();
        } else {
            return null;
        }
    }

    public byte[] value() {
        if (isValid()) {
            return cursor.getValue(family, qualifier);
        } else {
            return null;
        }
    }

    public boolean isValid() {
        return cursor != null && ! cursor.isEmpty();
    }

    @Override
    public void close() throws IOException {
        invalidateBackwardScanner();;
        invalidateForwardScanner();
    }

    private ResultScanner backwardScanner() throws IOException {
        if (backwardScanner == null) {
            Scan scan = getScan(true);
            backwardScanner = table.getScanner(scan);
        }
        return backwardScanner;
    }

    private ResultScanner forwardScanner() throws IOException {
        if (forwardScanner == null) {
            Scan scan = getScan(false);
            forwardScanner = table.getScanner(scan);
        }
        return forwardScanner;
    }

    private Scan getScan(boolean isReversed) {
        Scan scan = new Scan();
        scan.setReversed(isReversed);
        byte[] startRow = key();
        if (startRow != null) {
            scan.withStartRow(startRow, false);
        }
        scan.addColumn(family, qualifier);
        return scan;
    }

    private void invalidateForwardScanner() {
        if (forwardScanner != null) {
            forwardScanner.close();
            forwardScanner = null;
        }
    }

    private void invalidateBackwardScanner() {
        if (backwardScanner != null) {
            backwardScanner.close();
            backwardScanner = null;
        }
    }

    private void scannerNext(ResultScanner scanner) throws IOException {
        cursor = scanner.next();
    }

    private void resetCursor(byte [] key) throws IOException {
        Scan scan = new Scan();
        scan.withStartRow(key);
        try (ResultScanner scanner = table.getScanner(scan)) {
            cursor = scanner.next();
        }
    }
}
