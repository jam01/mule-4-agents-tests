package com.jam01.mule.tests;

import net.bytebuddy.dynamic.ClassFileLocator;

import java.io.IOException;
import java.util.Arrays;

public class RootPackageCustomLocator implements ClassFileLocator {

    private final String[] packages;
    private final ClassFileLocator classFileLocator;

    public RootPackageCustomLocator(ClassFileLocator classFileLocator, String... packages) {
        this.packages = packages;
        this.classFileLocator = classFileLocator;
    }

    /**
     * {@inheritDoc}
     */
    public Resolution locate(String name) throws IOException {
        return Arrays.stream(packages).anyMatch(name::startsWith) ? classFileLocator.locate(name) : new Resolution.Illegal(name);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        classFileLocator.close();
    }
}
