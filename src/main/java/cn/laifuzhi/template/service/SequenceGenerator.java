package cn.laifuzhi.template.service;

import cn.laifuzhi.template.dao.SequenceDao;
import cn.laifuzhi.template.model.PO.SequencePO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Optional;

@Slf4j
@Service
public class SequenceGenerator {
    private static final int DEFAULT_STEP = 100;
    private static final String DEFAULT_SEQUENCE_KEY = "default";

    // never negative
    private long curSequence;
    private int counter;
    @Resource
    private SequenceDao sequenceDao;
    @Resource(name = "transactionTemplate4New1")
    private TransactionTemplate transactionTemplate4New;

    @PostConstruct
    private void init() {
        refresh();
    }

    public synchronized Optional<Long> nextSequence() {
        try {
            if (counter == 0) {
                refresh();
            }
            long nextSequence = curSequence + 1;
            if (nextSequence < 0) {
                log.error("SequenceGenerator error, curSequence negative");
                return Optional.empty();
            }
            counter = counter - 1;
            curSequence = nextSequence;
            return Optional.of(nextSequence);
        } catch (Exception e) {
            log.error("SequenceGenerator error, curSequence:{} counter:{}", curSequence, counter, e);
            return Optional.empty();
        }
    }

    private synchronized void refresh() {
        Long dbSequence = transactionTemplate4New.execute(status -> {
            // 排它锁
            Optional<SequencePO> optional = sequenceDao.selectForUpdate(DEFAULT_SEQUENCE_KEY);
            if (!optional.isPresent()) {
                throw new RuntimeException("sequence not exist");
            }
            SequencePO sequencePO = optional.get();
            // sequence字段unsigned，newSequence为负数会抛异常
            if (sequenceDao.updateSequence(DEFAULT_SEQUENCE_KEY, sequencePO.getSequence() + DEFAULT_STEP) <= 0) {
                throw new RuntimeException("sequence update failed");
            }
            return sequencePO.getSequence();
        });
        curSequence = dbSequence;
        counter = counter + DEFAULT_STEP;
    }
}
