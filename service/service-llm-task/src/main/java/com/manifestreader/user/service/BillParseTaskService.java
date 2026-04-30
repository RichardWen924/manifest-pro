package com.manifestreader.user.service;

import com.manifestreader.user.messaging.BillParseTaskMessage;
import com.manifestreader.user.model.vo.BillExtractResultVO;
import com.manifestreader.user.model.vo.BillExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.BillExtractTaskVO;
import org.springframework.web.multipart.MultipartFile;

public interface BillParseTaskService {

    BillExtractTaskSubmitVO submitExtractTask(MultipartFile file);

    BillExtractTaskVO getTask(String taskNo);

    BillExtractResultVO resolveResult(String taskNo);

    void processTask(BillParseTaskMessage message);
}
