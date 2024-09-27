<template>
  <div class="table">
    <cv-modal ref="modalInfo">
      <template slot="label"
        >{{ getTermFullName(this.assetType) ? getTermFullName(this.assetType) : this.assetType }}</template
      >
      <template slot="title"
        ><h3>
          {{ assetName.toUpperCase() }}
        </h3></template
      >
      <template slot="content">
        <CryptoAssetDetails
          :asset="currentAssetModal"
          @open-code="openInCode"
          @open-asset="openAsset"
        />
      </template>
    </cv-modal>

    <cv-modal ref="modalPrompt" @after-modal-hidden="resetModal">
      <template slot="label">{{ modalPromptLabel }}</template>
      <template slot="title">{{ modalPromptTitle }}</template>
      <template slot="content">
        <GitInfoPrompt ref="gitInfoPrompt" @confirm-prompt="confirmPrompt"/>
      </template>
    </cv-modal>

    <cv-data-table-skeleton
      v-if="this.detections.length === 0 && model.scanning.isScanning"
      :columns="columns"
      :rows="5"
    >
      <template slot="actions">
        <cv-button :icon="downloadIcon" :disabled="true">
          Download CBOM
        </cv-button>
      </template></cv-data-table-skeleton
    >

    <cv-data-table
      v-else
      batch-cancel-label="Cancel"
      :columns="columns"
      :sortable="true"
      @sort="onSort"
      :pagination="pagination"
      @pagination="actionOnPagination"
      :overflow-menu="['Details', { label: 'More' }]"
    >
      <template slot="actions">
        <h5 style="margin-left: 16px; display: flex; align-items: center;" :style="isViewerOnly ? 'margin-right: auto' : ''">
          List of all assets
        </h5>
        <div v-if="!isViewerOnly" style="display: flex; align-items: center; margin-right: auto;">
          <LoaderView
            v-if="!model.scanning.isScanning"
            style="
              margin-right: auto;
              margin-left: 26px;
              display: flex;
              align-items: center;
            "
          />
          <div
            v-else
            style="
              font-size: small;
              margin-right: auto;
              margin-left: 26px;
              display: flex;
              align-items: center;
            "
          >
            Scan in progress...
          </div>
        </div>
        <cv-icon-button
          kind="ghost"
          @click="showPrompt(false)"
          :disabled="model.scanning.isScanning"
          :icon="SettingsAdjust24"
        ></cv-icon-button>
        <cv-button
          v-if="!isViewerOnly"
          @click="downloadCBOM"
          :disabled="model.scanning.isScanning"
          :icon="downloadIcon"
        >
          Download CBOM
        </cv-button>
      </template>
      <template slot="data">
        <cv-data-table-row
          v-for="(asset, rowIndex) in paginatedDetections"
          :key="`${rowIndex}`"
          :value="`${rowIndex}`"
        >
          <cv-data-table-cell>
            <div style="display: flex; align-items: center">
              <div style="padding-right: 6px; margin-bottom: -2px;">
                <cv-inline-loading
                  v-if="isLoadingCompliance"
                  state="loading"
                  loadingText=""
                  style="margin-bottom: 2px;"
                ></cv-inline-loading>
                <cv-tooltip
                  v-else-if="hasValidComplianceResults"
                  alignment="start"
                  direction="top"
                  :tip="getComplianceDescription(asset)"
                >
                  <!-- The compliance icon -->
                  <ComplianceIcon
                    :asset="asset"
                    v-if="hasValidComplianceResults"
                  />
                  <WatsonHealthImageAvailabilityUnavailable24 v-else/>
                </cv-tooltip>
              </div>
              <div style="padding: 6px; min-width: 130px">
                <div style="font-weight: 600">
                  {{ asset.name.toUpperCase() }}
                </div>
              </div>
            </div>
          </cv-data-table-cell>
          <cv-data-table-cell>
            <div style="padding: 6px; min-width: 100px">
              <div v-if="type(asset) !== ''">
                {{ getTermFullName(type(asset)) ? getTermFullName(type(asset)) : type(asset) }}
              </div>
              <div v-else>
                <em>Unspecified</em>
              </div> 
            </div>
          </cv-data-table-cell>
          <cv-data-table-cell>
            <div style="padding: 6px; min-width: 100px">
              <div v-if="primitive(asset) !== ''">
                {{ getTermFullName(primitive(asset)) ? getTermFullName(primitive(asset)) : primitive(asset) }}
              </div>
              <div v-else>
                <em>Unspecified</em>
              </div> 
            </div>
          </cv-data-table-cell>
          <cv-data-table-cell style="max-width: 200px; width: 30%">
            <div v-if="occurrences(asset) == null" >
              <em>No code location found</em>
            </div>
            <cv-link
              v-else
              @click="openInCodeFor({ index: rowIndex })"
              style="
                cursor: pointer;
                max-width: 100%;
                text-overflow: ellipsis;
                overflow: hidden;
                white-space: nowrap;
                display: inline-block;
              "
            >
              {{ fileName(occurrences(asset)) }}:{{
                lineNumber(occurrences(asset))
              }}
            </cv-link>
          </cv-data-table-cell>
          <cv-data-table-cell>
            <cv-icon-button
              :icon="Maximize24"
              kind="ghost"
              @click="showDetectionDetailsFor({ index: rowIndex })"
              style="float: right; margin-right: -8px; margin-left: -8px"
              label="See details"
              tip-alignment="end"
              tip-position="top"
            >
            </cv-icon-button>
          </cv-data-table-cell>
        </cv-data-table-row>
      </template>
    </cv-data-table>
  </div>
