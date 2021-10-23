package cn.laifuzhi.template.service;

import cn.laifuzhi.template.conf.StaticConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteOptions;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class KvStore {
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private volatile boolean stopped;

    private Options options;
    private RocksDB rocksDB;

    @Resource
    private StaticConfig staticConfig;

    @PostConstruct
    private void init() throws RocksDBException, IOException {
        options = new Options().setCreateIfMissing(true);
        rocksDB = RocksDB.open(options, SystemUtils.getUserHome().getCanonicalPath() + File.separator + "/myRock");
    }

    @PreDestroy
    private void destroy() {
        stopped = true;
        rocksDB.close();
        options.close();
    }

    @SneakyThrows
    public void delete(String key) {
        if (stopped) {
            throw new RuntimeException("closed");
        }
        rocksDB.delete(key.getBytes(DEFAULT_CHARSET));
    }

    @SneakyThrows
    public void put(String key, String value) {
        if (stopped) {
            throw new RuntimeException("closed");
        }
        rocksDB.put(key.getBytes(DEFAULT_CHARSET), value.getBytes(DEFAULT_CHARSET));
    }

    @SneakyThrows
    public void syncPut(String key, String value) {
        if (stopped) {
            throw new RuntimeException("closed");
        }
        rocksDB.put(new WriteOptions().setSync(true), key.getBytes(DEFAULT_CHARSET), value.getBytes(DEFAULT_CHARSET));
    }

    @SneakyThrows
    public String get(String key) {
        if (stopped) {
            throw new RuntimeException("closed");
        }
        return new String(rocksDB.get(key.getBytes(DEFAULT_CHARSET)), DEFAULT_CHARSET);
    }

    @SneakyThrows
    public void iterate(BiConsumer<String, String> consumer) {
        if (stopped) {
            throw new RuntimeException("closed");
        }
        try (RocksIterator it = rocksDB.newIterator()) {
            for (it.seekToFirst(); it.isValid(); it.next()) {
                String key = new String(it.key(), DEFAULT_CHARSET);
                String value = new String(it.value(), DEFAULT_CHARSET);
                consumer.accept(key, value);
            }
            it.status();
        }
    }

    @SneakyThrows
    public void flushWal(boolean sync) {
        if (stopped) {
            throw new RuntimeException("closed");
        }
        rocksDB.flushWal(sync);
    }
}
