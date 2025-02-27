/*
 * Copyright 2022 Holoinsight Project Authors. Licensed under Apache-2.0.
 */
package io.holoinsight.server.storage.web.initializer;

import io.holoinsight.server.storage.core.ModelCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * @author jiwliu
 * @version : TatrisInitializer.java, v 0.1 2022年10月12日 19:31 xiangwanpeng Exp $
 */
@Slf4j
public class TatrisInitializer implements ApplicationRunner {

  @Autowired
  private ModelCenter modelManager;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    try {
      modelManager.init();
      log.info("tatris init finish");
    } catch (Exception e) {
      log.error("[Tatris] init fail, msg={}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
