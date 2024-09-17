<template>
  <div style="padding: 20px 10px">
    <!-- CODE -->
    <div>
      <div
        style="
          display: flex;
          align-items: center;
          margin-bottom: -6px;
        "
      >
        <h4 style="font-weight: 500">Code</h4>
        <cv-button
          v-if="hasCodeLocation(asset)"
          class="code-button"
          kind="ghost"
          v-on:click="$emit('open-code', true)"
          style="margin-left: auto"
        >
          View code <Launch16 class="bx--btn__icon"
        /></cv-button>
      </div>
      <GithubEmbed v-if="hasCodeLocation(asset)" :asset="asset" @open-code="$emit('open-code', false)" />
      <!-- Text when no code location was specified -->
      <div v-else style="margin-top: 20px; border-radius: 15px; background-color: rgba(0, 0, 0, 0.15); padding: 15px;">
        No code location has been specified in the CBOM for this cryptographic asset. If you include code location information in a CBOM from a public online repository, you will be able to preview the code here and open it directly on a service like GitHub.
      </div>
    </div>

    <!-- POLICY FINDINGS -->
    <div v-if="hasValidComplianceResults">
      <div
        style="
          display: flex;
          align-items: center;
          padding-top: 16px;
          padding-bottom: 6px;
        "
      >
        <h4 style="font-weight: 500">Compliance</h4>
      </div>

      <div style="display: flex; align-items: center; padding: 4px 10px 12px;">
        <ComplianceIcon
          :asset="asset"
          v-if="hasValidComplianceResults"
          style="margin-right:12px; scale: 1.2;"
        />
        <WatsonHealthImageAvailabilityUnavailable24 v-else/>
        <div>
          <div style="font-size: large">
            {{ getComplianceDescription(asset) }}
          </div>
          <div style="font-size: small">
            Policy: {{ getCompliancePolicyName }}
          </div>
        </div>
      </div>
    
      <div class="list" v-if="getComplianceFindingsWithMessage(asset).length>0" style="margin-bottom: -60px">
        <cv-structured-list condensed="true">
          <template slot="headings">
            <cv-structured-list-heading>Compliance Information</cv-structured-list-heading>
            <cv-structured-list-heading style="width: 25%">
              Category
            </cv-structured-list-heading>
          </template>
          <template slot="items">
            <cv-structured-list-item v-for="(finding, index) in getComplianceFindingsWithMessage(asset)" :key="index">
              <cv-structured-list-data>
                {{ finding.message }}
              </cv-structured-list-data>
              <cv-structured-list-data style="display: flex; align-items: center;">
                {{ getComplianceObjectFromId(finding.levelId).label }}
              </cv-structured-list-data>
            </cv-structured-list-item>
          </template>
        </cv-structured-list>
      </div>
      
    </div>

    <!-- SPECIFICATION -->
    <h4 style="font-weight: 500; padding-top: 16px; padding-bottom: 4px">
      Specification
    </h4>
    <div class="list">
      <cv-structured-list condensed="true">
        <template slot="headings">
          <cv-structured-list-heading style="width: 30%"
            >Type</cv-structured-list-heading
          >
          <cv-structured-list-heading>Value</cv-structured-list-heading>
        </template>
        <template slot="items">
          <!-- When the asset is an `algorithm` -->
          <!-- TODO: add all terms to the dictionary and use `getTerm___` + add tooltip everywhere -->
          <cv-structured-list-item v-if="this.algorithmPrimitive">
            <cv-structured-list-data>Primitive</cv-structured-list-data>
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ getTermFullName(this.algorithmPrimitive, "primitive") }}
              <cv-tooltip
                v-if="
                  getTermFullName(
                    this.algorithmPrimitive,
                    'primitive',
                    false
                  ) !== ''
                "
                :tip="getTermDescription(this.algorithmPrimitive, 'primitive')"
                alignment="end"
                class="tooltip"
              >
              </cv-tooltip>
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item v-if="this.algorithmParameterSetIdentifier">
            <cv-structured-list-data>Parameter set identifier</cv-structured-list-data>
            <cv-structured-list-data style="display: flex; align-items: center">
              <!-- TODO: Currently, the dictionnary does not have a dedicated variant section but looks into the names -->
              {{ getTermFullName(this.algorithmParameterSetIdentifier, "name") }}
              <cv-tooltip
                v-if="
                  getTermFullName(this.algorithmParameterSetIdentifier, 'name', false) !== ''
                "
                :tip="getTermDescription(this.algorithmParameterSetIdentifier, 'name')"
                alignment="end"
                class="tooltip"
              >
              </cv-tooltip>
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item v-if="this.algorithmImplementationLevel">
            <cv-structured-list-data
              >Implementation Level</cv-structured-list-data
            >
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ this.algorithmImplementationLevel }}
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item v-if="this.algorithmImplementationPlatform">
            <cv-structured-list-data
              >Implementation Platform</cv-structured-list-data
            >
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ this.algorithmImplementationPlatform }}
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item v-if="this.algorithmCertificationLevel">
            <cv-structured-list-data
              >Certification Level</cv-structured-list-data
            >
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ this.algorithmCertificationLevel }}
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item v-if="this.algorithmMode">
            <cv-structured-list-data>Mode</cv-structured-list-data>
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ getTermFullName(this.algorithmMode, "mode") }}
              <cv-tooltip
                v-if="getTermFullName(this.algorithmMode, 'mode', false) !== ''"
                :tip="getTermDescription(this.algorithmMode, 'mode')"
                alignment="end"
                class="tooltip"
              >
              </cv-tooltip>
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item v-if="this.algorithmPadding">
            <cv-structured-list-data>Padding</cv-structured-list-data>
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ getTermFullName(this.algorithmPadding, "padding") }}
              <cv-tooltip
                v-if="
                  getTermFullName(this.algorithmPadding, 'padding', false) !== ''
                "
                :tip="getTermDescription(this.algorithmPadding, 'padding')"
                alignment="end"
                class="tooltip"
              >
              </cv-tooltip>
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item v-if="this.algorithmCryptoFunctions">
            <cv-structured-list-data>Crypto Functions</cv-structured-list-data>
            <cv-structured-list-data>
              <div
                v-for="func in this.algorithmCryptoFunctions"
                :key="func"
                style="display: flex; align-items: center; padding-bottom: 2px"
              >
                {{ getTermFullName(func, "cryptoFunction") }}
                <cv-tooltip
                  v-if="getTermFullName(func, 'cryptoFunction', false) !== ''"
                  :tip="getTermDescription(func, 'cryptoFunction')"
                  alignment="end"
                  class="tooltip"
                >
                </cv-tooltip>
              </div>
            </cv-structured-list-data>
          </cv-structured-list-item>

          <!-- When the asset is a `relatedCryptoMaterial` -->
          <!-- TODO: add these terms to the dictionary and use `getTerm___` + add tooltip -->
          <cv-structured-list-item v-if="this.relatedCryptoMaterialType">
            <cv-structured-list-data>Type</cv-structured-list-data>
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ this.relatedCryptoMaterialType }}
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item v-if="this.relatedCryptoMaterialSize">
            <cv-structured-list-data>Size</cv-structured-list-data>
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ this.relatedCryptoMaterialSize }}
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item v-if="this.relatedCryptoMaterialFormat">
            <cv-structured-list-data>Format</cv-structured-list-data>
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ this.relatedCryptoMaterialFormat }}
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item
            v-if="this.relatedCryptoMaterialSecured != null"
          >
            <cv-structured-list-data>Secured</cv-structured-list-data>
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ this.relatedCryptoMaterialSecured }}
            </cv-structured-list-data>
          </cv-structured-list-item>
          <cv-structured-list-item v-if="this.algorithmOid">
            <cv-structured-list-data>OID</cv-structured-list-data>
            <cv-structured-list-data style="display: flex; align-items: center">
              {{ this.algorithmOid }}
            </cv-structured-list-data>
          </cv-structured-list-item>
        </template>
      </cv-structured-list>
    </div>
  </div>
