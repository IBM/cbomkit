<template>
  <div>
    <div v-if="dependsOn.length > 0" style="padding-bottom: 12px;">
      <h4 class="title">
        Depends on
      </h4>
      <div v-for="([asset, path], index) in dependsOn" :key="index">
        <div style="display: flex; align-items: center; padding: 4px 10px;">
          <Downstream24 style="margin-right:13px; scale: 1.1; fill: #05BE8D"/>
          <div>
            <div style="font-size: large;">
              {{ getName(asset).toUpperCase() + "   —   " + getAssetType(asset) }}
            </div>
            <div style="font-size: small" v-if="getBomRef(asset)">
              BOM Reference: <span class="compact-code">{{ getBomRef(asset) }}</span>
            </div>
            <div style="font-size: small" v-if="getBomRef(asset)">
              Source: <span class="compact-code">{{ path }}</span>
            </div>
          </div>
          <cv-button
            v-on:click="$emit('open-asset', asset)"
            :icon="Launch24"
            style="margin-left:auto;"
            kind="ghost"
          >
            See details
          </cv-button>
        </div>
      </div>
    </div>

    <div v-if="provides.length > 0" style="padding-bottom: 12px;">
      <h4 class="title">
        Provides to
      </h4>
      <div v-for="([asset, path], index) in provides" :key="index">
        <div style="display: flex; align-items: center; padding: 4px 10px;">
          <Upstream24 style="margin-right:13px; scale: 1.1; fill: #188A99"/>
          <div>
            <div style="font-size: large;">
              {{ getName(asset).toUpperCase() + "   —   " + getAssetType(asset) }}
            </div>
            <div style="font-size: small" v-if="getBomRef(asset)">
              BOM Reference: <span class="compact-code">{{ getBomRef(asset) }}</span>
            </div>
            <div style="font-size: small" v-if="getBomRef(asset)">
              Source: <span class="compact-code">{{ path }}</span>
            </div>
          </div>
          <cv-button
            v-on:click="$emit('open-asset', asset)"
            :icon="Launch24"
            style="margin-left:auto;"
            kind="ghost"
          >
            See details
          </cv-button>
        </div>
      </div>
    </div>

    <div v-if="isDependedOn.length > 0" style="padding-bottom: 12px;">
      <h4 class="title">
        Is used by
      </h4>
      <div v-for="([asset, path], index) in isDependedOn" :key="index">
        <div style="display: flex; align-items: center; padding: 4px 10px;">
          <Upstream24 style="margin-right:13px; scale: 1.1; fill: #FFBA1A"/>
          <div>
            <div style="font-size: large;">
              {{ getName(asset).toUpperCase() + "   —   " + getAssetType(asset) }}
            </div>
            <div style="font-size: small" v-if="getBomRef(asset)">
              BOM Reference: <span class="compact-code">{{ getBomRef(asset) }}</span>
            </div>
            <div style="font-size: small" v-if="getBomRef(asset)">
              Source: <span class="compact-code">{{ path }}</span>
            </div>
          </div>
          <cv-button
            v-on:click="$emit('open-asset', asset)"
            :icon="Launch24"
            style="margin-left:auto;"
            kind="ghost"
          >
            See details
          </cv-button>
        </div>
      </div>
    </div>

    <div v-if="isProvidedBy.length > 0" style="padding-bottom: 12px;">
      <h4 class="title">
        Is provided by
      </h4>
      <div v-for="([asset, path], index) in isProvidedBy" :key="index">
        <div style="display: flex; align-items: center; padding: 4px 10px;">
          <Downstream24 style="margin-right:13px; scale: 1.1; fill: #FF488E"/>
          <div>
            <div style="font-size: large;">
              {{ getName(asset).toUpperCase() + "   —   " + getAssetType(asset) }}
            </div>
            <div style="font-size: small" v-if="getBomRef(asset)">
              BOM Reference: <span class="compact-code">{{ getBomRef(asset) }}</span>
            </div>
            <div style="font-size: small" v-if="getBomRef(asset)">
              Source: <span class="compact-code">{{ path }}</span>
            </div>
          </div>
          <cv-button
            v-on:click="$emit('open-asset', asset)"
            :icon="Launch24"
            style="margin-left:auto;"
            kind="ghost"
          >
            See details
          </cv-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getDependencies, getTermFullName } from "@/helpers.js";
import { Launch24, Downstream24, Upstream24 } from "@carbon/icons-vue";

export default {
  name: "DependenciesView",
  props: {
    bomRef: null,
  },
  data() {
    return {
      Launch24
    };
  },
  components: {
    Downstream24,
    Upstream24
  },
  methods: {
    getDependencies,
    getTermFullName,
    getName: function(cryptoAsset) {
      if (cryptoAsset === undefined || cryptoAsset === null) {
        return "";
      }
      if (!Object.hasOwn(cryptoAsset, "name")) {
        return "";
      }
      return cryptoAsset.name;
    },
    getAssetType: function(cryptoAsset) {
      if (cryptoAsset === undefined || cryptoAsset === null) {
        return "";
      }
      if (!Object.hasOwn(cryptoAsset, "cryptoProperties")) {
        return "";
      }
      if (!Object.hasOwn(cryptoAsset.cryptoProperties, "assetType")) {
        return "";
      }
      return getTermFullName(cryptoAsset.cryptoProperties.assetType);
    },
    getBomRef: function(cryptoAsset) {
      if (cryptoAsset === undefined || cryptoAsset === null) {
        return "";
      }
      if (!Object.hasOwn(cryptoAsset, "bom-ref")) {
        return "";
      }
      return cryptoAsset["bom-ref"];
    },
  },
  computed: {
    dependsOn() {
      return getDependencies(this.bomRef)["dependsComponentList"];
    },
    isDependedOn() {
      return getDependencies(this.bomRef)["isDependedOnComponentList"];
    },
    provides() {
      return getDependencies(this.bomRef)["providesComponentList"];
    },
    isProvidedBy() {
      return getDependencies(this.bomRef)["isProvidedByComponentList"];
    }
  }
};
</script>

<style scoped>
.title {
  font-weight: 500;
  padding-top: 16px;
  padding-bottom: 4px
}
.compact-code {
  font-family: monospace;
  background-color: #86868634; /* Light gray background */
  padding: 2px 4px;
  border-radius: 4px; /* Rounded corners */
  font-size: x-small;
}
</style>