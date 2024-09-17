import {ErrorStatus, model} from "@/model.js";

// This function partially checks the CBOM properties that are necessary for the frontend, but does not formally verifies the validity of the CBOM format
function checkCbomValidity(cbom) {
  var isValid = true;
  var isIgnoringSomeComponent = false;
  var errorMessages = [];

  if (cbom === undefined || cbom == null) {
    isValid = false;
    errorMessages.push("CBOM is undefined or null.");
  } else {
    if (!Object.hasOwn(cbom, "bomFormat")) {
      isValid = false;
      errorMessages.push("Missing mandatory field: bomFormat.");
    }
    if (!Object.hasOwn(cbom, "specVersion")) {
      isValid = false;
      errorMessages.push("Missing mandatory field: specVersion.");
    }
    if (!Object.hasOwn(cbom, "serialNumber")) {
      isValid = false;
      errorMessages.push("Missing mandatory field: serialNumber.");
    }
    if (!Object.hasOwn(cbom, "version")) {
      isValid = false;
      errorMessages.push("Missing mandatory field: version.");
    }
    if (!Object.hasOwn(cbom, "components")) {
      // We can have a valid CBOM with no components
      return;
    } else if (!Array.isArray(cbom.components)) {
      isValid = false;
      errorMessages.push("Components field is not an array.");
    } else {
      cbom.components.forEach(function (component, index) {
        if (!Object.hasOwn(component, "type")) {
          isValid = false;
          errorMessages.push(`Component at index ${index} is missing mandatory field: type.`);
        } else {
          if (component.type !== "cryptographic-asset") {
            isIgnoringSomeComponent = true
            console.warn(`Ignoring CBOM component at index ${index} of type: ${component.type}`)
          } else {
            if (!Object.hasOwn(component, "cryptoProperties")) {
              isValid = false;
              errorMessages.push(`Component at index ${index} is missing mandatory field: cryptoProperties.`);
            }
            // if (!Object.hasOwn(component, "evidence")) {
            //   isValid = false;
            //   errorMessages.push(`Component at index ${index} is missing mandatory field: evidence.`);
            // } else if (!Object.hasOwn(component.evidence, "occurrences")) {
            //   isValid = false;
            //   errorMessages.push(`Component at index ${index} is missing mandatory field: evidence.occurrences.`);
            // } else if (!Array.isArray(component.evidence.occurrences)) {
            //   isValid = false;
            //   errorMessages.push(`evidence.occurrences for component at index ${index} is not an array.`);
            // }
          }
        }
      });
    }
  }

  if (isIgnoringSomeComponent) {
    model.addError(ErrorStatus.IgnoredComponent);
  }
  if (!isValid) {
    console.error(`Invalid CBOM detected. ${errorMessages.length} errors:\n   - ${errorMessages.join("\n   - ")}`);
    model.addError(ErrorStatus.InvalidCbom);
  }
}


export function setCbom(cbom) {
  checkCbomValidity(cbom);
  model.cbom = cbom;

  if (Object.hasOwn(cbom, "metadata")) {
    if (Object.hasOwn(cbom.metadata, "properties") && Array.isArray(cbom.metadata.properties)) {
      model.codeOrigin.gitPurls = []
      cbom.metadata.properties.forEach(function (prop) {
        if (Object.hasOwn(prop, "name") && Object.hasOwn(prop, "value")) {
          switch (prop.name) {
            case "git-url":
              model.codeOrigin.gitLink = prop.value;
              break;
            case "git-branch":
              model.codeOrigin.gitBranch = prop.value;
              break;
            case "git-subfolder":
              model.codeOrigin.gitSubfolder = prop.value
              break;
            case "purl":
              model.codeOrigin.gitPurls.push(prop.value)
              break;
            case "commit":
              model.codeOrigin.commitID = prop.value
          }
        }
      });
    }
  }
}

export function showResultFromApi(cbomApi) {
  let cbom = getCbomFromScan(cbomApi);
  setCbom(cbom);
  model.codeOrigin.gitLink = cbomApi.gitUrl;
  model.codeOrigin.gitBranch = cbomApi.branch;
  model.showResults = true;
}

export function showResultFromUpload(cbom, name) {
  setCbom(cbom);
  model.codeOrigin.uploadedFileName = name;
  model.showResults = true;
}

// Takes a Scab object as returned by the API and returns the CBOM as an Object.
export function getCbomFromScan(scan) {
  if (scan && scan.bom) {
    return scan.bom
  }
  console.error("Error fetching latest CBOM");
  model.addError(ErrorStatus.InvalidCbom);
  return JSON.parse("{}");

}

export function getDetections() {
  var detections = getDetectionsFromCbom(model.cbom);
  if (model.scanning.isScanning) {
    detections = model.scanning.liveDetections;
  }
  return detections;
}

// Takes a CBOM Object and returns an array of detections
export function getDetectionsFromCbom(cbom) {
  // No invalid CBOM error handling occurs in this method because it is used to display reactive information, which could result in an infinite loop of errors
  try {
    if (cbom === undefined || cbom === null) {
      return [];
    }
    if (Object.hasOwn(cbom, "components") && Array.isArray(cbom.components)) {
      var detections = [];
      // In a CBOM, each component can be detected in several contexts.
      // To display them, we 'unwrap' all contexts and return a list of detections where each component appears with a single context.
      cbom.components.forEach(function (component) {
        if (Object.hasOwn(component, "type") && component.type === "cryptographic-asset") {
          if (
              Object.hasOwn(component, "evidence") &&
              Object.hasOwn(component.evidence, "occurrences") &&
              Array.isArray(component.evidence.occurrences)
          ) {
            component.evidence.occurrences.forEach(function (
              singleContext
            ) {
              var detectionWithSingleContext = JSON.parse(
                JSON.stringify(component)
              );
              detectionWithSingleContext.evidence.occurrences = [
                singleContext,
              ];
              detections.push(detectionWithSingleContext);
            });
          } else {
            // The component has no occurence
            detections.push(component);
          }
        }
      });
      return detections;
    } else {
      return []; // The "components" array is either missing or not an array.
    }
  } catch (error) {
    console.error("Error parsing JSON:", error);
    model.addError(ErrorStatus.JsonParsing);
    return []; // An error occurred while parsing the JSON string.
  }
}
