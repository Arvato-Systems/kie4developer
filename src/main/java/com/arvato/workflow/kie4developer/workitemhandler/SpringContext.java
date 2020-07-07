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
