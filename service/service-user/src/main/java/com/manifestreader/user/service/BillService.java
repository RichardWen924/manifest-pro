package com.manifestreader.user.service;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.user.model.dto.BillCreateRequest;
import com.manifestreader.user.model.dto.BillExtractSaveRequest;
import com.manifestreader.user.model.dto.BillPageQuery;
import com.manifestreader.user.model.dto.BillParseRequest;
import com.manifestreader.user.model.dto.BillUpdateRequest;
import com.manifestreader.user.model.dto.ExtractedBillSaveRequest;
import com.manifestreader.user.model.vo.BillExtractResultVO;
import com.manifestreader.user.model.vo.BillExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.BillExtractTaskVO;
import com.manifestreader.user.model.vo.BillDetailVO;
import com.manifestreader.user.model.vo.BillVO;
import org.springframework.web.multipart.MultipartFile;

public interface BillService {

    PageResult<BillVO> page(BillPageQuery query);

    BillDetailVO detail(Long id);

    BillVO create(BillCreateRequest request);

    BillVO saveExtractedFields(ExtractedBillSaveRequest request);

    BillExtractResultVO extractBill(MultipartFile file);

    BillExtractTaskSubmitVO submitExtractTask(MultipartFile file);

    BillExtractTaskVO getExtractTask(String taskNo);

    BillVO saveExtractedResult(BillExtractSaveRequest request);

    BillVO update(Long id, BillUpdateRequest request);

    void delete(Long id);

    BillDetailVO parse(BillParseRequest request);
}
