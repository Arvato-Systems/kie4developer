/*
 * Copyright 2021 Arvato Systems GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arvato.workflow.kie4developer.workitemhandler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Helper to access the spring context from non-spring classes.
 */
@Component
public class SpringContext implements ApplicationContextAware {

  private static ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    // store ApplicationContext reference to access required beans later on
    SpringContext.context = context;
  }

  /**
   * Returns the Spring managed bean instance of the given class type if it exists. Returns <code>null</code>
   * otherwise.
   *
   * @param beanClass the class to look for
   * @return the bean or <code>null</code>
   */
  public static <T extends Object> T getBean(Class<T> beanClass) {
    return context.getBean(beanClass);
  }


}
