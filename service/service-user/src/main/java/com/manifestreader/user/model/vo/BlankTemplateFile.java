package com.manifestreader.user.model.vo;

import java.nio.file.Path;

public record BlankTemplateFile(
        String fileName,
        Path path,
        String previewFileName,
        String previewContentType,
        Path previewPath
) {
    public BlankTemplateFile(String fileName, Path path) {
        this(fileName, path, null, null, null);
    }
}
