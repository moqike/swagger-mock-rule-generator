package io.swagger.codegen;

import io.swagger.codegen.*;
import io.swagger.codegen.languages.AbstractTypeScriptClientCodegen;
import io.swagger.models.Model;
import io.swagger.models.properties.*;
import io.swagger.util.Json;

import java.util.*;
import java.io.File;

public class RuleGenerator extends AbstractTypeScriptClientCodegen {

  // source folder where to write the files
  protected String sourceFolder = "src";
  protected String apiVersion = "1.0.0";

  /**
   * Configures the type of generator.
   * 
   * @return  the CodegenType for this generator
   * @see     io.swagger.codegen.CodegenType
   */
  public CodegenType getTag() {
    return CodegenType.CLIENT;
  }

  /**
   * Configures a friendly name for the generator.  This will be used by the generator
   * to select the library with the -l flag.
   * 
   * @return the friendly name for the generator
   */
  public String getName() {
    return "rule";
  }

  /**
   * Returns human-friendly help for the generator.  Provide the consumer with help
   * tips, parameters here
   * 
   * @return A string value for the help message
   */
  public String getHelp() {
    return "Generates a rule client library.";
  }

  public RuleGenerator() {
    super();

    // set the output folder here
    outputFolder = "generated-code/rule";

    /**
     * Models.  You can write model files using the modelTemplateFiles map.
     * if you want to create one template for file, you can do so here.
     * for multiple files for model, just put another entry in the `modelTemplateFiles` with
     * a different extension
     */
    modelTemplateFiles.put(
      "model.mustache", // the template to use
      ".ts");       // the extension for each file to write

    /**
     * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
     * as with models, add multiple entries with different extensions for multiple files per
     * class
     */
    apiTemplateFiles.put(
      "api.mustache",   // the template to use
      ".ts");       // the extension for each file to write

    /**
     * Template Location.  This is the location which templates will be read from.  The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "rule";

    /**
     * Api Package.  Optional, if needed, this can be used in templates
     */
    //apiPackage = "io.swagger.client.api";
    apiPackage = "validator";

    /**
     * Model Package.  Optional, if needed, this can be used in templates
     */
    //modelPackage = "io.swagger.client.model";
    modelPackage = "model";

    /**
     * Additional Properties.  These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */
    additionalProperties.put("apiVersion", apiVersion);
  }

  /**
   * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
   * those terms here.  This logic is only called if a variable matches the reserved words
   * 
   * @return the escaped term
   */
  @Override
  public String escapeReservedWord(String name) {
    return "_" + name;  // add an underscore to the name
  }

  /**
   * Location to write model files.  You can use the modelPackage() as defined when the class is
   * instantiated
   */
  public String modelFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
  }

  /**
   * Location to write api files.  You can use the apiPackage() as defined when the class is
   * instantiated
   */
  @Override
  public String apiFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
  }

  @Override
  public Map<String, Object> postProcessOperations(Map<String, Object> operations) {
    Map<String, Object> objs = (Map<String, Object>) operations.get("operations");

    // Add filename information for api imports
    objs.put("apiFilename", getApiFilenameFromClassname(objs.get("classname").toString()));

    List<CodegenOperation> ops = (List<CodegenOperation>) objs.get("operation");
    for (CodegenOperation op : ops) {
      if (!op.hasConsumes) {
        ArrayList<Map<String, String>> consumes = new ArrayList<Map<String, String>>();
        Map<String, String> consume = new HashMap<>();
        consume.put("mediaType", "application/json");
        consumes.add(consume);
        op.consumes = consumes;
      }
      for (Map<String, String> consume : op.consumes) {
        if (consume.get("mediaType").equals("multipart/form-data")) {
          consume.put("ruleBodyType", "form");
        } else {
          consume.put("ruleBodyType", "json");
        }
        // TODO: support text
      }
    }

    // Add additional filename information for model imports in the services
    List<Map<String, Object>> imports = (List<Map<String, Object>>) operations.get("imports");
    for (Map<String, Object> im : imports) {
      im.put("filename", im.get("import"));
      im.put("classname", getModelnameFromModelFilename(im.get("filename").toString()));
    }
    additionalProperties.put("refs", imports);
    return operations;
  }

  private Property modifyRef(Property property) {
    if (property instanceof RefProperty) {
      String ref = ((RefProperty) property).get$ref();
      ref = ref.substring("#/definitions".length());
      ((RefProperty) property).set$ref(ref);
    }
    return property;
  }

  @Override
  public CodegenModel fromModel(String name, Model model, Map<String, Model> allDefinitions) {
    CodegenModel codegenModel = super.fromModel(name, model, allDefinitions);
    Map<String, Property> properties = model.getProperties();

    for (Property property : properties.values()) {
      if (property instanceof RefProperty) {
        modifyRef(property);
      } else if (property instanceof ArrayProperty) {
        Property itemProperty = ((ArrayProperty) property).getItems();
        modifyRef(itemProperty);

      }
    }
    codegenModel.modelJson = Json.pretty(model);
    return codegenModel;
  }


  @Override
  public String toApiImport(String name) {
    return apiPackage() + "/" + toApiFilename(name);
  }

  @Override
  public String toModelFilename(String name) {
    return camelize(toModelName(name), false);
  }

  @Override
  public String toModelImport(String name) {
    return modelPackage() + "/" + toModelFilename(name);
  }

  private String getApiFilenameFromClassname(String classname) {
    String name = classname;
    return toApiFilename(name);
  }

  private String getModelnameFromModelFilename(String filename) {
    String name = filename.substring((modelPackage() + "/").length());
    return camelize(name);
  }
}