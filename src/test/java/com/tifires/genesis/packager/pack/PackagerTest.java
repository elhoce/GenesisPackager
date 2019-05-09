package com.tifires.genesis.packager.pack;

import com.tifires.genesis.packager.commons.Resource;
import com.tifires.mocks.TestResource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PackagerTest {
    private final static Logger LOG = LoggerFactory.getLogger(PackagerTest.class);

    @Test
    public void pack_classOnly() {
        String src = genMockJavaSrc();
        Packager packager = Packager.newInstance().addSource("com.tifires.HelloClass", src).pack(false,false);
        LOG.info(packager.getLocation().toString());
    }

    @Test
    public void pack_classAndResources() throws IOException {
        String src = genMockJavaSrc();
        Resource resource1 = new Resource("res1.json", new TestResource("res1", 1));
        Resource resource2 = new Resource("res2.json", new TestResource("res2", 2));

        Packager packager = Packager.newInstance().addSource("com.tifires.HelloClass", src)
                .addResources("desc", resource1, resource2).pack(false,false);
        LOG.info(packager.getLocation().toString());
    }


    private String genMockJavaSrc() {
        StringBuilder sourceCode = new StringBuilder();
        sourceCode.append("package com.tifires;\n");
        sourceCode.append("public class HelloClass {\n");
        sourceCode.append("   public String hello() { return \"hello\"; }");
        sourceCode.append("}");
        return sourceCode.toString();
    }
}
