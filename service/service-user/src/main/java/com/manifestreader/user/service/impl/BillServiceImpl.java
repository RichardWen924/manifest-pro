package com.manifestreader.user.service.impl;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.user.model.dto.BillCreateRequest;
import com.manifestreader.user.model.dto.BillPageQuery;
import com.manifestreader.user.model.dto.BillParseRequest;
import com.manifestreader.user.model.dto.BillUpdateRequest;
import com.manifestreader.user.model.vo.BillDetailVO;
import com.manifestreader.user.model.vo.BillVO;
import com.manifestreader.user.service.BillService;
import java.util.Collections;
import org.springframework.stereotype.Service;

@Service
public class BillServiceImpl implements BillService {

    @Override
    public PageResult<BillVO> page(BillPageQuery query) {
        return PageResult.empty(query.pageNo(), query.pageSize());
    }

    @Override
    public BillDetailVO detail(Long id) {
        return new BillDetailVO(id, null, null, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public BillVO create(BillCreateRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public BillVO update(Long id, BillUpdateRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public void delete(Long id) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public BillDetailVO parse(BillParseRequest request) {
        // TODO 后续接入提单解析流程，本阶段不实现复杂解析逻辑。
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }
}
