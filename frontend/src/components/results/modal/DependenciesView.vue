<template>
  <div>
    <div>
      <h4 class="title" v-if="dependsOn.length > 0">
        Depends on
      </h4>
      <div v-for="(asset, index) in dependsOn" :key="index">
        <div style="display: flex; align-items: center; padding: 4px 10px;">
          <Downstream24 style="margin-right:12px; scale: 1.1; fill: #4dbabf"/>
          <div>
            <div style="font-size: large;">
              {{ getName(asset) + "   —   " + getAssetType(asset) }}
            </div>
            <div style="font-size: small" v-if="getBomRef(asset)">
              {{ "BOM Reference: " + getBomRef(asset) }}
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

    <h4 class="title" v-if="provides.length > 0">
      Provides to
    </h4>
    <div v-for="(asset, index) in provides" :key="index">
      <div style="display: flex; align-items: center; padding: 4px 10px;">
        <Upstream24 style="margin-right:12px; scale: 1.1; fill: #ae58d6"/>
        <div>
          <div style="font-size: large;">
            {{ getName(asset) + "   —   " + getAssetType(asset) }}
          </div>
          <div style="font-size: small" v-if="getBomRef(asset)">
            {{ "BOM Reference: " + getBomRef(asset) }}
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
</template>

<script>
import { getDependencies, getTermFullName } from "@/helpers.js";
import { Launch24, Upstream24, Downstream24 } from "@carbon/icons-vue";

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
    Upstream24,
    Downstream24
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
  beforeMount() {
    // Executed on page load
    let dependencies = getDependencies(this.bomRef);
    this.dependsOn = dependencies["dependsComponentList"]
    this.provides = dependencies["providesComponentList"]
  },
  computed: {
    dependsOn() {
      return getDependencies(this.bomRef)["dependsComponentList"];
    },
    provides() {
      return getDependencies(this.bomRef)["providesComponentList"];
    }
  }
};
</script>

<style>
.title {
  font-weight: 500;
  padding-top: 16px;
  padding-bottom: 4px
}
</style>