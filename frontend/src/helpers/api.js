import { model, ErrorStatus } from "@/model.js";
import { API_LAST_CBOM_URL, API_CHECK_POLICY } from "@/app.config";
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

function getRemoteComplianceReport(cbom, policyIdentifier = 'quantum_safe') {
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
        // An error occured in the backend compliance service, we use the local compliance service instead
        console.warn("Using the local compliance report instead of the remote one")
        model.addError(ErrorStatus.FallBackLocalComplianceReport)
        getLocalComplianceReport(cbom)
      }
    })
    .catch(() => {
      console.warn("Using the local compliance report instead of the remote one")
      model.addError(ErrorStatus.FallBackLocalComplianceReport)
      getLocalComplianceReport(cbom)
    });
}

export function getComplianceReport(cbom, policyIdentifier = 'quantum_safe') {
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
