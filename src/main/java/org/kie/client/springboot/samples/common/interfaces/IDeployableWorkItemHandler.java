package org.kie.client.springboot.samples.common.interfaces;

import java.io.File;
import org.kie.api.runtime.process.WorkItemHandler;
import org.springframework.stereotype.Component;

@Component
public interface IDeployableWorkItemHandler extends WorkItemHandler {

  /**
   * Get the name of the workitemhandler
   * @return the name
   */
  default String getName() {
    return this.getClass().getSimpleName();
  }

  /**
   * Get the package of the workitemhandler
   * @return the package
   */
  default String getPackage() {
    return this.getClass().getPackage().getName();
  }

  /**
   * Get the version of the workitemhandler
   * @return the version number
   */
  String getVersion();

  /**
   * Get the jar file that contains the WorkItemHandlers
   * @return the .jar file
   */
  File getWorkItemHandlerJarFile();
}
