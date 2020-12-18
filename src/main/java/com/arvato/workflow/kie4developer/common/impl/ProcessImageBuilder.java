package com.arvato.workflow.kie4developer.common.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.PathParser;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.io.FileUtils;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.workflow.core.node.ActionNode;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.EventNode;
import org.jbpm.workflow.core.node.HumanTaskNode;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.core.node.Split;
import org.jbpm.workflow.core.node.StartNode;
import org.jbpm.workflow.core.node.SubProcessNode;
import org.jbpm.workflow.core.node.TimerNode;
import org.jbpm.workflow.core.node.WorkItemNode;
import org.kie.api.definition.process.Connection;
import org.kie.api.definition.process.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.svg.SVGDocument;

/**
 * Builder to create BPM Process Definition Images
 *
 * @author TRIBE01
 */
public class ProcessImageBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessImageBuilder.class);
  private static final int maxCharsInLine = 12;

  private ProcessImageBuilder() {
  }

  /**
   * Create the process model image (SVG) of a given process src for icons: https://iconify.design/icon-sets/bpmn/
   *
   * @param process the process model to use
   * @return the image
   * @throws Exception on any Exception
   */
  public static byte[] createImage(RuleFlowProcess process) throws Exception {
    DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
    SVGDocument document = (SVGDocument) impl.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

    svgGenerator.setFont(new Font("Arial", 0, 11));

    for (Node node : process.getNodes()) {
      int x = (int) node.getMetaData().get("x");
      int y = (int) node.getMetaData().get("y");
      int width = (int) node.getMetaData().get("width");
      int height = (int) node.getMetaData().get("height");

      //TODO: use specified connection waypoints from XML & show names
      for (List<Connection> connections : node.getOutgoingConnections().values()) {
        for (Connection connection : connections) {
          int x1 = (int) connection.getFrom().getMetaData().get("x");
          int y1 = (int) connection.getFrom().getMetaData().get("y");
          int width1 = (int) connection.getFrom().getMetaData().get("width");
          int height1 = (int) connection.getFrom().getMetaData().get("height");
          int x2 = (int) connection.getTo().getMetaData().get("x");
          int y2 = (int) connection.getTo().getMetaData().get("y");
          //int width2 = (int) connection.getTo().getMetaData().get("width");
          int height2 = (int) connection.getTo().getMetaData().get("height");
          drawEdge(svgGenerator, x1 + width1, y1 + (height1 / 2), x2, y2 + (height2 / 2), "");
        }
      }

      String name = node.getName() == null ? "" : node.getName();
      if (node instanceof StartNode || node instanceof EndNode || node instanceof TimerNode || node instanceof EventNode) {
        String type = null;
        if (node instanceof StartNode) {
          type = "start";
        } else if (node instanceof TimerNode) {
          type = "timer";
        } else if (node instanceof EventNode) {
          type = "event";
        } else if (node instanceof EndNode) {
          type = "end";
        }
        drawEvent(svgGenerator, x, y, width, height, name, type);
      } else if (node instanceof Join || node instanceof Split) {
        String type = null;
        if (node instanceof Join) {
          Join joinNode = (Join) node;
          if (joinNode.getType() == Join.TYPE_AND) {
            type = "and";
          } else if (joinNode.getType() == Join.TYPE_XOR) {
            type = "xor";
          }
        } else if (node instanceof Split) {
          Split splitNode = (Split) node;
          if (splitNode.getType() == Split.TYPE_AND) {
            type = "and";
          } else if (splitNode.getType() == Split.TYPE_XOR) {
            type = "xor";
          }
        }
        drawGateway(svgGenerator, x, y, width, height, name, type);
      } else if (node instanceof ActionNode || node instanceof WorkItemNode || node instanceof HumanTaskNode || node instanceof SubProcessNode) {
        String type = null;
        if (node instanceof ActionNode) {
          type = "script";
        } else if (node instanceof HumanTaskNode) {
          type = "user";
        } else if (node instanceof WorkItemNode) {
          type = "service";
        } else if (node instanceof SubProcessNode) {
          type = "call";
        }
        drawActivity(svgGenerator, x, y, width, height, name, type);
      } else {
        LOGGER.error(String.format("unsupported node with id '%s' on process with id '%s'", node.getId(), process.getId()));
      }
    }

    File svgFile = Files.createTempFile(null, ".svg").toFile();
    svgFile.deleteOnExit();
    FileWriter out = new FileWriter(svgFile);
    svgGenerator.stream(out, true);

    return FileUtils.readFileToByteArray(svgFile);
  }

  private static void drawActivity(SVGGraphics2D svgGenerator, int x, int y, int width, int height, String name,
      String type) {
    int CHAR_HEIGHT = svgGenerator.getFontMetrics().getHeight();
    int CHAR_WIDTH = (int) Arrays.stream(svgGenerator.getFontMetrics().getWidths()).average().getAsDouble();
    Shape shape = new RoundRectangle2D.Double(x, y, width, height, 20, 20);
    svgGenerator.setPaint(Color.black);
    svgGenerator.draw(shape);

    if (type == "script") {
      String path1 = "M775.382 435.919h688.37c-95.662 59.648-162.469 114.48-206.577 166.003c-49.32 57.612-70.521 111.644-71.137 162.45c-1.232 101.61 77.337 185.164 155.475 265.19c78.139 80.025 156.036 157.301 164.666 239.46c4.314 41.078-7.507 84.285-46.402 133.344c-38.553 48.628-104.083 102.354-205.449 161.715H566.383c84.656-52.562 142.286-101.176 178.994-147.476c41.632-52.511 56.198-102.74 51.236-149.978c-9.924-94.479-93.222-173.357-171.062-253.077c-77.84-79.72-150.037-159.505-148.954-248.9c.542-44.698 18.77-93.086 65.628-147.822c46.482-54.295 121.262-114.35 233.157-180.91zM769.016 413l-2.699 1.603c-114.757 68.004-192.18 129.707-241.5 187.32c-49.322 57.611-70.522 111.643-71.138 162.449c-1.232 101.61 77.332 185.164 155.47 265.19c78.14 80.025 156.041 157.301 164.67 239.46c4.315 41.078-7.506 84.285-46.4 133.344c-38.896 49.059-105.134 103.29-208.041 163.278L482.739 1587h777.865l2.677-1.558c104.39-60.852 172.823-116.326 214.455-168.837c41.632-52.511 56.198-102.74 51.236-149.978c-9.924-94.479-93.222-173.357-171.062-253.077c-77.84-79.72-150.037-159.505-148.954-248.9c.542-44.698 18.77-93.086 65.628-147.822c46.858-54.735 122.367-115.302 235.775-182.507L1546.335 413z";
      String path2 = "M916.72 1332.912v22.918h437.988v-22.918zm-63.196-231.175v22.92h421.987v-22.92zm-206.795-231.17v22.92h436.384v-22.92zm16.402-231.173v22.918h421.399v-22.918z";
      Shape pathShape = getShapeFromPath(path1 + " " + path2, 20, 20, x, y);
      svgGenerator.setPaint(Color.black);
      svgGenerator.draw(pathShape);
    } else if (type == "service") {
      String path1 = "M1130.71 627.789c-38.976-25.404-82.7-43.13-128.386-52.095l.591-102.517h-142.26h0l.267 103.322c-22.455 4.662-44.772 11.39-66.634 20.506c-21.861 9.145-42.217 20.327-61.158 32.966l-74.256-73.362l-99.45 100.026l74.164 73.264c-25.558 38.817-43.358 82.212-52.35 127.764l-105.291.19V998.58l106.158-.382c4.655 22.218 11.411 44.494 20.585 66.175c9.22 21.82 20.401 42.082 33.18 60.847";
      String path2 = "M997.93 547.347v-97.233c20.037-4 40.034-10.123 59.58-18.235c19.587-8.209 37.866-18.236 54.959-29.592l67.921 66.967l89.145-89.487l-67.963-66.973a322.22 322.22 0 0 0 46.944-114.535l92.253-.568V71.73l-93.07.568c-4.17-19.95-10.142-39.738-18.402-59.23c-8.26-19.526-18.279-37.705-29.728-54.604l64.323-64.485l-90.044-88.696l-64.242 64.461c-34.921-22.76-74.096-38.642-115.029-46.675l.529-91.85H867.648l.239 92.572c-20.119 4.177-40.115 10.202-59.702 18.37c-19.587 8.194-37.825 18.213-54.796 29.537l-66.53-65.73l-89.104 89.62l66.45 65.642C641.304-3.993 625.356 34.889 617.3 75.7l-94.338.17v126.084l95.115-.34c4.17 19.906 10.223 39.863 18.442 59.288c8.26 19.549 18.279 37.704 29.729 54.517L598.04 383.36l90.085 88.536l68.044-67.73c34.922 22.897 74.097 38.722 115.07 46.723l.043 96.414c42.195.32 126.647.043 126.647.043z";
      Shape pathShape1 = getShapeFromPath(path2, 20, 20, x, y + 20);
      Shape pathShape2 = getShapeFromPath(path1, 15, 15, x - 3, y - 2);
      svgGenerator.setPaint(Color.black);
      svgGenerator.draw(pathShape1);
      svgGenerator.draw(pathShape2);
      Ellipse2D ellipse = new Ellipse2D.Double(x + 20, y + 20, 6, 6);
      svgGenerator.draw(ellipse);
    } else if (type == "user") {
      String path1 = "M722.297 656.594C722.317 737.279 768 840 821.517 880.15c14.362 10.775 1.471 7.63 18.999 18.584C704.714 939.974 528 1010 448 1160v420h1087.5v-420c-80-150-256.714-220.026-392.515-261.266c96-60 118.195-143.433 118.218-242.14C1260.89 520 1151.5 400 991.75 400C832 400 722.61 520 722.297 656.594z";
      String path2 = "M684 1560v-280";
      String path3 = "M1298 1560v-280";
      String path4 = "M768 920s-37.712 122.288 0 160c105.595 105.595 342.405 105.595 448 0c37.712-37.712 0-160 0-160";
      Shape pathShape = getShapeFromPath(path1 + " " + path2 + " " + path3 + " " + path4, 20, 20, x, y);
      svgGenerator.setPaint(Color.black);
      svgGenerator.draw(pathShape);
      String path5 = "M752 550c38.64 3.52 39.962-25.563 145.847-19.322c158.153 9.322 107.028 57.135 214.93 60.28c84.098-3.145 80.524-29.91 119.223-40.958c-64-70-127.297-114.535-240-150c-96 30-176 80-240 150z";
      Shape pathShape2 = getShapeFromPath(path5, 11, 5, x - 4, y - 4);
      svgGenerator.fill(pathShape2);
    } else if (type == "call") {
      String path = "M300 300v1400h1400V300zm88 88h1224v1224H388zm522 212v310H600v180h310v310h180v-310h310V910h-310V600z";
      Shape pathShape = getShapeFromPath(path, 15, 15, x + (width /2) - 10, y + height - 23);
      svgGenerator.setPaint(Color.black);
      svgGenerator.fill(pathShape);
    }

    String line = "";
    int lineNo = 0;
    for (String word : name.split(" ")) {
      line += word;
      if (line.length() >= maxCharsInLine) {
        int textWidth = CHAR_WIDTH * line.length();
        int xAdjustment = (textWidth / 2);
        svgGenerator.drawString(line, x + (width / 2) - xAdjustment,
            y + (height / 2) + (CHAR_HEIGHT / 2) + (lineNo * CHAR_HEIGHT));
        line = "";
        lineNo++;
      } else {
        line += " ";
      }
    }
    int textWidth = CHAR_WIDTH * line.length();
    int xAdjustment = (textWidth / 2);
    svgGenerator.drawString(line, x + (width / 2) - xAdjustment,
        y + (height / 2) + (CHAR_HEIGHT / 2) + (lineNo * CHAR_HEIGHT));
  }

  private static Shape getShapeFromPath(String path, int width, int height, int x, int y) {
    AWTPathProducer app = new AWTPathProducer();
    PathParser parser = new PathParser();
    parser.setPathHandler(app);
    parser.parse(path);
    Shape shape = AffineTransform.getScaleInstance(
        width / app.getShape().getBounds2D().getWidth(),
        height / app.getShape().getBounds2D().getHeight()
    ).createTransformedShape(app.getShape());
    shape = AffineTransform.getTranslateInstance(x, y).createTransformedShape(shape);
    return shape;
  }

  private static void drawEvent(SVGGraphics2D svgGenerator, int x, int y, int width, int height, String name,
      String type) {
    int CHAR_HEIGHT = svgGenerator.getFontMetrics().getHeight();
    int CHAR_WIDTH = (int) Arrays.stream(svgGenerator.getFontMetrics().getWidths()).average().getAsDouble();
    Shape shape = new Ellipse2D.Double(x, y, width, height);
    svgGenerator.setPaint(Color.black);
    if (type == "end") {
      svgGenerator.setStroke(new BasicStroke(4));
    }
    svgGenerator.draw(shape);
    svgGenerator.setStroke(new BasicStroke());

    if (type == "timer" || type == "event") {
      shape = new Ellipse2D.Double(x+4, y+4, width -8, height-8);
      svgGenerator.setPaint(Color.black);
      svgGenerator.draw(shape);

      if (type == "timer") {
        Shape icon = new Ellipse2D.Double(x+10, y+10, width -20, height-20);
        svgGenerator.setPaint(Color.black);
        svgGenerator.draw(icon);
        icon = new Ellipse2D.Double(x + (width / 2) - 1, y + (height/2) - 1, 2, 2);
        svgGenerator.fill(icon);
        icon = new Line2D.Double(x + (width / 2), y + (height/2), x + (width / 2) + 5, y + (height/2));
        svgGenerator.draw(icon);
        icon = new Line2D.Double(x + (width / 2), y + (height/2), x + (width / 2) + 4, y + (height/2) - 6);
        svgGenerator.draw(icon);
      }
    }

    int textWidth = CHAR_WIDTH * name.length();
    int xAdjustment = (textWidth / 2);
    svgGenerator.drawString(name, x + (width / 2) - xAdjustment, y + height + CHAR_HEIGHT);
  }

  private static void drawGateway(SVGGraphics2D svgGenerator, int x, int y, int width, int height, String name,
      String type) {
    GeneralPath shape = new GeneralPath();
    shape.moveTo(x, y + (height / 2));
    shape.lineTo(x + (width / 2), y);
    shape.lineTo(x + width, y + (height / 2));
    shape.lineTo(x + (width / 2), y + height);
    shape.lineTo(x, y + (height / 2));
    svgGenerator.setPaint(Color.black);
    svgGenerator.draw(shape);

    double space = width / 2.5;
    if (type == "xor") {
      GeneralPath icon = new GeneralPath();
      icon.moveTo(x + space, y + space);
      icon.lineTo(x + width - space, y + height - space);
      icon.moveTo(x + width - space, y + space);
      icon.lineTo(x + space, y + height - space);
      svgGenerator.setStroke(new BasicStroke(4));
      svgGenerator.draw(icon);
      svgGenerator.setStroke(new BasicStroke());
    } else if (type == "and") {
      GeneralPath icon = new GeneralPath();
      icon.moveTo(x + (width / 2), y + space);
      icon.lineTo(x + (width / 2), y + height - space);
      icon.moveTo(x + space, y + (height / 2));
      icon.lineTo(x + width - space, y + (height / 2));
      svgGenerator.setStroke(new BasicStroke(4));
      svgGenerator.draw(icon);
      svgGenerator.setStroke(new BasicStroke());
    }

    svgGenerator.setFont(new Font("Arial", 0, 11));
    svgGenerator.drawString(name, x, y);
  }

  private static void drawEdge(SVGGraphics2D svgGenerator, int x1, int y1, int x2, int y2, String name) {
    Shape shape = new Line2D.Double(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
    svgGenerator.setPaint(Color.black);
    svgGenerator.draw(shape);

    GeneralPath triangle = new GeneralPath();
    triangle.moveTo(x2 - 8, y2 - 4);
    triangle.lineTo(x2 - 8, y2 + 4);
    triangle.lineTo(x2, y2);
    triangle.lineTo(x2 - 8, y2 - 4);
    triangle.closePath();
    svgGenerator.fill(triangle);
    svgGenerator.draw(triangle);

    svgGenerator.setFont(new Font("Arial", 0, 11));
    svgGenerator.drawString(name, x1 + ((x2 - x1) / 6), y1 + ((y2 - y1) / 2));
  }
}
