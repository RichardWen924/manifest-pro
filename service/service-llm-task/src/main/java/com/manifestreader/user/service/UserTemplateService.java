package com.manifestreader.user.service;

import com.manifestreader.common.result.PageResult;
import com.manifestreader.user.model.dto.TemplateExtractSaveRequest;
import com.manifestreader.user.model.dto.TemplatePageQuery;
import com.manifestreader.user.model.dto.TemplateStatusUpdateRequest;
import com.manifestreader.user.model.vo.BlankTemplateFile;
import com.manifestreader.user.model.vo.ExportedTemplateFile;
import com.manifestreader.user.model.vo.TemplateExtractResultVO;
import com.manifestreader.user.model.vo.TemplateExtractSaveResultVO;
import com.manifestreader.user.model.vo.TemplateExportResultVO;
import com.manifestreader.user.model.vo.TemplateManageVO;
import com.manifestreader.user.model.vo.TemplateOptionVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserTemplateService {

    PageResult<TemplateOptionVO> page(TemplatePageQuery query);

    TemplateOptionVO detail(Long id);

    List<TemplateOptionVO> usableTemplates();

    PageResult<TemplateManageVO> managePage(TemplatePageQuery query);

    TemplateManageVO manageDetail(Long id);

    void updateStatus(Long id, TemplateStatusUpdateRequest request);

    void deleteTemplate(Long id);

    TemplateExtractResultVO extractTemplate(MultipartFile file);

    BlankTemplateFile getBlankTemplate(String extractId);

    BlankTemplateFile getBlankTemplatePreview(String extractId);

    TemplateExtractSaveResultVO saveGeneratedTemplate(TemplateExtractSaveRequest request);

    TemplateExportResultVO exportWithTemplate(Long templateId, String outputFormat, MultipartFile file);

    ExportedTemplateFile getExportedTemplate(String exportId);
}
