package com.tproject.workshop.utils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.StringWriter;
import java.util.stream.Collectors;

public class UtilsSql {

    public static final String PAGINATION_PARAMS = "pagination_params";
    public static final String SORT_STATEMENT = "sort_statement";

    private static final String QUERY_PATH = "db/query/%s.sql";
    private static final MustacheFactory MF = new DefaultMustacheFactory();



    public static String getQuery(String queryName, Pageable pageable, Sort.Order defaultSort) {
        StringWriter stringWriter = new StringWriter();
        try {
            MF.compile(String.format(QUERY_PATH, queryName))
                    .execute(stringWriter,
                            getPaginationAndSortingContext(pageable.getPageNumber(), pageable.getPageSize(),
                                    pageable.getSort(), defaultSort));
            return stringWriter.toString();
        } catch (Exception ex) {
            //TODO: create an exception for this
//            throw new PixException(String.format("Não foi possível obter a query %s", queryName), ex);
            throw new RuntimeException(String.format("Não foi possível obter a query %s", queryName), ex);
        } finally {
            try {
                stringWriter.close();
            } catch (Exception ex) {
                // nothing
            }
        }
    }

    public static String getQuery(String queryName) {
        return LoadResourceUtil.getResource(String.format(QUERY_PATH, queryName));
    }

    public static String getQuery(String queryName, Pageable pageable) {
        return getQuery(queryName, pageable, null);
    }

    private static String getSortDesc(Sort.Order order) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, order.getProperty())
                + " " + order.getDirection();
    }

    private static ImmutableMap<String, Object> getPaginationAndSortingContext(int pageNumber, int pageSize,
                                                                               Sort sort, Sort.Order defaultSort) {

        ImmutableMap.Builder<String, Object> contextBuilder = ImmutableMap.builder();
        contextBuilder.put(PAGINATION_PARAMS, String
                .format("limit %d offset %d", pageSize + 1, pageSize * pageNumber));
        if (sort.isSorted()) {

            StringBuilder sortCriteria = new StringBuilder(sort.stream()
                    .map(UtilsSql::getSortDesc)
                    .collect(Collectors.joining(", ")));

            if (defaultSort != null) {
                sortCriteria.append(", ").append(getSortDesc(defaultSort));
            }

            contextBuilder.put(SORT_STATEMENT, String.format("order by %s", sortCriteria.toString()));

        }

        return contextBuilder.build();
    }
}
