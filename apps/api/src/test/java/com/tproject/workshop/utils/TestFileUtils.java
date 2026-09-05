package com.tproject.workshop.utils;

import dev.mccue.guava.base.CaseFormat;

public final class TestFileUtils {
    private TestFileUtils() {
    }


    private static StackWalker.StackFrame getClassNameAndMethodName(String packageName) {
        StackWalker walker = StackWalker.getInstance();
        return walker.walk(stream -> stream.filter(f -> f.getClassName().contains(packageName))
                        .filter(f -> !f.getClassName().contains("AbstractIntegrationLiveTest"))
                        .filter(f -> !f.getClassName().contains("TestFileUtils"))
                        .findFirst())
                .orElseThrow(() -> new RuntimeException("Could not find test spec"));
    }

    public static String getClassName(String packageName) {
        var stackFrame = getClassNameAndMethodName(packageName);
        final String fullClassName = stackFrame.getClassName();
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN,
                fullClassName.substring(fullClassName.lastIndexOf(".") + 1));
    }

    public static String getMethodName(String packageName) {
        var stackFrame = getClassNameAndMethodName(packageName);
        return stackFrame.getMethodName();
    }

}
