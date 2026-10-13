import { model, ErrorStatus } from "@/model.js";
import { API_LAST_CBOM_URL, API_CHECK_POLICY, API_CHECK_POLICY_NAME } from "@/app.config";
import { checkValidComplianceResults, createLocalComplianceReport, isViewerOnly } from "@/helpers.js";


export function fetchLastCboms(number) {
  let apiUrl = `${API_LAST_CBOM_URL}/${number}`;
  fetchDataFromApi(apiUrl, null)
    .then((jsonData) => {
      model.lastCboms = jsonData;
      if (Array.isArray(jsonData) && jsonData.length === 0) {
        model.addError(ErrorStatus.EmptyDatabase);
      }
    })
    .catch((error) => {
      console.error("Error:", error.message);
      model.addError(ErrorStatus.NoConnection);
    });
}

function getLocalComplianceReport(cbom) {
  let jsonDataLocal = createLocalComplianceReport(cbom);
  if (checkValidComplianceResults(jsonDataLocal)) {
    model.policyCheckResult = jsonDataLocal;
  } else {
    model.policyCheckResult = { error: true };
  }
}

function setRemoteComplianceFailure(cbom) {
  const totalCryptographicAssets = (cbom.components || []).filter(
    component => component.type === "cryptographic-asset"
  ).length;
  model.addError(ErrorStatus.FallBackLocalComplianceReport);
  model.policyCheckResult = {
    error: true,
    evaluationStatus: "error",
    globalComplianceStatus: false,
    findings: [],
    complianceLevels: [],
    defaultComplianceLevel: 2,
    evaluationSummary: {
      totalCryptographicAssets,
      applicableAssets: 0,
      evaluatedAssets: 0,
      quantumSafeAssets: 0,
      vulnerableAssets: 0,
      unknownAssets: 0,
      notApplicableAssets: 0
    }
  };
}

function getRemoteComplianceReport(cbom, policyIdentifier = API_CHECK_POLICY_NAME) {
  const apiUrl = `${API_CHECK_POLICY}?policyIdentifier=${policyIdentifier}`;

  // Create the request options
  const requestOptions = {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(cbom),
  };

  // Make the POST request
  fetchDataFromApi(apiUrl, requestOptions)
    .then((jsonData) => {
      if (checkValidComplianceResults(jsonData)) {
        model.policyCheckResult = jsonData;
      } else {
        console.warn("Remote compliance evaluation failed or returned an incompatible response")
        setRemoteComplianceFailure(cbom);
      }
    })
    .catch(() => {
      console.warn("Remote compliance evaluation failed; refusing a local green fallback")
      setRemoteComplianceFailure(cbom);
    });
}

export function getComplianceReport(cbom, policyIdentifier = API_CHECK_POLICY_NAME) {
  if (isViewerOnly()) {
    getLocalComplianceReport(cbom)
  } else {
    getRemoteComplianceReport(cbom, policyIdentifier)
  }
}

function fetchDataFromApi(apiUrl, requestOptions) {
  let fetchPromise;
  if (requestOptions === null) {
    fetchPromise = fetch(apiUrl);
  } else {
    fetchPromise = fetch(apiUrl, requestOptions);
  }
  return fetchPromise
    .then((response) => {
      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      return response.json();
    })
    .then((data) => {
      // console.log(`Received data from ${apiUrl}:`, data)
      return data;
    })
    .catch((error) => {
      // Handle errors during the fetch
      console.error("Error during request:", error.message);
      throw error;
    });
}
