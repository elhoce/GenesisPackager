package com.tifires.genesis.packager.commons;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * Created by trung on 5/3/15.
 */
public class SourceCode extends SimpleJavaFileObject {
    private String content;
    private String className;

    public SourceCode(String className, String content) throws Exception {
        super(URI.create("string:///" + className.replace('.', '/')
                + Kind.SOURCE.extension), Kind.SOURCE);
        this.content = content;
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }

    public String getContent() {
        return content;
    }

    public byte[] getContentAsBytes() {
        return content.getBytes();
    }
}
