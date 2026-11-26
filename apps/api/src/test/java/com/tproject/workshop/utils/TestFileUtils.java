package com.tproject.workshop.utils;

public final class TestFileUtils {
    private TestFileUtils() {}

    private static StackWalker.StackFrame getClassNameAndMethodName(String packageName) {
        StackWalker walker = StackWalker.getInstance();
        return walker.walk(stream ->
                        stream.filter(f -> f.getClassName().contains(packageName))
                                .filter(f -> !f.getClassName().contains("AbstractIntegrationTest"))
                                .filter(f -> !f.getClassName().contains("TestFileUtils"))
                                .findFirst())
                .orElseThrow(() -> new RuntimeException("Could not find test spec"));
    }

    public static String getClassName(String packageName) {
        var stackFrame = getClassNameAndMethodName(packageName);
        final String fullClassName = stackFrame.getClassName();
        return toLowerHyphen(fullClassName.substring(fullClassName.lastIndexOf(".") + 1));
    }

    public static String getMethodName(String packageName) {
        var stackFrame = getClassNameAndMethodName(packageName);
        return stackFrame.getMethodName();
    }

    static String toLowerHyphen(String upperCamel) {
        StringBuilder result = new StringBuilder(upperCamel.length() + 8);
        for (int i = 0; i < upperCamel.length(); i++) {
            char ch = upperCamel.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
