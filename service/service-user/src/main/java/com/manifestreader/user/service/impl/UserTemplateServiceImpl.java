package com.manifestreader.user.service.impl;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.user.dify.DifyTemplateMappingParser;
import com.manifestreader.user.dify.DifyWorkflowClient;
import com.manifestreader.user.model.dto.TemplatePageQuery;
import com.manifestreader.user.model.vo.TemplateExtractResultVO;
import com.manifestreader.user.model.vo.TemplateOptionVO;
import com.manifestreader.user.service.UserTemplateService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserTemplateServiceImpl implements UserTemplateService {

    private final DifyWorkflowClient difyWorkflowClient;
    private final DifyTemplateMappingParser mappingParser;

    public UserTemplateServiceImpl(DifyWorkflowClient difyWorkflowClient, DifyTemplateMappingParser mappingParser) {
        this.difyWorkflowClient = difyWorkflowClient;
        this.mappingParser = mappingParser;
    }

    @Override
    public PageResult<TemplateOptionVO> page(TemplatePageQuery query) {
        PageResult<TemplateOptionVO> pageResult = PageResult.empty(query.pageNo(), query.pageSize());
        List<TemplateOptionVO> records = usableTemplates();
        pageResult.setRecords(records);
        pageResult.setTotal(records.size());
        return pageResult;
    }

    @Override
    public TemplateOptionVO detail(Long id) {
        return new TemplateOptionVO(id, null, null, null);
    }

    @Override
    public List<TemplateOptionVO> usableTemplates() {
        return List.of(
                new TemplateOptionVO(1L, "BILL_STD", "标准海运提单模板", 1),
                new TemplateOptionVO(2L, "BILL_NA", "北美线提单模板", 1),
                new TemplateOptionVO(3L, "BOOKING_EU", "欧线订舱导出模板", 1)
        );
    }

    @Override
    public TemplateExtractResultVO extractTemplate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传模板样本文件");
        }
        String difyResponse = difyWorkflowClient.runTemplateExtraction(file);
        DifyTemplateMappingParser.ParsedMappings parsed = mappingParser.parse(difyResponse);
        String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "template-file";
        return new TemplateExtractResultVO(fileName, parsed.mappings().size(), parsed.mappings(), parsed.rawText());
    }
}
