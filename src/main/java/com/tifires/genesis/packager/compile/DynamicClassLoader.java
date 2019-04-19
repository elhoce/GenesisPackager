package com.tifires.genesis.packager.compile;

import com.tifires.genesis.packager.commons.CompiledCode;

import java.util.HashMap;
import java.util.Map;

public class DynamicClassLoader extends ClassLoader {

	private Map<String, CompiledCode> compiledCodes = new HashMap<>();

	public DynamicClassLoader(ClassLoader parent) {
		super(parent);
	}

	public void addCode(CompiledCode cc) {
		compiledCodes.put(cc.getName(), cc);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		CompiledCode cc = compiledCodes.get(name);
		if (cc == null) {
			return super.findClass(name);
		}
		byte[] byteCode = cc.getByteCode();
		return defineClass(name, byteCode, 0, byteCode.length);
	}

	public Map<String, CompiledCode> getCompiledCodes() {
		return compiledCodes;
	}
}
