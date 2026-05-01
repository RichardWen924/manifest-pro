package com.manifestreader.user.controller.template;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.manifestreader.user.model.vo.BlankTemplateFile;
import com.manifestreader.user.model.vo.ExportedTemplateFile;
import com.manifestreader.user.service.TemplateExtractTaskService;
import com.manifestreader.user.service.TemplateExportTaskService;
import com.manifestreader.user.service.TemplateSaveTaskService;
import com.manifestreader.user.service.UserTemplateService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserTemplateControllerTest {

    @TempDir
    Path tempDir;

    @Mock
    private UserTemplateService templateService;

    @Mock
    private TemplateExtractTaskService templateExtractTaskService;

    @Mock
    private TemplateExportTaskService templateExportTaskService;

    @Mock
    private TemplateSaveTaskService templateSaveTaskService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new UserTemplateController(templateService, templateExtractTaskService, templateExportTaskService, templateSaveTaskService)
        ).build();
    }

    @Test
    void previewUsesEncodedContentDispositionForUnicodeFileName() throws Exception {
        Path previewFile = Files.writeString(tempDir.resolve("preview.pdf"), "preview");
        when(templateService.getBlankTemplatePreview("extract-1"))
                .thenReturn(new BlankTemplateFile(
                        "ignored.docx",
                        previewFile,
                        "OBD BL-JR电放_ZIMUSHH32021612-blank-template-preview.pdf",
                        "application/pdf",
                        previewFile
                ));

        mockMvc.perform(get("/user/templates/extract/extract-1/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''OBD%20BL-JR%E7%94%B5%E6%94%BE_ZIMUSHH32021612-blank-template-preview.pdf"
                ));
    }

    @Test
    void blankTemplateDownloadUsesEncodedContentDispositionForUnicodeFileName() throws Exception {
        Path docxFile = Files.writeString(tempDir.resolve("blank.docx"), "blank");
        when(templateService.getBlankTemplate("extract-2"))
                .thenReturn(new BlankTemplateFile("模板示例.docx", docxFile));

        mockMvc.perform(get("/user/templates/extract/extract-2/blank-template"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''%E6%A8%A1%E6%9D%BF%E7%A4%BA%E4%BE%8B.docx"
                ));
    }

    @Test
    void exportedTemplateDownloadUsesEncodedContentDispositionForUnicodeFileName() throws Exception {
        Path pdfFile = Files.writeString(tempDir.resolve("export.pdf"), "export");
        when(templateService.getExportedTemplate("export-1"))
                .thenReturn(new ExportedTemplateFile("电放提单.pdf", "application/pdf", pdfFile));

        mockMvc.perform(get("/user/templates/export/export-1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''%E7%94%B5%E6%94%BE%E6%8F%90%E5%8D%95.pdf"
                ));
    }
}
