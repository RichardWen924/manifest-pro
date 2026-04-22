package com.manifestreader.user.model.vo;

import java.nio.file.Path;

public record BlankTemplateFile(
        String fileName,
        Path path
) {
}
