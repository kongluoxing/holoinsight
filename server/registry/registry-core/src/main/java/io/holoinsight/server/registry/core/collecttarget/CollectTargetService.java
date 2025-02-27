/*
 * Copyright 2022 Holoinsight Project Authors. Licensed under Apache-2.0.
 */
package io.holoinsight.server.registry.core.collecttarget;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.holoinsight.server.meta.common.model.QueryExample;
import io.holoinsight.server.meta.facade.service.DataClientService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.holoinsight.server.registry.core.agent.Agent;
import io.holoinsight.server.registry.core.agent.AgentStorage;
import io.holoinsight.server.registry.core.agent.CentralAgentService;
import io.holoinsight.server.registry.core.agent.DaemonsetAgent;
import io.holoinsight.server.registry.core.agent.DaemonsetAgentService;
import io.holoinsight.server.registry.core.dim.GaeaDimService;
import io.holoinsight.server.registry.core.template.CollectRange;
import io.holoinsight.server.registry.core.template.CollectTemplate;
import io.holoinsight.server.registry.core.template.ExecutorSelector;

/**
 * <p>
 * created at 2022/3/3
 *
 * @author zzhb101
 */
@Service
public class CollectTargetService {
  @Autowired(required = false)
  private GaeaDimService gaeaDimService;
  @Autowired
  private DataClientService dataClientService;
  @Autowired
  private AgentStorage agentStorage;
  @Autowired
  private CentralAgentService centralAgentService;
  @Autowired
  private DaemonsetAgentService daemonsetAgentService;

  /**
   * TODO 考虑一下有多个 refAgents 的case
   *
   * @param t
   * @param dimRow
   * @return
   */
  public String buildRefAgent(CollectTemplate t, Target dimRow) {
    String refAgent = buildRefAgent0(t, dimRow);
    if (refAgent == null) {
      String msg =
          String.format("table=[%d/%s] dim=[%s]", t.getId(), t.getTableName(), dimRow.getId());
      throw new IllegalStateException(msg);
    }
    return refAgent;
  }

  private String buildRefAgent0(CollectTemplate t, Target dimRow) {
    ExecutorSelector es = t.getExecutorSelector();
    if (es.getType() == null) {
      throw new IllegalArgumentException("ExecutorSelector.type is null" + t.getId());
    }

    switch (es.getType()) {
      case ExecutorSelector.SIDECAR:
        Map<String, Object> inner = (Map<String, Object>) dimRow.getInner();
        String tenant = t.getTenant();
        String hostIP = (String) inner.get("hostIP");
        if (StringUtils.isNotEmpty(hostIP)) {
          DaemonsetAgentService.State state = daemonsetAgentService.getState();
          DaemonsetAgent da = state.getAgents().get(new DaemonsetAgent.Key(tenant, hostIP));
          if (da != null) {
            return da.getHostAgentId();
          }
        }
        return (String) inner.get("agentId");
      case ExecutorSelector.FIXED:
        // 暂未实现
        throw new IllegalArgumentException("unsupported ExecutorSelector " + es.getType());
      case ExecutorSelector.DIM:
        throw new IllegalStateException("");
      case ExecutorSelector.CENTRAL:
        CentralAgentService.State state = centralAgentService.getState();
        // TODO 选择哪个集群?
        String requiredClusterName = "global0";
        CentralAgentService.ClusterState cluster = state.getClusters().get(requiredClusterName);
        if (cluster == null) {
          throw new IllegalStateException("no central agent cluster " + requiredClusterName);
        }
        if (cluster.getAgents().isEmpty()) {
          throw new IllegalStateException(
              "central agent cluster " + requiredClusterName + " is empty");
        }
        // TODO 使用一致性哈希
        Agent agent = cluster.getRing().select(t.getTableName().hashCode());
        if (agent == null) {
          throw new IllegalStateException(
              "central agent cluster " + requiredClusterName + " hash ring select null");
        }
        return agent.getId();
      default:
        throw new IllegalArgumentException("unsupported ExecutorSelector " + es.getType());
    }
  }

  public List<Target> getTargets(CollectTemplate t) {
    CollectRange cr = t.getCollectRange();
    if (cr.getType() == null) {
      throw new IllegalStateException("collectRange.type is null");
    }
    switch (cr.getType()) {
      case CollectRange.CLOUDMONITOR: {
        // 去重
        CollectRange.Cloudmonitor cm = cr.getCloudmonitor();
        if (cm.getRanges().size() == 1) {
          return dataClientService.queryByExample(cm.getTable(), cm.getRanges().get(0)) //
              .stream().map(Target::of2).collect(Collectors.toList()); //
        }
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (QueryExample r : cm.getRanges()) {
          List<Map<String, Object>> rows = dataClientService.queryByExample(cm.getTable(), r);
          for (Map<String, Object> m : rows) {
            result.put((String) m.get("_uk"), m);
          }
        }
        return result.values().stream().map(Target::of2).collect(Collectors.toList());
      }
      case CollectRange.CENTRAL: {
        // 路由到一台中心化节点上
        return Collections.singletonList(Target.ofCentral(t.getTenant()));
      }
      default:
        throw new IllegalArgumentException("unsupported CollectRange " + cr.getType());
    }
  }
}