</template>

<script>
import {model} from "@/model";
import {
  canOpenOnline,
  capitalizeFirstLetter,
  getDetections,
  getTermFullName,
  getComplianceLevel,
  openOnline,
  hasValidComplianceResults,
  isViewerOnly,
  isLoadingCompliance,
  getComplianceDescription,
  resolvePath
} from "@/helpers";
import {
  Maximize24,
  SettingsAdjust24,
  WatsonHealthImageAvailabilityUnavailable24,
} from "@carbon/icons-vue";
import CryptoAssetDetails from "@/components/results/modal/CryptoAssetDetails.vue";
import GitInfoPrompt from "@/components/results/modal/GitInfoPrompt.vue";
import LoaderView from "@/components/results/LoaderView.vue";
import ComplianceIcon from "@/components/results/ComplianceIcon.vue"

export default {
  name: "DataTable",
  components: {
    CryptoAssetDetails,
    GitInfoPrompt,
    LoaderView,
    WatsonHealthImageAvailabilityUnavailable24,
    ComplianceIcon
  },
  data: function () {
    return {
      model,
      localFinalListOfAssets: [],
      Maximize24,
      currentAssetModal: null,
      currentPagination: null,
      openInCodeOnConfirm: false, // If true, the user has clicked on the button to get the prompt. If false, the prompt was shown after the user tried to openInCode.
      columns: ["Cryptographic asset", "Type", "Primitive", "Location"],
      downloadIcon: `<svg fill-rule="evenodd" height="16" name="download" role="img" viewBox="0 0 14 16" width="14" aria-label="Download" alt="Download">
        <title>Download</title>
        <path d="M7.506 11.03l4.137-4.376.727.687-5.363 5.672-5.367-5.67.726-.687 4.14 4.374V0h1v11.03z"></path>
        <path d="M13 15v-2h1v2a1 1 0 0 1-1 1H1a1 1 0 0 1-1-1v-2h1v2h12z"></path>
        </svg>'`,
      SettingsAdjust24,
    };
  },
  computed: {
    isViewerOnly,
    isLoadingCompliance,
    hasValidComplianceResults,
    detections() {
      if (this.localFinalListOfAssets.length > 0) {
        // We return the local copy of the list of assets that gets ordered by the DataTable sorting options
        return this.localFinalListOfAssets;
      }
      return getDetections();
    },
    paginatedDetections() {
      if (this.currentPagination == null) {
        return [];
      } else {
        return this.detections.slice(
          this.currentPagination.start - 1,
          this.currentPagination.start + this.currentPagination.length - 1
        );
      }
    },
    pagination() {
      return {
        numberOfItems: this.detections.length,
        pageSizes: [10, 25, 50, 100],
      };
    },
    assetName() {
      if (
        this.currentAssetModal === undefined ||
        this.currentAssetModal === null
      ) {
        return "";
      }
      if (!Object.hasOwn(this.currentAssetModal, "name")) {
        return "";
      }
      return this.currentAssetModal.name;
    },
    assetType() {
      if (
        this.currentAssetModal === undefined ||
        this.currentAssetModal === null
      ) {
        return "";
      }
      if (!Object.hasOwn(this.currentAssetModal, "cryptoProperties")) {
        return "";
      }
      return this.currentAssetModal.cryptoProperties.assetType;
    },
    variant() {
      if (
        this.currentAssetModal === undefined ||
        this.currentAssetModal === null
      ) {
        return "";
      }
      if (!Object.hasOwn(this.currentAssetModal, "cryptoProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.currentAssetModal.cryptoProperties,
          "algorithmProperties"
        )
      ) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.currentAssetModal.cryptoProperties.algorithmProperties,
          "variant"
        )
      ) {
        return "";
      }
      return this.currentAssetModal.cryptoProperties.algorithmProperties
        .variant;
    },
    modalPromptTitle() {
      if (this.openInCodeOnConfirm) {
        return "Specify the code origin to open it";
      } else {
        return "Specify the code origin";
      }
    },
    modalPromptLabel() {
      if (this.openInCodeOnConfirm) {
        return "Incomplete code origin";
      } else {
        return "";
      }
    },
  },
  methods: {
    getComplianceLevel,
    getComplianceDescription,
    getTermFullName,
    capitalizeFirstLetter,
    resolvePath,
    actionOnPagination: function (content) {
      // console.log(content)
      this.currentPagination = content;
    },
    downloadCBOM: function () {
      let data = JSON.stringify(model.cbom, null, 2);
      let filename = "cbom.json";
      let element = document.createElement("a");
      element.setAttribute(
        "href",
        "data:application/json;charset=utf-8," + encodeURIComponent(data)
      );
      element.setAttribute("download", filename);
      element.style.display = "none";
      document.body.appendChild(element);
      element.click();
      document.body.removeChild(element);
    },
    showDetectionDetailsFor: function (value) {
      this.currentAssetModal = this.paginatedDetections[value.index];
      this.$refs.modalInfo.show();
    },
    showPrompt: function (openInCodeOnConfirm) {
      this.openInCodeOnConfirm = openInCodeOnConfirm;
      this.$refs.modalPrompt.show();
    },
    confirmPrompt() {
      this.$refs.modalPrompt.hide();
      if (this.openInCodeOnConfirm) {
        this.openInCode(this.openInCodeOnConfirm);
      }
    },
    resetModal() {
      this.$refs.gitInfoPrompt.resetModal(); // Call the method in GitInfoPrompt
    },
    openInCodeFor: function (value) {
      this.currentAssetModal = this.paginatedDetections[value.index];
      this.openInCode(true);
    },
    openInCode(openInCodeOnConfirm) {
      // This method is separated from `openInCodeFor` because it is also called by the details modal through an event
      if (!canOpenOnline()) {
        this.showPrompt(openInCodeOnConfirm);
        return;
      }
      openOnline(this.currentAssetModal);
    },
    openAsset(asset) {
      // Close the modal
      this.$refs.modalInfo.hide();
      // Wait a bit
      setTimeout(() => {
        // Set the new asset
        this.currentAssetModal = asset;
        // Show the modal again
        this.$refs.modalInfo.show();
      }, 300);
    },
    onSort(sortBy) {
      // Sort by sorting a copy of the detections to not create change in the graph views (that depend on ordering)
      this.localFinalListOfAssets = getDetections();
      if (sortBy) {
        this.localFinalListOfAssets.sort((a, b) => {
          let itemA, itemB;
          switch (sortBy.index) {
            case "0":
              // Sort by compliance first, then alphabetically
              itemA = getComplianceLevel(a).toString()
              itemB = getComplianceLevel(b).toString()
              itemA += a["name"];
              itemB += b["name"];
              break;
            case "1":
              itemA = getTermFullName(this.type(a)) ? getTermFullName(this.type(a)) : this.type(a)
              itemB = getTermFullName(this.type(b)) ? getTermFullName(this.type(b)) : this.type(b)
              break;
            case "2":
            itemA = getTermFullName(this.primitive(a)) ? getTermFullName(this.primitive(a)) : this.primitive(a)
            itemB = getTermFullName(this.primitive(b)) ? getTermFullName(this.primitive(b)) : this.primitive(b)
              break;
            case "3":
              itemA = this.fileName(this.occurrences(a));
              itemB = this.fileName(this.occurrences(b));
              break;
            default:
              break;
          }
          if (sortBy.order === "descending") {
            return itemB.localeCompare(itemA);
          }
          if (sortBy.order === "ascending") {
            return itemA.localeCompare(itemB);
          }
          return 0;
        });
      }
    },
    primitive(cryptoAsset) {
      let res = resolvePath(cryptoAsset, "cryptoProperties.algorithmProperties.primitive");
      return res ? res.toString() : "";
    },
    type(cryptoAsset) {
      let res = resolvePath(cryptoAsset, "cryptoProperties.assetType");
      return res ? res.toString() : "";
    },
    occurrences(cryptoAsset) {
      let res = resolvePath(cryptoAsset, "evidence.occurrences");
      if (res !== 0 && Array.isArray(res) && res.length > 0) {
        return res[0];
      }
      return null;
    },
    fileName(detection) {
      if (detection === undefined || detection === null) {
        return "";
      }
      const filePath = detection.location;
      return filePath.substring(filePath.lastIndexOf("/") + 1);
    },
    lineNumber(detection) {
      if (detection === undefined || detection === null) {
        return "";
      }
      return detection.line;
    },
  },
};
</script>
