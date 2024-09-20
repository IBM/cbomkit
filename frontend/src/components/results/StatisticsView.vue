<template>
  <div style="display: flex; align-items: flex-start">
    <div style="width: 20%; margin-inline: auto">
      <!-- <h5 style="text-align: center; padding-bottom: 10px;">Proportion of Quantum Safe cryptographic assets</h5> -->
      <cv-loading
        v-if="isLoadingCompliance"
        :active="true"
        :overlay="false"
        class="loading-indicator"
      ></cv-loading>
      <div v-else-if="hasValidComplianceResults">
        <ccv-donut-chart
          :data="complianceData"
          :options="complianceOptions"
          style="flex: 1"
        ></ccv-donut-chart>
        <!-- Compliance disclaimer only shown when using the local compliance service -->
        <div
          v-if="isUsingLocalComplianceService"
          style="display: flex; padding-top: 14px; text-align: left; color: var(--cds-text-secondary);"
        >
          <p style="padding-right: 3px; size: small">
          *
          </p>
          <p style="font-size: small">
            This compliance data is approximate and given for illustrative purposes only.
          </p>
        </div>
      </div>
      <p
        v-else
        class="small-text"
        style="margin-top: 50%;"
      >
        Unavailable compliance results
      </p>
    </div>
    <div style="width: 20%; margin-inline: auto">
      <cv-loading
        v-if="getDetections().length == 0 && model.scanning.isScanning"
        :active="true"
        :overlay="false"
        class="loading-indicator"
      ></cv-loading>
      <CcvCirclePackChart v-else :data="nameData" :options="nameOptions" />
      <p
        v-if="nameNumber > 0"
        class="small-text"
        style="padding-top: 46px;"
      >
        {{ nameNumber }} types of crypto assets
      </p>
    </div>
    <div style="width: 20%; margin-inline: auto">
      <!-- <h5 style="text-align: center; padding-bottom: 10px;">Distribution of detected cryptographic primitives</h5> -->
      <cv-loading
        v-if="getDetections().length == 0 && model.scanning.isScanning"
        :active="true"
        :overlay="false"
        class="loading-indicator"
      ></cv-loading>
      <ccv-donut-chart
        v-else
        :data="primitiveData"
        :options="primitiveOptions"
        style="flex: 1"
      ></ccv-donut-chart>
    </div>
    <!-- <div style="width: 20%; margin-inline: auto;">
            <cv-loading
            v-if="getDetections().length==0 && model.scanning.isScanning"
            :active="true"
            :overlay="false"
            style="margin: auto; margin:70px auto"></cv-loading>
            <ccv-donut-chart v-else :data='symmetricData' :options='symmetricOptions' style="flex: 1;"></ccv-donut-chart>
        </div> -->
    <div style="width: 20%; margin-inline: auto">
      <!-- <h5 style="text-align: center; padding-bottom: 10px;">Distribution of detected cryptographic functions</h5> -->
      <cv-loading
        v-if="getDetections().length == 0 && model.scanning.isScanning"
        :active="true"
        :overlay="false"
        class="loading-indicator"
      ></cv-loading>
      <ccv-donut-chart
        v-else
        :data="functionsData"
        :options="functionsOptions"
        style="flex: 1"
      ></ccv-donut-chart>
    </div>
  </div>
</template>

<script>
import { model } from "@/model.js";
import {
  getDetections,
  getComplianceRepartition,
  getComplianceLevels,
  getColorScale,
  countOccurrences,
  capitalizeFirstLetter,
  countNames,
  hasValidComplianceResults,
  isLoadingCompliance,
  isUsingLocalComplianceService,
} from "@/helpers";

export default {
  name: "StatisticsView",
  data() {
    return {
      model,
    };
  },
  methods: {
    getDetections,
  },
  computed: {
    isLoadingCompliance,
    hasValidComplianceResults,
    isUsingLocalComplianceService,
    complianceData() {
      let countsMap = getComplianceRepartition();
      let labelsMap = getComplianceLevels().reduce((acc, info) => {
        acc[info.id] = info.label; // Map id to its label
        return acc;
      }, {});
      
      // Calculate total detections
      let totalDetections = Object.values(countsMap).reduce((sum, value) => sum + value, 0);
      
      if (totalDetections === 0) {
        // Return nothing to not display the chart when there is no data
        return [];
      }

      // Map the results to the expected format
      let data = [];
      Object.keys(countsMap).forEach(id => {
        data.push({
          group: labelsMap[id],
          value: countsMap[id]
        });
      });

      return data;
    },
    complianceOptions() {
      let colorScale = getColorScale();
      return {
        resizable: true,
        donut: {
          center: {
            label: `Crypto Assets${isUsingLocalComplianceService() ? "*" : ""}`,
          },
          alignment: "center",
        },
        height: "320px",
        toolbar: {
          enabled: false,
        },
        theme: model.useDarkMode ? "g100" : "white",
        color: {
          scale: colorScale,
        },
        legend: {
          alignment: "center",
        },
      };
    },
    nameData() {
      const capitalisedList = countNames()[0].map((obj) => ({
        ...obj,
        name: obj.name.toUpperCase(),
      }));
      return capitalisedList;
    },
    nameNumber() {
      return countNames()[1];
    },
    nameOptions() {
      return {
        resizable: true,
        height: "230px",
        toolbar: {
          enabled: false,
        },
        theme: model.useDarkMode ? "g100" : "white",
        legend: {
          enabled: false,
        },
      };
    },
    primitiveData() {
      // Small transformation: capitalise the first letter of each group (legend name)
      const capitalisedList = countOccurrences("primitive")[0].map((obj) => ({
        ...obj,
        group: capitalizeFirstLetter(obj.group),
      }));
      return capitalisedList;
    },
    primitiveOptions() {
      return {
        resizable: true,
        donut: {
          center: {
            label: "Crypto Primitives",
            number: countOccurrences("primitive")[1],
          },
          alignment: "center",
        },
        height: "320px",
        toolbar: {
          enabled: false,
        },
        theme: model.useDarkMode ? "g100" : "white",
        legend: {
          alignment: "center",
          enabled: true,
        },
      };
    },
    functionsData() {
      // Small transformation: capitalise the first letter of each group (legend name)
      const capitalisedList = countOccurrences("cryptoFunctions")[0].map(
        (obj) => ({
          ...obj,
          group: capitalizeFirstLetter(obj.group),
        })
      );
      return capitalisedList;
    },
    functionsOptions() {
      return {
        resizable: true,
        donut: {
          center: {
            label: "Crypto Functions",
            number: countOccurrences("cryptoFunctions")[1],
          },
          alignment: "center",
        },
        height: "320px",
        toolbar: {
          enabled: false,
        },
        theme: model.useDarkMode ? "g100" : "white",
        legend: {
          alignment: "center",
          enabled: true,
        },
      };
    },
  },
};
</script>

<style scoped>
.loading-indicator {
  margin: 70px auto
}
.small-text {
  text-align: center;
  font-size: small;
  color: var(--cds-text-secondary);
}
</style>

<style>
@import "@carbon/charts-vue/styles.css";

/* Changes the color of the loading indicator EVERYWHERE */
.bx--loading__stroke {
  stroke: var(--cds-layer-active) !important;
}
</style>