</template>

<script>
import {
  getTermFullName,
  getTermDescription,
  capitalizeFirstLetter,
  getPolicyResultsByAsset,
  hasValidComplianceResults,
  getComplianceDescription,
  getComplianceFindingsWithMessage,
  getCompliancePolicyName,
  getComplianceLabel,
  getComplianceObjectFromId,
} from "@/helpers";
import {
  Launch16,
  WatsonHealthImageAvailabilityUnavailable24,
} from "@carbon/icons-vue";
import GithubEmbed from "@/components/results/modal/GithubEmbed.vue";
import ComplianceIcon from "@/components/results/ComplianceIcon.vue"

export default {
  name: "CryptoAssetDetails",
  data: function () {
    return {

    };
  },
  components: {
    GithubEmbed,
    Launch16,
    WatsonHealthImageAvailabilityUnavailable24,
    ComplianceIcon,
  },
  props: {
    asset: null,
  },
  computed: {
    hasValidComplianceResults,
    getCompliancePolicyName,
    algorithmOid() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(this.asset.cryptoProperties, "oid")) {
        return "";
      }
      return this.asset.cryptoProperties.oid;
    },
    algorithmParameterSetIdentifier() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(this.asset.cryptoProperties, "algorithmProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.algorithmProperties,
          "parameterSetIdentifier"
        )
      ) {
        return "";
      }
      return this.asset.cryptoProperties.algorithmProperties.parameterSetIdentifier;
    },
    algorithmImplementationLevel() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(this.asset.cryptoProperties, "algorithmProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.algorithmProperties,
          "implementationLevel"
        )
      ) {
        return "";
      }
      return this.asset.cryptoProperties.algorithmProperties
        .implementationLevel;
    },
    algorithmImplementationPlatform() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(this.asset.cryptoProperties, "algorithmProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.algorithmProperties,
          "implementationPlatform"
        )
      ) {
        return "";
      }
      return this.asset.cryptoProperties.algorithmProperties
        .implementationPlatform;
    },
    algorithmCertificationLevel() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(this.asset.cryptoProperties, "algorithmProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.algorithmProperties,
          "certificationLevel"
        )
      ) {
        return "";
      }
      return this.asset.cryptoProperties.algorithmProperties.certificationLevel;
    },
    algorithmPrimitive() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(this.asset.cryptoProperties, "algorithmProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.algorithmProperties,
          "primitive"
        )
      ) {
        return "";
      }
      return this.asset.cryptoProperties.algorithmProperties.primitive;
    },
    algorithmMode() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(this.asset.cryptoProperties, "algorithmProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(this.asset.cryptoProperties.algorithmProperties, "mode")
      ) {
        return "";
      }
      return this.asset.cryptoProperties.algorithmProperties.mode;
    },
    algorithmPadding() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(this.asset.cryptoProperties, "algorithmProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.algorithmProperties,
          "padding"
        )
      ) {
        return "";
      }
      return this.asset.cryptoProperties.algorithmProperties.padding;
    },
    algorithmCryptoFunctions() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(this.asset.cryptoProperties, "algorithmProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.algorithmProperties,
          "cryptoFunctions"
        )
      ) {
        return "";
      }
      return this.asset.cryptoProperties.algorithmProperties.cryptoFunctions;
    },
    relatedCryptoMaterialType() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties,
          "relatedCryptoMaterialProperties"
        )
      ) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.relatedCryptoMaterialProperties,
          "relatedCryptoMaterialType"
        )
      ) {
        return "";
      }
      return this.asset.cryptoProperties.relatedCryptoMaterialProperties
        .relatedCryptoMaterialType;
    },
    relatedCryptoMaterialSize() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties,
          "relatedCryptoMaterialProperties"
        )
      ) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.relatedCryptoMaterialProperties,
          "size"
        )
      ) {
        return "";
      }
      return this.asset.cryptoProperties.relatedCryptoMaterialProperties.size;
    },
    relatedCryptoMaterialFormat() {
      if (this.asset === undefined || this.asset === null) {
        return "";
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties,
          "relatedCryptoMaterialProperties"
        )
      ) {
        return "";
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.relatedCryptoMaterialProperties,
          "format"
        )
      ) {
        return "";
      }
      return this.asset.cryptoProperties.relatedCryptoMaterialProperties.format;
    },
    relatedCryptoMaterialSecured() {
      if (this.asset === undefined || this.asset === null) {
        return null;
      }
      if (!Object.hasOwn(this.asset, "cryptoProperties")) {
        return null;
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties,
          "relatedCryptoMaterialProperties"
        )
      ) {
        return null;
      }
      if (
        !Object.hasOwn(
          this.asset.cryptoProperties.relatedCryptoMaterialProperties,
          "secured"
        )
      ) {
        return null;
      }
      return this.asset.cryptoProperties.relatedCryptoMaterialProperties
        .secured;
    },
    assetDetails() {
      var fullName;
      var description;
      if (this.asset != undefined || this.asset != null) {
        fullName = getTermFullName(this.asset.name, "name");
        description = getTermDescription(this.asset.name, "name");
      }
      if (fullName == "") {
        fullName = "Unknown asset";
      }
      if (description == "") {
        description = "No description was found for this asset.";
      }
      return { fullName: fullName, description: description };
    },
  },
  methods: {
    getTermFullName,
    getTermDescription,
    capitalizeFirstLetter,
    getPolicyResultsByAsset,
    getComplianceDescription,
    getComplianceFindingsWithMessage,
    getComplianceLabel,
    getComplianceObjectFromId,
    hasCodeLocation(cryptoAsset) {
      if (cryptoAsset === undefined || cryptoAsset === null) {
        return false;
      }
      if (!Object.hasOwn(cryptoAsset, "evidence")) {
        return false;
      }
      if (!Object.hasOwn(cryptoAsset.evidence, "occurrences")) {
        return false;
      }
      return true;
    }
  },
};
</script>

<style scoped>
.tooltip {
  margin-left: 10px;
}
</style>