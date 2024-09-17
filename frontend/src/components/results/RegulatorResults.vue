<template>
  <div>
    <!-- Scanning loader -->
    <LoaderView
      v-if="model.scanning.isScanning"
      class="skeleton-bordered"
      style="
        margin-right: auto;
        padding-left: 16px;
        display: flex;
        align-items: center;
      "
      v-bind:style="{ borderColor: skeletonColor }"
    />

    <!-- Skeleton inline notification -->
    <div
      class="skeleton-bordered"
      v-else-if="isLoadingCompliance"
      v-bind:style="{ borderColor: skeletonColor }"
    >
      <InProgress16
        style="margin: 0px 16px 0px 6px"
        v-bind:style="{ color: skeletonColor }"
      />
      <h6 style="padding: 7px 12px 7px 0px">Analyzing compliance...</h6>
      <cv-skeleton-text
        v-if="true"
        :heading="false"
        :paragraph="false"
        :line-count="1"
        style="margin-bottom: -8px; width: 60%"
      >
      </cv-skeleton-text>
    </div>

    <!-- Inline notification -->
    <cv-inline-notification
      v-else
      :kind="this.kind"
      :title="this.title"
      :sub-title="this.description"
      :low-contrast="true"
      :hide-close-button="true"
      style="margin: 0px"
      v-bind:style="{ background: backgroundColor }"
    >
    </cv-inline-notification>
  </div>
</template>

<script>
import { model } from "@/model.js";
import { getComplianceReport,
  isLoadingCompliance,
  hasValidComplianceResults,
  globalComplianceResult,
  getCompliancePolicyName,
  getComplianceServiceName} from "@/helpers";
import { InProgress16 } from "@carbon/icons-vue";
import LoaderView from "@/components/results/LoaderView.vue";

export default {
  name: "RegulatorResults",
  data() {
    return {
      model,
    };
  },
  components: {
    InProgress16,
    LoaderView,
  },
  computed: {
    isLoadingCompliance,
    getComplianceServiceName,
    title() {
      if (isLoadingCompliance()) {
        return "Analyzing compliance...";
      }
      if (hasValidComplianceResults()) {
        if (globalComplianceResult()) {
          return "Compliant –";
        } else {
          return "Not compliant –";
        }
      }
      return "Compliance results unavailable –";
    },
    description() {
      let complianceServiceName = getComplianceServiceName();
      let sourceString = ""
      if (complianceServiceName !== "") {
        sourceString = `<br/><span style="font-size: x-small;">Source: ${getComplianceServiceName()}</span>`
      }
      if (isLoadingCompliance()) {
        return "";
      }
      if (hasValidComplianceResults()) {
        const complianceText = globalComplianceResult() 
          ? "complies with the policy" 
          : "does not comply with the policy";
        return `This CBOM ${complianceText} "${getCompliancePolicyName()}".` + sourceString;
      }
      return `Compliance could not be assessed at this time.` + sourceString;
    },
    backgroundColor() {
      if (model.useDarkMode && !isLoadingCompliance()) {
        if (!hasValidComplianceResults()) {
          return "#2f4c78";
        }
        else if (globalComplianceResult()) {
          return "#1B5E20";
        } else {
          return "#705b1a";
        }
      }
      // In light mode, returning an empty string keeps the default background color of the component
      return "";
    },
    skeletonColor() {
      if (model.useDarkMode) {
        return "#929191";
      } else {
        return "#BAB9B9";
      }
    },
    kind() {
      if (hasValidComplianceResults()) {
        if (globalComplianceResult()) {
          return "success";
        } else {
          return "warning";
        }
      }
      return "info";
    },
  },
  beforeMount() {
    // Executed on page load
    // Case where the results page is loaded with a CBOM (when uploading a CBOM or clicking on recent scans)
    // The CBOM is therefore immediately sent to the API
    if (model.cbom != null) {
      getComplianceReport(model.cbom);
    }
  },
  watch: {
    // Case where the results page is loaded without a CBOM yet (when starting the scan of a repo)
    // The CBOM is sent to the API as soon as it is received by the client
    "model.cbom": function (newResult) {
      if (newResult != null) {
        getComplianceReport(model.cbom);
      }
    },
  },
};
</script>

<style scoped>
.skeleton-bordered {
  padding: 7px;
  border-style: solid;
  border-width: thin;
  border-color: lightgray;
  border-left-width: medium;
  display: flex; /* Use flexbox to arrange items horizontally */
  align-items: center;
}
</style>

<!-- Allows cv-inline-notification to fully extend horizontally -->
<style>
.bx--inline-notification {
  max-width: none !important;
}
</style>
