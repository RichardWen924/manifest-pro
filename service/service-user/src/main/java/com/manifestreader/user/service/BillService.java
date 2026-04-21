package com.manifestreader.user.service;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.user.model.dto.BillCreateRequest;
import com.manifestreader.user.model.dto.BillPageQuery;
import com.manifestreader.user.model.dto.BillParseRequest;
import com.manifestreader.user.model.dto.BillUpdateRequest;
import com.manifestreader.user.model.vo.BillDetailVO;
import com.manifestreader.user.model.vo.BillVO;

public interface BillService {

    PageResult<BillVO> page(BillPageQuery query);

    BillDetailVO detail(Long id);

    BillVO create(BillCreateRequest request);

    BillVO update(Long id, BillUpdateRequest request);

    void delete(Long id);

    BillDetailVO parse(BillParseRequest request);
}
