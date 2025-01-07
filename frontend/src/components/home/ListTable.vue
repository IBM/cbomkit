<template>
  <div>
    <cv-data-table-skeleton
      v-if="model.lastCboms.length === 0"
      :columns="columns.slice(0, 2)"
      :rows="numberOfLines"
    >
    </cv-data-table-skeleton>
    <cv-data-table :columns="columns" v-else>
      <template v-slot:data>
        <cv-data-table-row v-for="scan in model.lastCboms" :key="scan.id">
          <cv-data-table-cell>
            <div class="container">
              {{ scan.projectIdentifier }}
              <cv-icon-button
                @click="openGitRepo(scan.gitUrl)"
                kind="ghost"
                size="sm"
                :icon="Launch16"
                label="Open in a new tab"
              />
            </div>
          </cv-data-table-cell>
          <cv-data-table-cell>{{ dateString(scan) }}</cv-data-table-cell>
          <cv-data-table-cell>
            <cv-button
              @click="showResultFromApi(scan)"
              style="float: right"
              kind="ghost"
              :icon="ArrowRight24"
              label="See cryptography components"
            >
              See {{ countComponents(scan) }}
              {{
                countComponents(scan) > 1
                  ? "cryptographic assets"
                  : "cryptographic asset"
              }}
            </cv-button>
          </cv-data-table-cell>
        </cv-data-table-row>
      </template>
    </cv-data-table>
  </div>
</template>

<script>
import {model} from "@/model";
import {fetchLastCboms, getCbomFromScan, getDetectionsFromCbom, openGitRepo, showResultFromApi,} from "@/helpers";
import {ArrowRight24, Launch16} from "@carbon/icons-vue";

export default {
  name: "ListTable",
  data: function () {
    return {
      model,
      ArrowRight24,
      Launch16,
      columns: ["Most recent scans", "Date of scan", ""],
      numberOfLines: 5,
    };
  },
  methods: {
    showResultFromApi,
    getDetectionsFromCbom,
    countComponents: function (scan) {
      return getDetectionsFromCbom(getCbomFromScan(scan)).length;
    },
    dateString: function (scan) {
      // Parse the input date string
      const date = new Date(scan.createdAt);

      // Check if the date is valid
      if (isNaN(date)) {
        return "-";
      }

      // Get day, month, and year components
      const day = date.getDate();
      const month = date.getMonth() + 1; // Months are 0-based, so add 1
      const year = date.getFullYear();

      // Create the formatted date string
      return `${day}/${month}/${year}`;
    },
    openGitRepo,
    test: function () {

    },
  },
  beforeMount() {
    // Executed on page load
    fetchLastCboms(this.numberOfLines);
  },
};
</script>

<style scoped>
.container {
  display: flex; /* Use flexbox to arrange items horizontally */
  align-items: center; /* Vertically align items to the center */
}
</style>

<!-- Prevents scrolling bug on the ListTable -->
<style>
.bx--data-table-content {
  overflow: hidden;
}
</style>
