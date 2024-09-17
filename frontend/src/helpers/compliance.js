import { model } from "@/model.js";
import { getDetections, getLocalComplianceServiceName } from "@/helpers.js";

// Dictionary mapping the icon enum values to the actual icon names
export const complianceIconMap = {
  CHECKMARK: 'Checkmark24',
  CHECKMARK_SECURE: 'Security24',
  WARNING: 'WarningAlt24',
  ERROR: 'MisuseOutline24',
  NOT_APPLICABLE: 'NotAvailable24',
  UNKNOWN: 'WatsonHealthImageAvailabilityUnavailable24'
};

// Returns the list findings for one asset (identified by its bom-ref)
export function getPolicyResultsByAsset(asset) {
  if (
    asset == null ||
    !Object.hasOwn(asset, "bom-ref") ||
    model.policyCheckResult == null
  ) {
    return [];
  }
  let bomRef = asset["bom-ref"];

  // Filter findings based on bomRef
  const filteredFindings = model.policyCheckResult.findings.filter(
    (finding) => finding["bomRef"] === bomRef
  );

  return filteredFindings;
}

// Returns the interger representing compliance for one asset
export function getComplianceLevel(asset) {
  if (!hasValidComplianceResults()) {
    return false
  }
  // Default compliance level
  let status = model.policyCheckResult.defaultComplianceLevel

  let complianceLevels = getPolicyResultsByAsset(asset).map((finding) => finding["levelId"]);
  if (complianceLevels.length > 0) {
    status = Math.min.apply(null, complianceLevels)
  }
  return status;
}

export function getComplianceObjectFromId(id) {
  let res = getComplianceLevels().filter((level) => level.id === id)
  if (res.length === 1) {
    return res[0]
  }
  console.error(`No compliance level has been found for an asset with compliance ID ${id}`)
}

function getComplianceObject(asset) {
  let levelId = getComplianceLevel(asset)
  return getComplianceObjectFromId(levelId)
}

// Returns the string representing the compliance color for on asset
export function getComplianceColor(asset) {
  return getComplianceObject(asset).colorHex
}

// Returns the string representing the compliance icon for on asset
export function getComplianceIcon(asset) {
  const complianceObject = getComplianceObject(asset);
  return complianceIconMap[complianceObject.icon];
}

// Returns the compliance label for an asset
export function getComplianceLabel(asset) {
  return getComplianceObject(asset).label
}

// Returns the compliance description for an asset (if there is no description, it uses the label)
export function getComplianceDescription(asset) {
  let description = getComplianceObject(asset).description
  if (description === undefined) {
    description = getComplianceLabel(asset)
  }
  return description
}

// Returns the list of findings of an asset having a message
export function getComplianceFindingsWithMessage(asset) {
  if (!hasValidComplianceResults() || asset == null) {
    return []
  }
  let findings = model.policyCheckResult.findings.filter(
    (finding) => finding.message && finding.bomRef === asset["bom-ref"]
  );
  return findings
}

// Returns a boolean indicating whether we are waiting for compliance results
export function isLoadingCompliance() {
  return model.policyCheckResult == null
}

// Returns a boolean indicating whether we have a valid object describing the compliance results (and no error has occurred)
export function hasValidComplianceResults() {
  return !isLoadingCompliance() && !model.policyCheckResult.error;
}


// Returns a boolean indeicating if the CBOM overalls comply with the policy
export function globalComplianceResult() {
    return hasValidComplianceResults() && model.policyCheckResult.globalComplianceStatus;
}

// Returns the name of the compliance policy
export function getCompliancePolicyName() {
    return hasValidComplianceResults() ? model.policyCheckResult.policyName : "";
}

// Returns the name of the compliance service
export function getComplianceServiceName() {
    return hasValidComplianceResults() ? model.policyCheckResult.complianceServiceName : "";
}

// Returns the name of the compliance service
export function isUsingLocalComplianceService() {
    return getComplianceServiceName() === getLocalComplianceServiceName();
}

// Returns an object specyfing the compliance labels (and long labels) for each used compliance level
export function getComplianceLevels() {
    return hasValidComplianceResults ? model.policyCheckResult.complianceLevels : [];
}

