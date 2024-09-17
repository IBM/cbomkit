import apiConfig from "../api.json";

const API_BASE_WS = process.env.VUE_APP_WS_API_BASE;
const API_BASE_HTTP = process.env.VUE_APP_HTTP_API_BASE;

export const API_SCAN_URL = joinURL(API_BASE_WS, apiConfig.SCAN);
export const API_LAST_CBOM_URL = joinURL(API_BASE_HTTP, apiConfig.LAST_CBOMS);
export const API_CHECK_POLICY = joinURL(API_BASE_HTTP, apiConfig.CHECK_POLICY);

function joinURL(baseURL, endpoint) {
  const url = new URL(endpoint, baseURL);
  return url.toString();
}
