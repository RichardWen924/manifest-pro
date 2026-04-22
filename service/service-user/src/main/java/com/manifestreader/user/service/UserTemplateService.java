package com.manifestreader.user.service;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.user.model.dto.TemplatePageQuery;
import com.manifestreader.user.model.vo.TemplateExtractResultVO;
import com.manifestreader.user.model.vo.TemplateOptionVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserTemplateService {

    PageResult<TemplateOptionVO> page(TemplatePageQuery query);

    TemplateOptionVO detail(Long id);

    List<TemplateOptionVO> usableTemplates();

    TemplateExtractResultVO extractTemplate(MultipartFile file);
}
