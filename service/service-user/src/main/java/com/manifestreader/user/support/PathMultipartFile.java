package com.manifestreader.user.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class PathMultipartFile implements MultipartFile {

    private final Path path;
    private final String fileName;
    private final String contentType;

    public PathMultipartFile(Path path, String fileName, String contentType) {
        this.path = path;
        this.fileName = StringUtils.hasText(fileName) ? fileName : path.getFileName().toString();
        this.contentType = StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public String getOriginalFilename() {
        return fileName;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        try {
            return Files.size(path) == 0;
        } catch (IOException ex) {
            return true;
        }
    }

    @Override
    public long getSize() {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return 0L;
        }
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(path);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        Files.copy(path, dest.toPath());
    }
}
