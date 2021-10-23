package cn.laifuzhi.template.dao;

import cn.laifuzhi.template.model.PO.SequencePO;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

public interface SequenceDao {
    Optional<SequencePO> selectForUpdate(@Param("sequenceKey") String sequenceKey);

    int updateSequence(@Param("sequenceKey") String sequenceKey, @Param("sequence") long newSequence);
}
