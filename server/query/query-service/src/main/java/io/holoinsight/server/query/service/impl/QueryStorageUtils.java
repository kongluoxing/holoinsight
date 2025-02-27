/*
 * Copyright 2022 Holoinsight Project Authors. Licensed under Apache-2.0.
 */
package io.holoinsight.server.query.service.impl;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.holoinsight.server.extension.model.QueryMetricsParam;
import io.holoinsight.server.extension.model.QueryParam;
import io.holoinsight.server.extension.model.QueryResult;
import io.holoinsight.server.query.grpc.QueryProto;

/**
 * <p>
 * created at 2023/1/30
 *
 * @author xzchaoo
 */
public class QueryStorageUtils {

  public static QueryMetricsParam convertToQueryMetricsParam(
      QueryProto.QueryMetricsRequest request) {
    QueryMetricsParam param = new QueryMetricsParam();
    param.setTenant(request.getTenant());
    param.setName(request.getName());
    // TODO
    // 是否精确查询
    // bool explicit = 3;
    param.setLimit(request.getLimit());

    return param;
  }

  public static QueryProto.Result convertToPb(QueryResult.Result result) {
    QueryProto.Result.Builder b = QueryProto.Result.newBuilder();
    if (result.getMetric() != null) {
      b.setMetric(result.getMetric());
    }
    if (result.getPoints() != null) {
      for (QueryResult.Point p : result.getPoints()) {
        QueryProto.Point.Builder pointB = QueryProto.Point.newBuilder();
        pointB.setTimestamp(p.getTimestamp());
        pointB.setValue(p.getValue());
        if (p.getStrValue() != null) {
          pointB.setStrValue(p.getStrValue());
        }
        b.addPoints(pointB.build());
      }
    }
    if (result.getTags() != null) {
      b.putAllTags(result.getTags());
    }
    return b.build();
  }

  public static QueryParam convertToQueryParam(String tenant, QueryProto.Datasource d) {
    QueryParam param = new QueryParam();
    param.setTenant(tenant);
    param.setStart(d.getStart());
    param.setEnd(d.getEnd());
    param.setMetric(d.getMetric());
    param.setDownsample(d.getDownsample());
    param.setAggregator(d.getAggregator());
    if (StringUtils.isEmpty(param.getAggregator())) {
      param.setAggregator("none");
    }

    if (d.hasSlidingWindow()) {
      QueryParam.SlidingWindow sw = new QueryParam.SlidingWindow();
      sw.setAggregator(d.getSlidingWindow().getAggregator());
      sw.setWindowMs(d.getSlidingWindow().getWindowMs());
      param.setSlidingWindow(sw);
    }

    param.setFilters(d.getFiltersList().stream().map(pb -> {
      QueryParam.QueryFilter f = new QueryParam.QueryFilter();
      f.setType(pb.getType());
      f.setName(pb.getName());
      f.setValue(pb.getValue());
      return f;
    }).collect(Collectors.toList()));

    param.setGroupBy(d.getGroupByList());

    return param;
  }


  public static long convertDownSample(String downsample) {

    long sample = 1000;
    switch (downsample) {
      case "1s":
        sample = 1000;
        break;
      case "5s":
        sample = 5000;
        break;
      case "10s":
        sample = 10000;
        break;
      case "30s":
        sample = 30000;
        break;
      case "1min":
      case "1m":
        sample = 60000;
        break;
      case "5min":
      case "5m":
        sample = 60000 * 5;
        break;
      case "10min":
      case "10m":
        sample = 60000 * 10;
        break;
      case "30min":
      case "30m":
        sample = 60000 * 30;
        break;
      case "1hour":
      case "1h":
        sample = 60000 * 60;
        break;
      case "1day":
      case "1d":
        sample = 60000 * 60 * 24;
        break;
      default:
        throw new IllegalArgumentException("unknown downsample: " + downsample);
    }

    return sample;
  }

  public static Object convertFillPolocy(String fillPolicy) {
    Object fill = null;
    switch (fillPolicy) {
      case "nan":
        fill = "NaN";
        break;
      case "null":
        fill = "null";
        break;
      case "zero":
        fill = 0;
        break;
      case "percent":
        fill = 100;
        break;
      case "none":
      default:
        break;
    }
    return fill;
  }
}
