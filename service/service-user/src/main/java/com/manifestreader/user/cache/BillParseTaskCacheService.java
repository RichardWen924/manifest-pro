package com.manifestreader.user.cache;

import com.manifestreader.user.model.vo.BillExtractTaskVO;
import java.util.Optional;

public interface BillParseTaskCacheService {

    Optional<String> findTaskNoByFileHash(Long companyId, String fileHash);

    void bindFileHash(Long companyId, String fileHash, String taskNo);

    void cacheTask(BillExtractTaskVO task);

    Optional<BillExtractTaskVO> getTask(String taskNo);
}
