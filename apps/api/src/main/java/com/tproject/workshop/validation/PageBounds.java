package com.tproject.workshop.validation;

public final class PageBounds {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 15;
    public static final int MIN_PAGE = 0;
    public static final int MAX_PAGE = 100_000;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;

    private PageBounds() {}

    public static int normalizePage(Integer page) {
        return page == null ? DEFAULT_PAGE : page;
    }

    public static int normalizeSize(Integer size) {
        return size == null ? DEFAULT_SIZE : size;
    }
}