// Returns a boolean indicating whether we have a valid object describing the compliance results (and no error has occurred)
export function checkValidComplianceResults(policyCheckResult) {
  if (policyCheckResult == null) {
    return false;
  }

  // Check if error field is set to false
  if (policyCheckResult.error !== false) {
    console.error("The compliance backend was not able to return a compliance result")
    return false;
  }

  // Check if policyName, findings, and complianceLevels fields have the right types
  if (typeof policyCheckResult.policyName !== 'string' ||
      typeof policyCheckResult.complianceServiceName !== 'string' ||
      !Array.isArray(policyCheckResult.findings) ||
      !Array.isArray(policyCheckResult.complianceLevels) ||
      typeof policyCheckResult.defaultComplianceLevel !== 'number' ||
      typeof policyCheckResult.globalComplianceStatus !== 'boolean') {
    console.error("The compliance JSON object does not have the correct format")
    return false;
  }

  // Create a set of valid label ids from complianceLevels
  const validLabelIds = new Set();
  for (const label of policyCheckResult.complianceLevels) {
    if (typeof label.id !== 'number' || typeof label.label !== 'string' || typeof label.colorHex !== 'string' || typeof label.icon !== 'string') {
      console.error("A label of `complianceLevels` in the compliance JSON object does not have the correct format")
      return false;
    }
    // Check if the icon is a valid key in the complianceIconMap
    if (!Object.keys(complianceIconMap).includes(label.icon)) {
      console.error("The icon of a label of `complianceLevels` in the compliance JSON object is not a valid icon defined in the enum");
      return false;
    }
    // Check for optional description field
    if (label.description && typeof label.description !== 'string') {
      console.error("The description of a label of `complianceLevels` in the compliance JSON object does not have the correct format")
      return false;
    }
    // Check for duplicate label ids
    if (validLabelIds.has(label.id)) {
      console.error("Two (or more) labels of `complianceLevels` in the compliance JSON object have the same ID, which should be unique")
      return false;
    }
    validLabelIds.add(label.id);
  }

  // Check if findings list contains objects with bomRef, levelId, and message fields with the right types
  for (const item of policyCheckResult.findings) {
    if (typeof item.bomRef !== 'string' || typeof item.levelId !== 'number') {
      console.error("An element of `findings` in the compliance JSON object does not have the correct format")
      return false;
    }
    // Check for optional message field
    if (item.message && typeof item.message !== 'string') {
      console.error("The message of an element of `findings` in the compliance JSON object does not have the correct format")
      return false;
    }
    // Check if levelId value exists in validLabelIds
    if (!validLabelIds.has(item.levelId)) {
      console.error("An element of `findings` in the compliance JSON object does not have a valid label ID specified in `complianceLevels`")
      return false;
    }
  }

  return true;
}

// Returns an object specifying the number of assets at each compliance level
export function getComplianceRepartition() {
  let detections = getDetections();
  let complianceIds = getComplianceLevels().map(level => level.id);

  // Initialize counters for each compliance label
  let idCounts = {};
  complianceIds.forEach(id => {
    idCounts[id] = 0;
  });

  detections.forEach((detection) => {
    const status = getComplianceLevel(detection);
    // Increment the counter for the detected compliance level if it exists in idCounts
    idCounts[status] += 1;
  });

  return idCounts;
}

// Returns an object specyfing the color scale for a specific repartition of assets
// The object does not contain information for compliance levels that are not used by these specific assets
// Otherwise, this would lead to warnings when using this scale in a `ccv-donut-chart`
export function getColorScale() {
  let countsMap = getComplianceRepartition();
  let colorsMap = getComplianceLevels().reduce((acc, level) => {
    acc[level.id] = level.colorHex; // Map id to its color
    return acc;
  }, {});
  let labelsMap = getComplianceLevels().reduce((acc, level) => {
    acc[level.id] = level.label; // Map id to its label
    return acc;
  }, {});

  let colorScale = {};

  Object.keys(countsMap).forEach(id => {
    colorScale[labelsMap[id]] = colorsMap[id];
  });

  return colorScale;
}