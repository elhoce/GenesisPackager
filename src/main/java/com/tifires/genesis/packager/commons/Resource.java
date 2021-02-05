package com.tifires.genesis.packager.commons;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Properties;

public class Resource {
    private String filename;
    private byte[] content;

    public Resource(String filename) {
        this.filename = filename;
    }

    public Resource(String filename, Serializable serializable) throws IOException {
        this.filename = Objects.requireNonNull(filename);
        content = new ObjectMapper().writeValueAsBytes(Objects.requireNonNull(serializable));
    }

    public Resource(String filename, byte[] content) {

        this.filename = Objects.requireNonNull(filename);
        this.content = Objects.requireNonNull(content);
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = Objects.requireNonNull(filename);
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = Objects.requireNonNull(content);
    }

    public void setContent(Serializable serializable) throws IOException {
        content = new ObjectMapper().writeValueAsBytes(Objects.requireNonNull(serializable));
    }

    public void setPropertiesContent(Properties properties) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        properties.store(bos, filename);
        this.content = bos.toByteArray();
    }
}
