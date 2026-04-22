package com.manifestreader.user.model.vo;

import java.nio.file.Path;

public record ExportedTemplateFile(
        String fileName,
        String contentType,
        Path path
) {
}
